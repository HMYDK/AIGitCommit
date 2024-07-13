package com.hmydk.aigit;

import com.hmydk.aigit.pojo.AnalysisResult;
import com.hmydk.aigit.pojo.ChangeInfo;
import com.hmydk.aigit.service.AIService;
import com.hmydk.aigit.service.ChangeAnalyzer;
import com.hmydk.aigit.service.ChangeCollector;
import com.hmydk.aigit.service.MessageGenerator;
import com.hmydk.aigit.service.impl.DefaultChangeAnalyzer;
import com.hmydk.aigit.service.impl.DefaultChangeCollector;
import com.hmydk.aigit.service.impl.OpenAIService;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.util.List;

public class GenerateCommitMessageAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        List<Change> changes = (List<Change>) changeListManager.getAllChanges();

        if (changes.isEmpty()) {
            Messages.showInfoMessage(project, "No changes detected. Make some changes before generating a commit message.", "No Changes");
            return;
        }

        try {
            ChangeCollector changeCollector = new DefaultChangeCollector();
            ChangeAnalyzer changeAnalyzer = new DefaultChangeAnalyzer();
            AIService aiService = new OpenAIService(); // Ensure this is properly configured
            MessageGenerator messageGenerator = new MessageGenerator(aiService);

            List<ChangeInfo> changeInfos = changeCollector.collectChanges(changes);
            AnalysisResult analysisResult = changeAnalyzer.analyzeChanges(changeInfos);
            String commitMessage = messageGenerator.generateMessage(analysisResult);

            // Show the generated message to the user
            int result = Messages.showOkCancelDialog(
                    project,
                    "Generated Commit Message:\n\n" + commitMessage + "\n\nDo you want to use this message?",
                    "AI Generated Commit Message",
                    "Use Message",
                    "Cancel",
                    Messages.getQuestionIcon()
            );

            if (result == Messages.OK) {
                // Use the generated message
                // Note: We can't directly set the commit message here as we're not in a commit dialog
                // Instead, we'll copy it to the clipboard
                copyToClipboard(commitMessage);
                Messages.showInfoMessage(project, "Commit message has been copied to clipboard.", "Message Copied");
            }

        } catch (Exception ex) {
            Messages.showErrorDialog(project, "Error generating commit message: " + ex.getMessage(), "Error");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable/disable the action based on whether there's an open project
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private void copyToClipboard(String text) {
        CopyPasteManager.getInstance().setContents(new StringSelection(text));
    }
}