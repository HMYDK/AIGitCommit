package com.hmydk.aigit.service;

import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.context.AIPromptBuilder;
import com.hmydk.aigit.context.ChangeStatistics;
import com.hmydk.aigit.context.CommitContext;
import com.hmydk.aigit.context.FileChange;
import com.hmydk.aigit.context.FileChangeType;
import com.hmydk.aigit.context.ProjectInfo;
import com.hmydk.aigit.pojo.PromptInfo;
import com.hmydk.aigit.util.PromptUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Headless IntelliJ Platform integration test for the configurable prompt size of issue #96.
 *
 * <p>The commit action resolves the prompt through
 * {@link CommitMessageService#resolvePrompt(com.intellij.openapi.project.Project, CommitContext)},
 * which reads the limit from the persisted settings. This test boots the real platform application,
 * changes the setting, and verifies that the same change set switches between compacted and full
 * prompt modes. No provider is called and no network access is needed.</p>
 */
public class MaxPromptLengthIntegrationTest extends BasePlatformTestCase {

    private int previousMaxPromptCharacters;
    private PromptInfo previousCustomPrompt;
    private List<PromptInfo> previousCustomPrompts;
    private String previousPromptType;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ApiKeySettings settings = ApiKeySettings.getInstance();
        assertNotNull("ApiKeySettings application service is not registered", settings);

        previousMaxPromptCharacters = settings.getMaxPromptCharacters();
        previousCustomPrompt = settings.getCustomPrompt();
        previousCustomPrompts = new ArrayList<>(settings.getCustomPrompts());
        previousPromptType = settings.getPromptType();

        settings.setPromptType(Constants.CUSTOM_PROMPT);
        settings.setCustomPrompt(new PromptInfo("Default", PromptUtil.DEFAULT_PROMPT_1));
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            ApiKeySettings settings = ApiKeySettings.getInstance();
            if (settings != null) {
                settings.setMaxPromptCharacters(previousMaxPromptCharacters);
                settings.setCustomPrompt(previousCustomPrompt);
                settings.setCustomPrompts(previousCustomPrompts);
                settings.setPromptType(previousPromptType);
            }
        } finally {
            super.tearDown();
        }
    }

    public void testConfiguredPromptLimitControlsPromptCompaction() throws Exception {
        CommitContext context = contextWith(24, 12);
        CommitMessageService service = new CommitMessageService();
        ApiKeySettings settings = ApiKeySettings.getInstance();

        settings.setMaxPromptCharacters(Constants.DEFAULT_MAX_PROMPT_CHARACTERS);
        CommitMessageService.ResolvedPrompt defaultLimit = service.resolvePrompt(getProject(), context);
        assertFalse("the sample change set must exceed the default 20000-character budget: "
                        + defaultLimit.mode(),
                AIPromptBuilder.InputMode.FULL.equals(defaultLimit.mode()));
        assertTrue("the compacted prompt must stay within the default budget: "
                        + defaultLimit.prompt().length(),
                defaultLimit.prompt().length() <= Constants.DEFAULT_MAX_PROMPT_CHARACTERS);

        settings.setMaxPromptCharacters(100_000);
        CommitMessageService.ResolvedPrompt raisedLimit = service.resolvePrompt(getProject(), context);
        assertEquals("raising the limit must keep the full diff in the prompt",
                AIPromptBuilder.InputMode.FULL, raisedLimit.mode());
        assertTrue("prompt should fit the raised budget: " + raisedLimit.prompt().length(),
                raisedLimit.prompt().length() <= 100_000);
        assertTrue("the full prompt must contain the real diff content",
                raisedLimit.prompt().contains("new line 0 value"));

        settings.setMaxPromptCharacters(6_000);
        CommitMessageService.ResolvedPrompt loweredLimit = service.resolvePrompt(getProject(), context);
        assertFalse("lowering the limit must compact the prompt again: " + loweredLimit.mode(),
                AIPromptBuilder.InputMode.FULL.equals(loweredLimit.mode()));
        assertTrue("the lowered budget must produce a smaller prompt: "
                        + loweredLimit.prompt().length() + " vs " + raisedLimit.prompt().length(),
                loweredLimit.prompt().length() < raisedLimit.prompt().length());
    }

    private CommitContext contextWith(int fileCount, int changedLinePairs) {
        List<FileChange> changes = new ArrayList<>();
        for (int i = 0; i < fileCount; i++) {
            String path = "/repo/src/module/File" + i + ".java";
            changes.add(new FileChange(
                    path,
                    FileChangeType.MODIFIED,
                    "Java",
                    "java",
                    changedLinePairs,
                    changedLinePairs,
                    diffFor(path, changedLinePairs),
                    "Update File" + i + ".java with important logic changes"));
        }

        ProjectInfo projectInfo = new ProjectInfo("demo", "/tmp/demo", "main", true);
        ChangeStatistics statistics = ChangeStatistics.from(changes);
        return new CommitContext(projectInfo, null, changes, statistics, Map.of());
    }

    private static String diffFor(String path, int changedLinePairs) {
        StringBuilder diff = new StringBuilder();
        diff.append("[MODIFY]: ").append(path).append('\n');
        diff.append("--- a/").append(path).append('\n');
        diff.append("+++ b/").append(path).append('\n');
        for (int i = 0; i < changedLinePairs; i++) {
            diff.append("-old line ").append(i).append(" value ").append("a".repeat(32)).append('\n');
            diff.append("+new line ").append(i).append(" value ").append("b".repeat(32)).append('\n');
        }
        return diff.toString();
    }
}
