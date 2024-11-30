package com.hmydk.aigit.service;


import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.impl.*;
import com.hmydk.aigit.util.PromptUtil;

import java.util.function.Consumer;

public class CommitMessageService {
    private final AIService aiService;

    ApiKeySettings settings = ApiKeySettings.getInstance();

    public CommitMessageService() {
        String selectedClient = settings.getSelectedClient();
        this.aiService = getAIService(selectedClient);
    }

    public boolean checkNecessaryModuleConfigIsRight() {
        return aiService.checkNecessaryModuleConfigIsRight();
    }

    public String generateCommitMessage(String diff) throws Exception{
        String prompt = PromptUtil.constructPrompt(diff);
        return aiService.generateCommitMessage(prompt);
    }

    public void generateCommitMessageStream(String diff, Consumer<String> onNext, Consumer<Throwable> onError) throws Exception{
        String prompt = PromptUtil.constructPrompt(diff);
        System.out.println(prompt);
        aiService.generateCommitMessageStream(prompt, onNext);
    }

    public boolean generateByStream() {
        return aiService.generateByStream();
    }


    public static AIService getAIService(String selectedClient) {
        return switch (selectedClient) {
            case Constants.Ollama -> new OllamaService();
            case Constants.Gemini -> new GeminiService();
            case Constants.OpenAI_API -> new OpenAIAPIService();
            case Constants.CloudflareWorkersAI -> new CloudflareWorkersAIService();
            case Constants.阿里云百炼 -> new AliYunBaiLianService();
            case Constants.SiliconFlow -> new SiliconFlowService();
            default -> throw new IllegalArgumentException("Invalid LLM client: " + selectedClient);
        };
    }

}
