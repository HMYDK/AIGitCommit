package com.hmydk.aigit.service.impl;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.AIService;
import com.hmydk.aigit.util.OpenAIUtil;

import java.util.function.Consumer;

public class DeepSeekAPIService implements AIService {
    @Override
    public boolean generateByStream() {
        return true;
    }

    @Override
    public String generateCommitMessage(String content) throws Exception {
        return "null";
    }

    @Override
    public void generateCommitMessageStream(String content, Consumer<String> onNext) throws Exception {
        OpenAIUtil.getAIResponseStream(Constants.DeepSeek, content, onNext);
    }

    @Override
    public boolean checkNecessaryModuleConfigIsRight() {
        return OpenAIUtil.checkNecessaryModuleConfigIsRight(Constants.DeepSeek);
    }
}
