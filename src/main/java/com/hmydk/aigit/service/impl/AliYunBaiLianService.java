package com.hmydk.aigit.service.impl;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.AIService;
import com.hmydk.aigit.util.OpenAIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * AliYunBaiLianService
 *
 * @author hmydk
 */
public class AliYunBaiLianService implements AIService {

    private static final Logger log = LoggerFactory.getLogger(AliYunBaiLianService.class);

    @Override
    public boolean generateByStream() {
        return true;
    }

    @Override
    public String generateCommitMessage(String content) throws Exception {
        return "null";
    }

    @Override
    public void generateCommitMessageStream(String content, Consumer<String> onNext)
            throws Exception {
        OpenAIUtil.getAIResponseStream(Constants.阿里云百炼, content, onNext);
    }

    @Override
    public boolean checkNecessaryModuleConfigIsRight() {
        return OpenAIUtil.checkNecessaryModuleConfigIsRight(Constants.阿里云百炼);
    }
}
