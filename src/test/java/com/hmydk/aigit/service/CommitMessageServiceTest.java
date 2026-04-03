package com.hmydk.aigit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmydk.aigit.context.AIPromptBuilder;
import com.hmydk.aigit.context.CommitContext;
import com.hmydk.aigit.context.FileChange;
import com.hmydk.aigit.context.FileChangeType;
import com.hmydk.aigit.context.ProjectInfo;
import com.hmydk.aigit.context.ChangeStatistics;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommitMessageServiceTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void keepsFullModeForSmallChanges() throws Exception {
        CommitContext context = createContext(1, 4, 16);

        CommitMessageService.ResolvedPrompt resolvedPrompt =
                CommitMessageService.resolvePrompt(context, aiInput -> aiInput, 20_000);

        Map<String, Object> json = extractJson(resolvedPrompt.aiInput());
        List<Map<String, Object>> changes = flattenChanges(json);

        assertEquals("full", json.get("input_mode"));
        assertFalse((Boolean) json.get("truncated"));
        assertEquals(0, json.get("omitted_full_diff_files"));
        assertTrue(changes.get(0).containsKey("full_diff_content"));
    }

    @Test
    void fallsBackToCompactModeForLargeChanges() throws Exception {
        CommitContext context = createContext(1, 220, 24);

        CommitMessageService.ResolvedPrompt resolvedPrompt =
                CommitMessageService.resolvePrompt(context, aiInput -> aiInput, 5_000);

        Map<String, Object> json = extractJson(resolvedPrompt.aiInput());
        List<Map<String, Object>> changes = flattenChanges(json);
        List<Map<String, Object>> focusedChanges = castList(json.get("focused_changes"));

        assertEquals("compact", json.get("input_mode"));
        assertTrue((Boolean) json.get("truncated"));
        assertFalse(changes.get(0).containsKey("full_diff_content"));
        assertTrue(changes.get(0).containsKey("diff_summary"));
        assertEquals(1, focusedChanges.size());
    }

    @Test
    void fallsBackToSummaryModeForHugeChanges() throws Exception {
        CommitContext context = createContext(24, 120, 28);

        CommitMessageService.ResolvedPrompt resolvedPrompt =
                CommitMessageService.resolvePrompt(context, aiInput -> aiInput, 8_000);

        Map<String, Object> json = extractJson(resolvedPrompt.aiInput());
        List<Map<String, Object>> changes = flattenChanges(json);

        assertEquals("summary", json.get("input_mode"));
        assertTrue((Boolean) json.get("truncated"));
        assertFalse(json.containsKey("focused_changes"));
        assertFalse(changes.get(0).containsKey("diff_summary"));
        assertFalse(changes.get(0).containsKey("full_diff_content"));
    }

    @Test
    void compactModeKeepsOnlySixFocusedChangesAndTruncatesDiffExcerpt() throws Exception {
        CommitContext context = createContext(8, 180, 20);

        String aiInput = new AIPromptBuilder(context.analyze())
                .buildIntelligent(context, AIPromptBuilder.InputMode.COMPACT);
        Map<String, Object> json = extractJson(aiInput);
        List<Map<String, Object>> focusedChanges = castList(json.get("focused_changes"));

        assertEquals("compact", json.get("input_mode"));
        assertEquals(6, focusedChanges.size());
        assertEquals(2, json.get("omitted_full_diff_files"));
        assertTrue(focusedChanges.get(0).get("diff_excerpt").toString().endsWith("…truncated"));
    }

    @Test
    void summaryModeFallsBackToTopSixtyFilesWhenStillTooLong() throws Exception {
        CommitContext context = createContext(80, 18, 12);

        CommitMessageService.ResolvedPrompt resolvedPrompt =
                CommitMessageService.resolvePrompt(context, aiInput -> aiInput, 10_000);

        Map<String, Object> json = extractJson(resolvedPrompt.aiInput());
        List<Map<String, Object>> changes = flattenChanges(json);

        assertEquals("summary", json.get("input_mode"));
        assertEquals(60, changes.size());
        assertEquals(20, json.get("omitted_files"));
        assertEquals(80, json.get("omitted_full_diff_files"));
        assertEquals(CommitMessageService.SUMMARY_FILE_LIMIT, resolvedPrompt.summaryFileLimit());
    }

    @Test
    void usesFinalPromptLengthInsteadOfRawInputLength() throws Exception {
        CommitContext context = createContext(1, 24, 18);
        String fullAiInput = new AIPromptBuilder(context.analyze())
                .buildIntelligent(context, AIPromptBuilder.InputMode.FULL);
        String fullPrompt = "X".repeat(2_300) + fullAiInput;

        CommitMessageService.ResolvedPrompt resolvedPrompt =
                CommitMessageService.resolvePrompt(context, aiInput -> "X".repeat(2_300) + aiInput, 3_000);

        Map<String, Object> json = extractJson(resolvedPrompt.aiInput());

        assertNotEquals("full", json.get("input_mode"));
        assertTrue(resolvedPrompt.prompt().length() < fullPrompt.length());
    }

    private CommitContext createContext(int fileCount, int changedLinePairs, int pathPadding) {
        List<FileChange> changes = new ArrayList<>();
        for (int i = 0; i < fileCount; i++) {
            String path = buildPath(i, pathPadding);
            String diff = buildDiff(path, changedLinePairs);
            changes.add(new FileChange(
                    path,
                    FileChangeType.MODIFIED,
                    "Java",
                    "java",
                    changedLinePairs,
                    changedLinePairs,
                    diff,
                    "Update " + path.substring(path.lastIndexOf('/') + 1) + " with important logic changes"
            ));
        }

        ProjectInfo projectInfo = new ProjectInfo("demo", "/tmp/demo", "main", true);
        ChangeStatistics statistics = ChangeStatistics.from(changes);
        return new CommitContext(projectInfo, null, changes, statistics, Map.of());
    }

    private String buildPath(int index, int pathPadding) {
        return "/repo/src/" + "module".repeat(Math.max(1, pathPadding / 6)) + "/File" + index + ".java";
    }

    private String buildDiff(String path, int changedLinePairs) {
        StringBuilder diff = new StringBuilder();
        diff.append("[MODIFY]: ").append(path).append("\n");
        diff.append("--- a/").append(path).append("\n");
        diff.append("+++ b/").append(path).append("\n");

        for (int i = 0; i < changedLinePairs; i++) {
            diff.append("-old line ").append(i).append(" value ").append("a".repeat(32)).append("\n");
            diff.append("+new line ").append(i).append(" value ").append("b".repeat(32)).append("\n");
        }

        return diff.toString();
    }

    private Map<String, Object> extractJson(String aiInput) throws Exception {
        int start = aiInput.indexOf('{');
        int end = aiInput.lastIndexOf('}');
        assertTrue(start >= 0 && end > start);
        return OBJECT_MAPPER.readValue(aiInput.substring(start, end + 1), new TypeReference<>() {
        });
    }

    private List<Map<String, Object>> flattenChanges(Map<String, Object> root) {
        Map<String, Object> categorizedChanges = castMap(root.get("categorized_changes"));
        List<Map<String, Object>> flattened = new ArrayList<>();
        for (Object value : categorizedChanges.values()) {
            flattened.addAll(castList(value));
        }
        assertFalse(flattened.isEmpty());
        return flattened;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        assertNotNull(value);
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        assertNotNull(value);
        return (List<Map<String, Object>>) value;
    }
}
