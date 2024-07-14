package com.hmydk.aigit;

import com.hmydk.aigit.service.CommitMessageService;
import com.hmydk.aigit.util.GItCommitUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.vcs.commit.AbstractCommitWorkflowHandler;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.util.Collection;
import java.util.List;

public class GenerateCommitMessageAction extends AnAction {

    private final CommitMessageService commitMessageService = new CommitMessageService();

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        //check api key
        if (!commitMessageService.checkApiKey()) {
            Messages.showWarningDialog(project, "Please set your API key first.", "No API Key Set");
            return;
        }


        var commitWorkflowHandler = (AbstractCommitWorkflowHandler<?, ?>) e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
        if (commitWorkflowHandler == null) {
            Messages.showWarningDialog(project, "No changes selected. Please select files to commit.", "No Changes Selected");
            return;
        }

        List<Change> includedChanges = commitWorkflowHandler.getUi().getIncludedChanges();
        if (includedChanges.isEmpty()) {
            Messages.showWarningDialog(project, "No changes selected. Please select files to commit.", "No Changes Selected");
            return;
        }
        CommitMessageI data = VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(e.getDataContext());

        List<String> gitHistoryMsg = GItCommitUtil.computeGitHistoryMsg(project, 10);
        String diff = GItCommitUtil.computeDiff(includedChanges, project);
        String branch = GItCommitUtil.commonBranch(includedChanges, project);


        try {
            String commitMessage = commitMessageService.generateCommitMessage(branch, diff, gitHistoryMsg);
            if (commitMessageService.showCommitMessageDialog(project, commitMessage)) {
                copyToClipboard(commitMessage);
                Messages.showInfoMessage(project, "Commit message has been copied to clipboard.", "Message Copied");
            }
        } catch (IllegalArgumentException ex) {
            Messages.showWarningDialog(project, ex.getMessage(), "AI Commit Message Warning");
        } catch (Exception ex) {
            Messages.showErrorDialog(project, "Error generating commit message: " + ex.getMessage(), "Error");
        }
    }

    private Collection<Change> getSelectedChanges(Project project) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        LocalChangeList activeChangeList = changeListManager.getDefaultChangeList();
        return activeChangeList.getChanges();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable/disable the action based on whether there's an open project
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private void copyToClipboard(String text) {
        CopyPasteManager.getInstance().setContents(new StringSelection(text));
    }
}