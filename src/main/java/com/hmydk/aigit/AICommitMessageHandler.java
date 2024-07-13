package com.hmydk.aigit;

import com.hmydk.aigit.pojo.AnalysisResult;
import com.hmydk.aigit.pojo.ChangeInfo;
import com.hmydk.aigit.service.ChangeAnalyzer;
import com.hmydk.aigit.service.ChangeCollector;
import com.hmydk.aigit.service.MessageGenerator;
import com.hmydk.aigit.service.impl.DefaultChangeAnalyzer;
import com.hmydk.aigit.service.impl.DefaultChangeCollector;
import com.hmydk.aigit.service.impl.OpenAIService;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.checkin.CheckinHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * AICommitMessageHandler
 *
 * @author hmydk
 */
public class AICommitMessageHandler extends CheckinHandler {
    private final CheckinProjectPanel panel;
    private final ChangeCollector changeCollector;
    private final ChangeAnalyzer changeAnalyzer;
    private final MessageGenerator messageGenerator;

    AICommitMessageHandler(CheckinProjectPanel panel) {
        this.panel = panel;
        this.changeCollector = new DefaultChangeCollector();
        this.changeAnalyzer = new DefaultChangeAnalyzer();
        this.messageGenerator = new MessageGenerator(new OpenAIService());
    }

    @Override
    public ReturnResult beforeCheckin() {
        Collection<Change> selectedChanges = panel.getSelectedChanges();

        if (selectedChanges == null || selectedChanges.isEmpty()) {
            Messages.showWarningDialog(
                    panel.getProject(),
                    "No changes selected for commit.",
                    "AI Commit Message Warning"
            );
            return ReturnResult.CANCEL;
        }

        List<Change> changes = new ArrayList<>(selectedChanges);

        try {
            List<ChangeInfo> changeInfos = changeCollector.collectChanges(changes);
            if (changeInfos.isEmpty()) {
                Messages.showWarningDialog(
                        panel.getProject(),
                        "No relevant changes found for commit message generation.",
                        "AI Commit Message Warning"
                );
                return ReturnResult.COMMIT; // Allow commit to proceed without generated message
            }

            AnalysisResult analysisResult = changeAnalyzer.analyzeChanges(changeInfos);
            String commitMessage = messageGenerator.generateMessage(analysisResult);

            // Ask user if they want to use the generated message
            int result = Messages.showYesNoDialog(
                    panel.getProject(),
                    "Use the following AI-generated commit message?\n\n" + commitMessage,
                    "AI Commit Message",
                    Messages.getQuestionIcon()
            );

            if (result == Messages.YES) {
                panel.setCommitMessage(commitMessage);
            }
            return ReturnResult.COMMIT;
        } catch (Exception e) {
            Messages.showErrorDialog(
                    panel.getProject(),
                    "Failed to generate commit message: " + e.getMessage(),
                    "AI Commit Message Error"
            );
            return ReturnResult.CANCEL;
        }
    }
}
