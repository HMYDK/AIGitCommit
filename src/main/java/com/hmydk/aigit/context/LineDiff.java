package com.hmydk.aigit.context;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal line-based diff used to build the prompt sent to the model.
 *
 * <p>The previous implementation compared both revisions line by line by index, so inserting or
 * deleting a single line turned every following line into a fake "-/+ pair". The prompt then
 * contained code the developer never touched and the real change was pushed out by truncation
 * (see issue #84). This class computes a longest-common-subsequence edit script instead, which
 * reports only the lines that actually changed.</p>
 *
 * <p>Output is a unified diff without context lines: changed lines are grouped into hunks with a
 * {@code @@} header, so the model keeps the position information without paying for the whole
 * file. Line endings are normalized before comparison, so a CRLF/LF mismatch between the Git
 * revision and the working tree does not look like a whole-file rewrite.</p>
 */
final class LineDiff {

    /** Above this many LCS cells (changed lines only) fall back to a coarse delete+add block. */
    private static final int MAX_LCS_CELLS = 4_000_000;

    private LineDiff() {
    }

    static String unified(String path, String beforeContent, String afterContent) {
        List<String> before = splitLines(beforeContent);
        List<String> after = splitLines(afterContent);

        StringBuilder diff = new StringBuilder();
        diff.append("--- a/").append(path).append('\n');
        diff.append("+++ b/").append(path).append('\n');

        for (Hunk hunk : computeHunks(before, after)) {
            hunk.appendTo(diff);
        }

        return diff.toString();
    }

    private static List<Hunk> computeHunks(List<String> before, List<String> after) {
        List<Hunk> hunks = new ArrayList<>();
        int prefix = commonPrefixLength(before, after);
        int suffix = commonSuffixLength(before, after, prefix);

        int beforeMiddleEnd = before.size() - suffix;
        int afterMiddleEnd = after.size() - suffix;
        int beforeMiddleCount = beforeMiddleEnd - prefix;
        int afterMiddleCount = afterMiddleEnd - prefix;

        if (beforeMiddleCount == 0 && afterMiddleCount == 0) {
            return hunks;
        }

        if ((long) beforeMiddleCount * afterMiddleCount > MAX_LCS_CELLS) {
            hunks.add(new Hunk(prefix, prefix,
                    before.subList(prefix, beforeMiddleEnd),
                    after.subList(prefix, afterMiddleEnd)));
            return hunks;
        }

        return toHunks(editScript(before, after, prefix, beforeMiddleEnd, afterMiddleEnd), before, after);
    }

    /**
     * Longest-common-subsequence edit script over the changed middle sections. Common lines are
     * trimmed before the table is filled to keep the cost proportional to the actual change.
     */
    private static List<Edit> editScript(List<String> before,
                                         List<String> after,
                                         int beforeStart,
                                         int beforeEnd,
                                         int afterEnd) {
        int n = beforeEnd - beforeStart;
        int m = afterEnd - beforeStart;
        int width = m + 1;
        int[] lcs = new int[(n + 1) * width];

        for (int i = n - 1; i >= 0; i--) {
            String beforeLine = before.get(beforeStart + i);
            int row = i * width;
            int nextRow = (i + 1) * width;
            for (int j = m - 1; j >= 0; j--) {
                if (beforeLine.equals(after.get(beforeStart + j))) {
                    lcs[row + j] = lcs[nextRow + j + 1] + 1;
                } else {
                    lcs[row + j] = Math.max(lcs[nextRow + j], lcs[row + j + 1]);
                }
            }
        }

        List<Edit> edits = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < n || j < m) {
            if (i < n && j < m && before.get(beforeStart + i).equals(after.get(beforeStart + j))) {
                edits.add(new Edit(EditType.EQUAL, beforeStart + i, beforeStart + j));
                i++;
                j++;
            } else if (j == m || (i < n && lcs[(i + 1) * width + j] >= lcs[i * width + j + 1])) {
                edits.add(new Edit(EditType.DELETE, beforeStart + i, beforeStart + j));
                i++;
            } else {
                edits.add(new Edit(EditType.INSERT, beforeStart + i, beforeStart + j));
                j++;
            }
        }
        return edits;
    }

    private static List<Hunk> toHunks(List<Edit> edits, List<String> before, List<String> after) {
        List<Hunk> hunks = new ArrayList<>();
        Hunk current = null;

        for (Edit edit : edits) {
            if (edit.type == EditType.EQUAL) {
                if (current != null) {
                    hunks.add(current);
                    current = null;
                }
                continue;
            }
            if (current == null) {
                current = new Hunk(edit.beforeIndex, edit.afterIndex, new ArrayList<>(), new ArrayList<>());
            }
            if (edit.type == EditType.DELETE) {
                current.deleted.add(before.get(edit.beforeIndex));
            } else {
                current.added.add(after.get(edit.afterIndex));
            }
        }
        if (current != null) {
            hunks.add(current);
        }
        return hunks;
    }

    private static int commonPrefixLength(List<String> before, List<String> after) {
        int limit = Math.min(before.size(), after.size());
        int prefix = 0;
        while (prefix < limit && before.get(prefix).equals(after.get(prefix))) {
            prefix++;
        }
        return prefix;
    }

    private static int commonSuffixLength(List<String> before, List<String> after, int prefix) {
        int limit = Math.min(before.size(), after.size()) - prefix;
        int suffix = 0;
        while (suffix < limit
                && before.get(before.size() - 1 - suffix).equals(after.get(after.size() - 1 - suffix))) {
            suffix++;
        }
        return suffix;
    }

    private static List<String> splitLines(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < normalized.length(); i++) {
            if (normalized.charAt(i) == '\n') {
                lines.add(normalized.substring(start, i));
                start = i + 1;
            }
        }
        if (start < normalized.length()) {
            lines.add(normalized.substring(start));
        }
        return lines;
    }

    private enum EditType {
        EQUAL,
        DELETE,
        INSERT
    }

    private record Edit(EditType type, int beforeIndex, int afterIndex) {
    }

    private static final class Hunk {
        private final int oldStart;
        private final int newStart;
        private final List<String> deleted;
        private final List<String> added;

        private Hunk(int oldStart, int newStart, List<String> deleted, List<String> added) {
            this.oldStart = oldStart;
            this.newStart = newStart;
            this.deleted = deleted;
            this.added = added;
        }

        private void appendTo(StringBuilder diff) {
            diff.append("@@ -").append(rangeStart(oldStart, deleted.size())).append(',').append(deleted.size())
                    .append(" +").append(rangeStart(newStart, added.size())).append(',').append(added.size())
                    .append(" @@\n");
            for (String line : deleted) {
                diff.append('-').append(line).append('\n');
            }
            for (String line : added) {
                diff.append('+').append(line).append('\n');
            }
        }

        /**
         * Unified-diff ranges are 1-based; a zero-length range starts on the line before the
         * insertion point, which matches the 0-based insertion index.
         */
        private static int rangeStart(int zeroBasedStart, int count) {
            return count == 0 ? zeroBasedStart : zeroBasedStart + 1;
        }
    }
}
