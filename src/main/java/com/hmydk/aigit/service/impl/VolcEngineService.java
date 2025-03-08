package com.hmydk.aigit.service.impl;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.AIService;
import com.hmydk.aigit.util.OpenAIUtil;

import java.util.function.Consumer;

/**
 * <a href="https://www.volcengine.com/docs/82379/1099455">火山引擎</a>
 */
public class VolcEngineService implements AIService {
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
        OpenAIUtil.getAIResponseStream(Constants.VolcEngine, content, onNext, onError, onComplete);
    }

    @Override
    public boolean checkNecessaryModuleConfigIsRight() {
        return OpenAIUtil.checkNecessaryModuleConfigIsRight(Constants.VolcEngine);
    }
}
