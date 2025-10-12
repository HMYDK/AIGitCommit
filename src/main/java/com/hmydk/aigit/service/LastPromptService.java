package com.hmydk.aigit.service;

import com.intellij.openapi.project.Project;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存最近一次生成使用的 Prompt（按项目维度）
 */
public class LastPromptService {
    private static final Map<Project, String> LAST_PROMPT_MAP = new ConcurrentHashMap<>();

    public static void setLastPrompt(Project project, String prompt) {
        if (project == null || prompt == null) return;
        LAST_PROMPT_MAP.put(project, prompt);
    }

    public static String getLastPrompt(Project project) {
        return LAST_PROMPT_MAP.get(project);
    }
}