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
        switch (selectedClient) {
            case Constants.Ollama:
                this.aiService = new OllamaService();
                break;
            case Constants.Gemini:
                this.aiService = new GeminiService();
                break;
            default:
                throw new IllegalArgumentException("Invalid LLM client: " + selectedClient);
        }
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
