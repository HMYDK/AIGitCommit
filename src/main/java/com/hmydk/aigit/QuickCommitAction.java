package com.hmydk.aigit;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.vcs.commit.AbstractCommitWorkflowHandler;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * 快速提交Action类，用于通过快捷键打开commit窗口、选中所有文件并生成commit message
 */
public class QuickCommitAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // 检查是否有未提交的更改
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        if (changeListManager.getDefaultChangeList().getChanges().isEmpty()) {
            return; // 没有更改，不执行任何操作
        }
        
        // 使用ActionManager查找并执行内置的提交操作
        AnAction checkinAction = ActionManager.getInstance().getAction("CheckinProject");
        if (checkinAction != null) {
            // 打开commit窗口
            checkinAction.actionPerformed(e);
            
            // 使用延迟执行来确保窗口已完全打开
            EdtExecutorService.getScheduledExecutorInstance().schedule(() -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                        // 再次延迟一小段时间后生成commit message
                        EdtExecutorService.getScheduledExecutorInstance().schedule(() -> {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                // 从数据上下文中获取commit handler
                                DataContext dataContext = DataManager.getInstance().getDataContextFromFocus().getResult();
                                AbstractCommitWorkflowHandler<?, ?> handler = (AbstractCommitWorkflowHandler<?, ?>) dataContext.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
                                if (handler != null) {
                                    // 创建新的事件对象，包含正确的上下文
                                    AnActionEvent newEvent = AnActionEvent.createFromInputEvent(
                                        e.getInputEvent(),
                                        ActionPlaces.UNKNOWN,
                                        e.getPresentation(),
                                        createActionContext(project, handler)
                                    );
                                    
                                    // 使用新的事件对象触发生成操作
                                    GenerateCommitMessageAction generateAction = new GenerateCommitMessageAction();
                                    generateAction.actionPerformed(newEvent);
                                }
                            });
                        }, 800, TimeUnit.MILLISECONDS);
                });
            }, 500, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 创建包含必要上下文的DataContext
     */
    private DataContext createActionContext(Project project, AbstractCommitWorkflowHandler<?, ?> handler) {
        return dataId -> {
            if (CommonDataKeys.PROJECT.is(dataId)) {
                return project;
            }
            if (VcsDataKeys.COMMIT_WORKFLOW_HANDLER.is(dataId)) {
                return handler;
            }
            if (VcsDataKeys.COMMIT_MESSAGE_CONTROL.is(dataId)) {
                return handler.getCommitMessage();
            }
            // 添加更多可能需要的数据键
            if (VcsDataKeys.CHANGES.is(dataId)) {
                return handler.getUi().getIncludedChanges().toArray(new Change[0]);
            }
            return null;
        };
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        boolean enabled = false;
        
        if (project != null) {
            // 检查是否有版本控制系统
            ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
            if (vcsManager != null && vcsManager.hasActiveVcss()) {
                // 检查是否有未提交的更改
                ChangeListManager changeListManager = ChangeListManager.getInstance(project);
                enabled = !changeListManager.getDefaultChangeList().getChanges().isEmpty();
            }
        }
        
        // 设置操作的可用性
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
} 