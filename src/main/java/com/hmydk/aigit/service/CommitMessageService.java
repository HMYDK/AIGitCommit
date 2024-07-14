package com.hmydk.aigit.service;


import com.hmydk.aigit.service.impl.GeminiService;
import com.hmydk.aigit.util.PromptUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.util.List;

public class CommitMessageService {
    private final GeminiService aiService;

    public CommitMessageService() {
        this.aiService = new GeminiService();
    }

    public boolean showCommitMessageDialog(Project project, String commitMessage) {
        int result = Messages.showYesNoDialog(
                project,
                "Use the following AI-generated commit message?\n\n" + commitMessage,
                "AI Commit Message",
                Messages.getQuestionIcon()
        );
        return result == Messages.YES;
    }

    public boolean checkApiKey() {
        return aiService.checkApiKey();
    }

    public String generateCommitMessage(String branch, String diff, List<String> gitHistoryMsg) {
        String prompt = PromptUtil.constructPrompt(diff, branch, gitHistoryMsg);
        return aiService.generateCommitMessage(prompt);
    }
}
