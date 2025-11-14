package com.hmydk.aigit.service.impl;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.AIService;
import com.hmydk.aigit.util.OpenAIUtil;

import java.util.function.Consumer;

/**
 * Kimi Service Implementation
 * Uses Moonshot AI's OpenAI-compatible API
 *
 * @author hmydk
 */
public class KimiService implements AIService {

    @Override
    public boolean generateByStream() {
        return true;
    }

    @Override
    public String generateCommitMessage(String content) {
        return "null";
    }

    @Override
    public void generateCommitMessageStream(String content, Consumer<String> onNext, Consumer<Throwable> onError, Runnable onComplete) throws Exception {
        OpenAIUtil.getAIResponseStream(Constants.Kimi, content, onNext, onError, onComplete);
    }

    @Override
    public boolean checkNecessaryModuleConfigIsRight() {
        return OpenAIUtil.checkNecessaryModuleConfigIsRight(Constants.Kimi);
    }
}