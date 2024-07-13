package com.hmydk.aigit.service;

import com.hmydk.aigit.pojo.AnalysisResult;

/**
 * MessageGenerator
 *
 * @author hmydk
 */
public class MessageGenerator {
    private final AIService aiService;

    public MessageGenerator(AIService aiService) {
        this.aiService = aiService;
    }

    public String generateMessage(AnalysisResult analysisResult) {
        return aiService.generateCommitMessage(analysisResult);
    }
}
