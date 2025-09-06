package com.hmydk.aigit.context;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 变更统计信息
 * Linus式设计：数据驱动，消除猜测
 */
public class ChangeStatistics {
    private final int filesChanged;
    private final int linesAdded;
    private final int linesDeleted;
    private final ChangeType primaryType;
    private final String scope;
    private final int complexity;
    private final Map<String, Integer> languageDistribution;
    
    public ChangeStatistics(int filesChanged, int linesAdded, int linesDeleted,
                          ChangeType primaryType, String scope, int complexity,
                          Map<String, Integer> languageDistribution) {
        this.filesChanged = filesChanged;
        this.linesAdded = linesAdded;
        this.linesDeleted = linesDeleted;
        this.primaryType = primaryType;
        this.scope = scope;
        this.complexity = complexity;
        this.languageDistribution = languageDistribution;
    }
    
    public static ChangeStatistics analyze(List<FileChange> changes) {
        if (changes.isEmpty()) {
            return new ChangeStatistics(0, 0, 0, ChangeType.OTHER, "none", 0, new HashMap<>());
        }
        
        int filesChanged = changes.size();
        int linesAdded = changes.stream().mapToInt(FileChange::getLinesAdded).sum();
        int linesDeleted = changes.stream().mapToInt(FileChange::getLinesDeleted).sum();
        
        // 智能推断变更类型
        ChangeType primaryType = inferChangeType(changes);
        
        // 推断影响范围
        String scope = inferScope(changes);
        
        // 计算复杂度
        int complexity = calculateComplexity(changes);
        
        // 语言分布统计
        Map<String, Integer> languageDistribution = changes.stream()
            .collect(Collectors.groupingBy(
                FileChange::getLanguage,
                Collectors.summingInt(c -> 1)
            ));
        
        return new ChangeStatistics(filesChanged, linesAdded, linesDeleted, primaryType, scope, complexity, languageDistribution);
    }
    
    /**
     * 别名方法，保持API一致性
     */
    public static ChangeStatistics from(List<FileChange> changes) {
        return analyze(changes);
    }
    
    private static ChangeType inferChangeType(List<FileChange> changes) {
        // 基于文件路径和变更内容智能推断
        boolean hasTestFiles = changes.stream()
            .anyMatch(c -> c.getPath().contains("test") || c.getPath().contains("Test"));
        
        boolean hasNewFiles = changes.stream()
            .anyMatch(c -> c.getType() == FileChangeType.ADDED);
        
        boolean hasOnlyDeletions = changes.stream()
            .allMatch(c -> c.getLinesAdded() == 0 && c.getLinesDeleted() > 0);
        
        boolean hasDocFiles = changes.stream()
            .anyMatch(c -> c.getPath().endsWith(".md") || c.getPath().endsWith(".txt"));
        
        // 简单的推断逻辑
        if (hasTestFiles && !hasNewFiles) return ChangeType.TEST;
        if (hasOnlyDeletions) return ChangeType.REFACTOR;
        if (hasDocFiles && changes.size() == 1) return ChangeType.DOCS;
        if (hasNewFiles) return ChangeType.FEAT;
        
        return ChangeType.FIX; // 默认为修复
    }
    
    private static String inferScope(List<FileChange> changes) {
        // 基于文件路径推断影响范围
        Map<String, Long> pathCounts = changes.stream()
            .map(c -> extractScope(c.getPath()))
            .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        
        return pathCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("core");
    }
    
    private static String extractScope(String path) {
        // 从路径中提取范围
        if (path.contains("/service/")) return "service";
        if (path.contains("/util/")) return "util";
        if (path.contains("/config/")) return "config";
        if (path.contains("/test/")) return "test";
        if (path.contains("/ui/") || path.contains("/view/")) return "ui";
        return "core";
    }
    
    private static int calculateComplexity(List<FileChange> changes) {
        // 基于变更规模和文件数量计算复杂度
        int totalLines = changes.stream()
            .mapToInt(c -> c.getLinesAdded() + c.getLinesDeleted())
            .sum();
        
        int fileCount = changes.size();
        
        // 简单的复杂度计算
        if (totalLines > 500 || fileCount > 10) return 3; // 高复杂度
        if (totalLines > 100 || fileCount > 5) return 2;  // 中复杂度
        return 1; // 低复杂度
    }
    
    // Getters
    public int getFilesChanged() { return filesChanged; }
    public int getLinesAdded() { return linesAdded; }
    public int getLinesDeleted() { return linesDeleted; }
    public int getTotalLines() { return linesAdded + linesDeleted; }
    public ChangeType getPrimaryType() { return primaryType; }
    public String getScope() { return scope; }
    public int getComplexity() { return complexity; }
    public Map<String, Integer> getLanguageDistribution() { return languageDistribution; }
    
    @Override
    public String toString() {
        return String.format("ChangeStatistics{files=%d, +%d/-%d, type=%s, scope=%s}", 
                           filesChanged, linesAdded, linesDeleted, primaryType, scope);
    }
}