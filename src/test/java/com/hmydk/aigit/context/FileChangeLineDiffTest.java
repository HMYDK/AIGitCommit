package com.hmydk.aigit.context;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for issue #84: the prompt diff used to compare file contents line by line by
 * index, so a single inserted or deleted line shifted every following line and turned the whole
 * file into a fake "-/+ pair" diff. The model then received code the developer never touched.
 */
class FileChangeLineDiffTest {

    @Test
    void reportsOnlyTheInsertedLineWhenALineIsInsertedInTheMiddle() {
        List<String> original = numberedLines(1, 300);
        List<String> inserted = new ArrayList<>(original);
        inserted.add(10, "inserted line");

        FileChange change = fileChangeFor(String.join("\n", original), String.join("\n", inserted));
        String diff = change.getDiffContent();

        assertEquals(1, change.getLinesAdded(), "inserted line count");
        assertEquals(0, change.getLinesDeleted(), "deleted line count");
        assertTrue(diff.contains("+inserted line"), () -> "missing inserted line in:\n" + diff);
        assertFalse(diff.contains("-line-150"), () -> "unchanged line reported as deleted in:\n" + diff);
        assertFalse(diff.contains("+line-150"), () -> "unchanged line reported as added in:\n" + diff);
        assertEquals(1, countChangedLines(diff),
                "a one-line insertion must produce exactly one changed diff line, but got:\n" + diff);
    }

    @Test
    void reportsOnlyTheReplacedLineWhenOneLineChanges() {
        List<String> original = numberedLines(1, 300);
        List<String> modified = new ArrayList<>(original);
        modified.set(149, "line-150 changed");

        FileChange change = fileChangeFor(String.join("\n", original), String.join("\n", modified));
        String diff = change.getDiffContent();

        assertEquals(1, change.getLinesAdded(), "added line count");
        assertEquals(1, change.getLinesDeleted(), "deleted line count");
        assertTrue(diff.contains("-line-150"), () -> "missing deleted line in:\n" + diff);
        assertTrue(diff.contains("+line-150 changed"), () -> "missing added line in:\n" + diff);
        assertEquals(2, countChangedLines(diff),
                "a one-line replacement must produce exactly two changed diff lines, but got:\n" + diff);
    }

    @Test
    void reportsOnlyTheDeletedLineWhenALineIsRemoved() {
        List<String> original = numberedLines(1, 300);
        List<String> removed = new ArrayList<>(original);
        removed.remove(149);

        FileChange change = fileChangeFor(String.join("\n", original), String.join("\n", removed));
        String diff = change.getDiffContent();

        assertEquals(0, change.getLinesAdded(), "added line count");
        assertEquals(1, change.getLinesDeleted(), "deleted line count");
        assertTrue(diff.contains("-line-150"), () -> "missing deleted line in:\n" + diff);
        assertEquals(1, countChangedLines(diff),
                "a one-line deletion must produce exactly one changed diff line, but got:\n" + diff);
    }

    @Test
    void doesNotTreatLineEndingsAsContentChanges() {
        List<String> original = numberedLines(1, 120);
        List<String> modified = new ArrayList<>(original);
        modified.set(49, "line-050 changed");

        FileChange change = fileChangeFor(String.join("\r\n", original), String.join("\n", modified));
        String diff = change.getDiffContent();

        assertEquals(1, change.getLinesAdded(), "added line count");
        assertEquals(1, change.getLinesDeleted(), "deleted line count");
        assertTrue(diff.contains("-line-050"), () -> "missing deleted line in:\n" + diff);
        assertTrue(diff.contains("+line-050 changed"), () -> "missing added line in:\n" + diff);
        assertEquals(2, countChangedLines(diff),
                "CRLF/LF normalization must not report every line as changed, but got:\n" + diff);
    }

    private FileChange fileChangeFor(String beforeContent, String afterContent) {
        FilePath path = filePath("/repo/src/Example.java");
        Change change = new Change(revision(path, beforeContent), revision(path, afterContent));

        List<FileChange> changes = FileChange.fromGitChanges(List.of(change), List.of());

        assertEquals(1, changes.size());
        return changes.get(0);
    }

    private static List<String> numberedLines(int fromInclusive, int toInclusive) {
        List<String> lines = new ArrayList<>();
        for (int i = fromInclusive; i <= toInclusive; i++) {
            lines.add(String.format("line-%03d", i));
        }
        return lines;
    }

    private static int countChangedLines(String diff) {
        int count = 0;
        for (String line : diff.split("\n")) {
            if (line.startsWith("+++ ") || line.startsWith("--- ")) {
                continue;
            }
            if (line.startsWith("+") || line.startsWith("-")) {
                count++;
            }
        }
        return count;
    }

    private ContentRevision revision(FilePath path, String content) {
        return proxy(ContentRevision.class, (method, args) -> switch (method.getName()) {
            case "getFile" -> path;
            case "getContent" -> content;
            default -> defaultValue(method.getReturnType());
        });
    }

    private FilePath filePath(String path) {
        return proxy(FilePath.class, (method, args) -> switch (method.getName()) {
            case "getPath", "getPresentableUrl" -> path;
            case "getName" -> "Example.java";
            default -> defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + " test proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return invocation.invoke(method, args);
                }
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return 0;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(Method method, Object[] args);
    }
}
