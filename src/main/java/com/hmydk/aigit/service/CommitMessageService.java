package com.hmydk.aigit.service;


import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.impl.*;
import com.hmydk.aigit.util.PromptUtil;
import com.intellij.openapi.project.Project;

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

    public String generateCommitMessage(Project project, String diff) throws Exception {
        String prompt = PromptUtil.constructPrompt(project, diff);
        LastPromptService.setLastPrompt(project, prompt);
        return aiService.generateCommitMessage(prompt);
    }

    public void generateCommitMessageStream(Project project, String diff, Consumer<String> onNext, Consumer<Throwable> onError, Runnable onComplete) throws Exception {
        String prompt = PromptUtil.constructPrompt(project, diff);
        LastPromptService.setLastPrompt(project, prompt);

        aiService.generateCommitMessageStream(prompt, onNext, onError, onComplete);
    }

    public boolean generateByStream() {
        return aiService.generateByStream();
    }


    public static AIService getAIService(String selectedClient) {
        return switch (selectedClient) {
            case Constants.Ollama -> new OllamaService();
            case Constants.Gemini -> new GeminiService();
            case Constants.DeepSeek -> new DeepSeekAPIService();
            case Constants.OpenAI_API -> new OpenAIAPIService();
            case Constants.OpenAI_Compatible -> new OpenAICompatibleService();
            case Constants.CloudflareWorkersAI -> new CloudflareWorkersAIService();
            case Constants.阿里云百炼 -> new AliYunBaiLianService();
            case Constants.SiliconFlow -> new SiliconFlowService();
            case Constants.VolcEngine -> new VolcEngineService();
            case Constants.OpenRouter -> new OpenRouterService();
            case Constants.Kimi -> new KimiService();
            default -> throw new IllegalArgumentException("Invalid LLM client: " + selectedClient);
        };
    }

}
