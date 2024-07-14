package com.hmydk.aigit;

import com.hmydk.aigit.service.CommitMessageService;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.checkin.CheckinHandler;

import java.util.Collection;

/**
 * AICommitMessageHandler
 *
 * @author hmydk
 */
public class AICommitMessageHandler extends CheckinHandler {
    private final CheckinProjectPanel panel;
    private final CommitMessageService commitMessageService;

    AICommitMessageHandler(CheckinProjectPanel panel) {
        this.panel = panel;
        this.commitMessageService = new CommitMessageService();
    }

    @Override
    public ReturnResult beforeCheckin() {
        try {
            Collection<Change> selectedChanges = panel.getSelectedChanges();
            if (selectedChanges.isEmpty()) {
                Messages.showWarningDialog(panel.getProject(), "No changes selected for commit.", "AI Commit Message Warning");
                return ReturnResult.CANCEL;
            }

            String commitMessage = commitMessageService.generateCommitMessage(panel.getProject(), panel.getSelectedChanges());
            if (commitMessageService.showCommitMessageDialog(panel.getProject(), commitMessage)) {
                panel.setCommitMessage(commitMessage);
            }
            return ReturnResult.COMMIT;
        } catch (IllegalArgumentException e) {
            Messages.showWarningDialog(panel.getProject(), e.getMessage(), "AI Commit Message Warning");
            return ReturnResult.COMMIT;
        } catch (Exception e) {
            Messages.showErrorDialog(panel.getProject(), "Failed to generate commit message: " + e.getMessage(), "AI Commit Message Error");
            return ReturnResult.CANCEL;
        }
    }
}