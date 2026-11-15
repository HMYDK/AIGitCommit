package com.hmydk.aigit.analyzer;

import com.hmydk.aigit.context.AIPromptBuilder;
import com.hmydk.aigit.context.ChangeStatistics;
import com.hmydk.aigit.context.CommitContext;
import com.hmydk.aigit.context.FileChange;
import com.hmydk.aigit.context.FileChangeType;
import com.hmydk.aigit.context.ProjectInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for issue #98: the generated message said {@code refactor(core): ...} for a
 * small functional edit.
 *
 * <p>The commit type and scope were copied from the prompt metadata: {@code detectChangePattern}
 * reported {@code REFACTORING} whenever more than 70% of the changed files were modified, which is
 * true for almost every commit, and the prompt told the model to "leverage analysis.pattern for
 * better type selection". A plain "this file was modified" is not evidence of refactoring, so the
 * metadata must only claim it when the edit volume actually looks like a rewrite, and the prompt
 * must tell the model to verify the heuristic against the real diff.</p>
 */
class ContextAnalyzerPatternTest {

    @Test
    void smallFunctionalEditInTwoModifiedFilesIsNotRefactoring() {
        CommitContext context = contextOf(
                change("app/api/v1/controller/AccountController.php", 2, 0),
                change("app/api/v1/service/rental/AccountSearchService.php", 6, 1)
        );

        ContextAnalyzer.ChangePattern pattern = new ContextAnalyzer(null).analyze(context).getPattern();

        assertNotEquals(ContextAnalyzer.ChangePattern.REFACTORING, pattern,
                "a 9-line functional edit in two files is not a refactoring");
    }

    @Test
    void balancedLargeRewriteIsStillReportedAsRefactoring() {
        CommitContext context = contextOf(
                change("app/api/v1/controller/AccountController.php", 150, 140),
                change("app/api/v1/service/rental/AccountSearchService.php", 90, 95)
        );

        ContextAnalyzer.ChangePattern pattern = new ContextAnalyzer(null).analyze(context).getPattern();

        assertEquals(ContextAnalyzer.ChangePattern.REFACTORING, pattern,
                "a balanced 475-line rewrite without new files is the case the heuristic exists for");
    }

    @Test
    void newFileStillReportsFeatureDevelopment() {
        CommitContext context = contextOf(
                changeOfType("app/api/v1/service/rental/AccountSearchService.php", FileChangeType.ADDED, 120, 0),
                change("app/api/v1/controller/AccountController.php", 1, 1)
        );

        ContextAnalyzer.ChangePattern pattern = new ContextAnalyzer(null).analyze(context).getPattern();

        assertEquals(ContextAnalyzer.ChangePattern.FEATURE_DEVELOPMENT, pattern);
    }

    @Test
    void promptTellsTheModelToVerifyHeuristicsAgainstTheDiff() {
        CommitContext context = contextOf(
                change("app/api/v1/controller/AccountController.php", 2, 0),
                change("app/api/v1/service/rental/AccountSearchService.php", 6, 1)
        );
        ContextAnalyzer analyzer = new ContextAnalyzer(null);

        String prompt = new AIPromptBuilder(analyzer.analyze(context))
                .buildIntelligent(context, AIPromptBuilder.InputMode.FULL);

        assertFalse(prompt.contains("Leverage analysis.pattern"),
                "the prompt must not tell the model to trust the file-level pattern blindly");
        assertTrue(prompt.contains("verify"),
                "the prompt must tell the model to verify the heuristics against the real diff");
    }

    private static CommitContext contextOf(FileChange... changes) {
        List<FileChange> list = List.of(changes);
        return new CommitContext(
                new ProjectInfo("demo", "/repo", "main", true),
                null,
                list,
                ChangeStatistics.from(list),
                null
        );
    }

    private static FileChange change(String path, int added, int deleted) {
        return changeOfType(path, FileChangeType.MODIFIED, added, deleted);
    }

    private static FileChange changeOfType(String path, FileChangeType type, int added, int deleted) {
        String extension = path.substring(path.lastIndexOf('.') + 1);
        return new FileChange(path, type, "PHP", extension, added, deleted,
                "[MODIFY]: " + path + "\n", "Update " + path);
    }
}
