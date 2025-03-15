package com.hmydk.aigit.service.impl;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.AIService;
import com.hmydk.aigit.util.OpenAIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 *
 * <a href="https://openrouter.ai/">OpenRouter</a>
 *
 * @author hmydk
 */
public class OpenRouterService implements AIService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterService.class);

    @Override
    public boolean generateByStream() {
        return true;
    }

    @Override
    public String generateCommitMessage(String content) throws Exception {
        return "null";
    }

    @Override
    public void generateCommitMessageStream(String content, Consumer<String> onNext, Consumer<Throwable> onError, Runnable onComplete) throws Exception {
        OpenAIUtil.getAIResponseStream(Constants.OpenRouter, content, onNext, onError, onComplete);
    }

    @Override
    public boolean checkNecessaryModuleConfigIsRight() {
        return OpenAIUtil.checkNecessaryModuleConfigIsRight(Constants.OpenRouter);
    }
}
