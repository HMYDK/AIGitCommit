package com.hmydk.aigit.service;

import com.hmydk.aigit.pojo.AnalysisResult;

/**
 * AIService
 *
 * @author hmydk
 */
public interface AIService {
    String generateCommitMessage(AnalysisResult analysisResult);
}
