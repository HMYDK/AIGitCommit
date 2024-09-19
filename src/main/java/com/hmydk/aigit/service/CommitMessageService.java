package com.hmydk.aigit.service;


import com.hmydk.aigit.service.impl.OllamaService;
import com.hmydk.aigit.util.PromptUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

public class CommitMessageService {
    private final AIService aiService;

    public CommitMessageService() {
        this.aiService = new OllamaService();
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

    public boolean checkApiKeyIsExists() {
        return aiService.checkApiKeyIsExists();
    }

    public boolean validateConfig(String model, String apiKey, String language) {
        return aiService.validateConfig(model, apiKey, language);
    }

    public String generateCommitMessage(String diff) {
        String prompt = PromptUtil.constructPrompt(diff);
        return aiService.generateCommitMessage(prompt);
    }


}
