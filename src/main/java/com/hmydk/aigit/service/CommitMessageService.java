package com.hmydk.aigit.service;

import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.context.AIPromptBuilder;
import com.hmydk.aigit.context.CommitContext;
import com.hmydk.aigit.service.impl.AliYunBaiLianService;
import com.hmydk.aigit.service.impl.CloudflareWorkersAIService;
import com.hmydk.aigit.service.impl.DeepSeekAPIService;
import com.hmydk.aigit.service.impl.GeminiService;
import com.hmydk.aigit.service.impl.KimiService;
import com.hmydk.aigit.service.impl.OllamaService;
import com.hmydk.aigit.service.impl.OpenAIAPIService;
import com.hmydk.aigit.service.impl.OpenAICompatibleService;
import com.hmydk.aigit.service.impl.OpenRouterService;
import com.hmydk.aigit.service.impl.SiliconFlowService;
import com.hmydk.aigit.service.impl.VolcEngineService;
import com.hmydk.aigit.util.PromptUtil;
import com.intellij.openapi.project.Project;

import java.util.function.Consumer;
import java.util.function.Function;

public class CommitMessageService {
    static final int MAX_PROMPT_LENGTH = 20_000;
    static final int SUMMARY_FILE_LIMIT = 60;

    private final AIService aiService;

    ApiKeySettings settings = ApiKeySettings.getInstance();

    public record ResolvedPrompt(String aiInput,
                                 String prompt,
                                 AIPromptBuilder.InputMode mode,
                                 Integer summaryFileLimit) {
    }

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

    public String generateCommitMessage(Project project, CommitContext context) throws Exception {
        ResolvedPrompt resolvedPrompt = resolvePrompt(project, context);
        LastPromptService.setLastPrompt(project, resolvedPrompt.prompt());
        return aiService.generateCommitMessage(resolvedPrompt.prompt());
    }

    public void generateCommitMessageStream(Project project,
                                            String diff,
                                            Consumer<String> onNext,
                                            Consumer<Throwable> onError,
                                            Runnable onComplete) throws Exception {
        String prompt = PromptUtil.constructPrompt(project, diff);
        LastPromptService.setLastPrompt(project, prompt);
        aiService.generateCommitMessageStream(prompt, onNext, onError, onComplete);
    }

    public void generateCommitMessageStream(Project project,
                                            CommitContext context,
                                            Consumer<String> onNext,
                                            Consumer<Throwable> onError,
                                            Runnable onComplete) throws Exception {
        ResolvedPrompt resolvedPrompt = resolvePrompt(project, context);
        LastPromptService.setLastPrompt(project, resolvedPrompt.prompt());
        aiService.generateCommitMessageStream(resolvedPrompt.prompt(), onNext, onError, onComplete);
    }

    public boolean generateByStream() {
        return aiService.generateByStream();
    }

    ResolvedPrompt resolvePrompt(Project project, CommitContext context) {
        return resolvePrompt(context, aiInput -> PromptUtil.constructPrompt(project, aiInput), MAX_PROMPT_LENGTH);
    }

    static ResolvedPrompt resolvePrompt(CommitContext context,
                                        Function<String, String> promptFactory,
                                        int maxPromptLength) {
        AIPromptBuilder builder = new AIPromptBuilder(context.analyze());
        ResolvedPrompt resolvedPrompt = buildPrompt(builder, context, promptFactory, AIPromptBuilder.InputMode.FULL, null);
        if (resolvedPrompt.prompt().length() <= maxPromptLength) {
            return resolvedPrompt;
        }

        resolvedPrompt = buildPrompt(builder, context, promptFactory, AIPromptBuilder.InputMode.COMPACT, null);
        if (resolvedPrompt.prompt().length() <= maxPromptLength) {
            return resolvedPrompt;
        }

        resolvedPrompt = buildPrompt(builder, context, promptFactory, AIPromptBuilder.InputMode.SUMMARY, null);
        if (resolvedPrompt.prompt().length() <= maxPromptLength) {
            return resolvedPrompt;
        }

        return buildPrompt(builder, context, promptFactory, AIPromptBuilder.InputMode.SUMMARY, SUMMARY_FILE_LIMIT);
    }

    private static ResolvedPrompt buildPrompt(AIPromptBuilder builder,
                                              CommitContext context,
                                              Function<String, String> promptFactory,
                                              AIPromptBuilder.InputMode mode,
                                              Integer summaryFileLimit) {
        String aiInput = builder.buildIntelligent(context, mode, summaryFileLimit);
        String prompt = promptFactory.apply(aiInput);
        return new ResolvedPrompt(aiInput, prompt, mode, summaryFileLimit);
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
