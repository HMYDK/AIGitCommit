package com.hmydk.aigit.service;

import java.util.Map;

/**
 * AIService
 *
 * @author hmydk
 */
public interface AIService {

    String generateCommitMessage(String content) throws Exception;

    boolean checkNecessaryModuleConfigIsRight();

    boolean validateConfig(Map<String,String> config);
}
