package com.hmydk.aigit;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.vcs.commit.CommitWorkflowUi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Snapshot of the files and commit-message control exposed by the current commit UI.
 *
 * <p>This class deliberately depends on public IntelliJ interfaces only. Keeping the action
 * independent from concrete workflow-handler and commit-editor implementations lets the same
 * plugin artifact work across IDE releases.</p>
 */
record CommitSelection(
        @NotNull List<Change> includedChanges,
        @NotNull List<FilePath> includedUnversionedFiles,
        @NotNull CommitMessageI commitMessage
) {

    static @Nullable CommitSelection from(@NotNull AnActionEvent event) {
        return from(
                event.getData(VcsDataKeys.COMMIT_WORKFLOW_UI),
                event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)
        );
    }

    static @Nullable CommitSelection from(@Nullable CommitWorkflowUi workflowUi,
                                          @Nullable CommitMessageI commitMessage) {
        if (workflowUi == null || commitMessage == null) {
            return null;
        }

        return new CommitSelection(
                List.copyOf(workflowUi.getIncludedChanges()),
                List.copyOf(workflowUi.getIncludedUnversionedFiles()),
                commitMessage
        );
    }

    boolean isEmpty() {
        return includedChanges.isEmpty() && includedUnversionedFiles.isEmpty();
    }
}
