package com.hmydk.aigit.service;


import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.impl.GeminiService;
import com.hmydk.aigit.service.impl.OllamaService;
import com.hmydk.aigit.util.PromptUtil;

public class CommitMessageService {
    private final AIService aiService;

    ApiKeySettings settings = ApiKeySettings.getInstance();

    public CommitMessageService() {
        String selectedClient = settings.getSelectedClient();
        this.aiService = getAIService(selectedClient);
    }

    public boolean checkApiKeyIsExists() {
        return aiService.checkApiKeyIsExists();
    }

    public String generateCommitMessage(String diff) {
        String prompt = PromptUtil.constructPrompt(diff);
        return aiService.generateCommitMessage(prompt);
    }


    public static AIService getAIService(String selectedClient) {
        return switch (selectedClient) {
            case Constants.Ollama -> new OllamaService();
            case Constants.Gemini -> new GeminiService();
            default -> throw new IllegalArgumentException("Invalid LLM client: " + selectedClient);
        };
    }

}
