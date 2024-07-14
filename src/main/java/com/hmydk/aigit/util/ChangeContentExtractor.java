package com.hmydk.aigit.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ChangeContentExtractor {

    public static List<String> extractChangeContent(@NotNull Project project, @NotNull Change change) {
        List<String> changeContent = new ArrayList<>();

        ContentRevision beforeRevision = change.getBeforeRevision();
        ContentRevision afterRevision = change.getAfterRevision();

        try {
            if (beforeRevision != null && afterRevision != null) {
                // Modified file
                String beforeContent = beforeRevision.getContent();
                String afterContent = afterRevision.getContent();
                if (beforeContent != null && afterContent != null) {
                    changeContent.add("--- " + beforeRevision.getFile().getPath());
                    changeContent.add("+++ " + afterRevision.getFile().getPath());
                    changeContent.addAll(generateDiff(beforeContent, afterContent));
                }
            } else if (beforeRevision == null && afterRevision != null) {
                // New file
                String content = afterRevision.getContent();
                if (content != null) {
                    changeContent.add("+++ " + afterRevision.getFile().getPath());
                    changeContent.add(content);
                }
            } else if (beforeRevision != null && afterRevision == null) {
                // Deleted file
                String content = beforeRevision.getContent();
                if (content != null) {
                    changeContent.add("--- " + beforeRevision.getFile().getPath());
                    changeContent.add("File deleted");
                }
            }
        } catch (Exception e) {
            changeContent.add("Error extracting change content: " + e.getMessage());
        }

        return changeContent;
    }

    private static List<String> generateDiff(String beforeContent, String afterContent) {
        // This is a simplified diff. For a real-world scenario, you might want to use
        // a proper diff algorithm or library.
        List<String> diff = new ArrayList<>();
        String[] beforeLines = beforeContent.split("\n");
        String[] afterLines = afterContent.split("\n");

        for (int i = 0; i < Math.max(beforeLines.length, afterLines.length); i++) {
            if (i < beforeLines.length && i < afterLines.length) {
                if (!beforeLines[i].equals(afterLines[i])) {
                    diff.add("- " + beforeLines[i]);
                    diff.add("+ " + afterLines[i]);
                }
            } else if (i < beforeLines.length) {
                diff.add("- " + beforeLines[i]);
            } else if (i < afterLines.length) {
                diff.add("+ " + afterLines[i]);
            }
        }

        return diff;
    }
}
