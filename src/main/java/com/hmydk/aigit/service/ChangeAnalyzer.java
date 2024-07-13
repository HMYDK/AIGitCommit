package com.hmydk.aigit.service;

import com.hmydk.aigit.pojo.AnalysisResult;
import com.hmydk.aigit.pojo.ChangeInfo;

import java.util.List;


/**
 * ChangeAnalyzer
 *
 * @author hmydk
 */
public interface ChangeAnalyzer {
    AnalysisResult analyzeChanges(List<ChangeInfo> changes);
}
