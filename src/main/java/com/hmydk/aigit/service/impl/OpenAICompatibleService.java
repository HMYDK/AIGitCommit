package com.hmydk.aigit.service.impl;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.AIService;
import com.hmydk.aigit.util.OpenAIUtil;

/**
 * OpenAICompatibleService
 * Support for OpenAI-compatible services (custom Base URL + API Key + Model ID)
 *
 * @author KaoriEl
 */
public class OpenAICompatibleService implements AIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAICompatibleService.class);

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
        OpenAIUtil.getAIResponseStream(Constants.OpenAI_Compatible, content, onNext, onError, onComplete);
    }

    @Override
    public boolean checkNecessaryModuleConfigIsRight() {
        return OpenAIUtil.checkNecessaryModuleConfigIsRight(Constants.OpenAI_Compatible);
    }
}

