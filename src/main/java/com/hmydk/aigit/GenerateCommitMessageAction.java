package com.hmydk.aigit;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.CommitMessageService;
import com.hmydk.aigit.util.GItCommitUtil;
import com.hmydk.aigit.util.IdeaDialogUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.ui.CommitMessage;
import com.intellij.vcs.commit.AbstractCommitWorkflowHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GenerateCommitMessageAction extends AnAction {
    
    private final CommitMessageService commitMessageService = new CommitMessageService();
    
    /**
     * 获取CommitMessage对象
     */
    private CommitMessage getCommitMessage(AnActionEvent e) {
        return (CommitMessage) e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        CommitMessage commitMessage = getCommitMessage(e);
        
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        //check api key
        if (!commitMessageService.checkApiKeyIsExists()) {
            IdeaDialogUtil.handleApiKeyMissing(project);
            return;
        }
        
        AbstractCommitWorkflowHandler<?, ?> commitWorkflowHandler = (AbstractCommitWorkflowHandler<?, ?>) e.getData(
                VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
        if (commitWorkflowHandler == null) {
            IdeaDialogUtil.handleNoChangesSelected(project);
            return;
        }
        
        List<Change> includedChanges = commitWorkflowHandler.getUi().getIncludedChanges();

        if (includedChanges.isEmpty()){
            commitMessage.setCommitMessage(Constants.NO_FILE_SELECTED);
            return;
        }
        
        commitMessage.setCommitMessage(Constants.GENERATING_COMMIT_MESSAGE);
        
        // Run the time-consuming operations in a background task
        ProgressManager.getInstance().run(new Task.Backgroundable(project, Constants.TASK_TITLE, true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    String diff = GItCommitUtil.computeDiff(includedChanges, project);
                    String commitMessageFromAi = commitMessageService.generateCommitMessage(diff).trim();
                    ApplicationManager.getApplication().invokeLater(() -> {
                        commitMessage.setCommitMessage(commitMessageFromAi);
                    });
                } catch (IllegalArgumentException ex) {
                    IdeaDialogUtil.showWarning(project, ex.getMessage(), "AI Commit Message Warning");
                } catch (Exception ex) {
                    IdeaDialogUtil.showError(project, "Error generating commit message: " + ex.getMessage(), "Error");
                }
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
}