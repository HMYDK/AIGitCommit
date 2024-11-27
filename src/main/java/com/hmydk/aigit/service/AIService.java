package com.hmydk.aigit.service;

import java.util.Map;
import java.util.function.Consumer;

/**
 * AIService
 *
 * @author hmydk
 */
public interface AIService {

    boolean generateByStream();

    String generateCommitMessage(String content) throws Exception;

    void generateCommitMessageStream(String content, Consumer<String> onNext) throws Exception;

    boolean checkNecessaryModuleConfigIsRight();

    boolean validateConfig(Map<String,String> config);
}
