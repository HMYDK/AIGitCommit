package com.hmydk.aigit.context;

/**
 * 文件变更类型枚举
 * Linus式设计：简单明确，无特殊情况
 */
public enum FileChangeType {
    ADDED("新增"),
    MODIFIED("修改"),
    DELETED("删除"),
    MOVED("移动");
    
    private final String description;
    
    FileChangeType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return name();
    }
}