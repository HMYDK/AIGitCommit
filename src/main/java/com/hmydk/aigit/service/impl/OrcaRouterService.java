package com.hmydk.aigit.service.impl;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.AIService;
import com.hmydk.aigit.util.OpenAIUtil;

import java.util.function.Consumer;

/**
 * OrcaRouter service implementation using its OpenAI-compatible API.
 *
 * @see <a href="https://docs.orcarouter.ai/introduction">OrcaRouter documentation</a>
 */
public class OrcaRouterService implements AIService {

    @Override
    public boolean generateByStream() {
        return true;
    }

    @Override
    public String generateCommitMessage(String content) {
        return "null";
    }

    @Override
    public void generateCommitMessageStream(String content, Consumer<String> onNext,
                                            Consumer<Throwable> onError, Runnable onComplete) throws Exception {
        OpenAIUtil.getAIResponseStream(Constants.OrcaRouter, content, onNext, onError, onComplete);
    }

    @Override
    public boolean checkNecessaryModuleConfigIsRight() {
        return OpenAIUtil.checkNecessaryModuleConfigIsRight(Constants.OrcaRouter);
    }
}
