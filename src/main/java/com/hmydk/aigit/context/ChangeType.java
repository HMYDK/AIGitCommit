package com.hmydk.aigit.context;

/**
 * 变更类型枚举
 * 基于Conventional Commits规范
 * Linus式设计：简单枚举，无特殊情况
 */
public enum ChangeType {
    FEAT("feat", "新功能"),
    FIX("fix", "修复"),
    DOCS("docs", "文档"),
    STYLE("style", "格式"),
    REFACTOR("refactor", "重构"),
    PERF("perf", "性能"),
    TEST("test", "测试"),
    CHORE("chore", "构建"),
    OTHER("other", "其他");
    
    private final String code;
    private final String description;
    
    ChangeType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() { return code; }
    public String getDescription() { return description; }
    
    @Override
    public String toString() {
        return code;
    }
}