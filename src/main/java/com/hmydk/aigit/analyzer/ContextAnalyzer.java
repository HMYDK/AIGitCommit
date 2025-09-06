package com.hmydk.aigit.analyzer;

import com.hmydk.aigit.context.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Linus式智能分析器
 * "The key insight is that you have to design your data structures right."
 * 
 * 这个类的职责：
 * 1. 分析代码变更的语义上下文
 * 2. 提取关键信息供AI理解
 * 3. 消除噪音，突出重点
 * 
 * 设计原则：
 * - 一个方法只做一件事
 * - 数据结构驱动逻辑
 * - 消除特殊情况
 */
public class ContextAnalyzer {
    
    // 常见的配置文件模式
    private static final Set<String> CONFIG_EXTENSIONS = Set.of(
        "json", "xml", "yml", "yaml", "properties", "conf", "config", "ini"
    );
    
    // 测试文件模式
    private static final Pattern TEST_PATTERN = Pattern.compile(
        ".*[Tt]est.*|.*[Ss]pec.*|.*\\.test\\.|.*\\.spec\\."
    );
    
    // 文档文件模式
    private static final Set<String> DOC_EXTENSIONS = Set.of(
        "md", "txt", "rst", "adoc", "doc", "docx"
    );
    
    private final Project project;
    
    public ContextAnalyzer(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * 分析提交上下文
     * 这是核心方法，其他都是辅助
     */
    @NotNull
    public AnalysisResult analyze(@NotNull CommitContext context) {
        List<FileChange> changes = context.getChanges();
        
        // 分类文件变更
        Map<ChangeCategory, List<FileChange>> categorized = categorizeChanges(changes);
        
        // 分析变更模式
        ChangePattern pattern = detectChangePattern(changes);
        
        // 提取关键信息
        List<String> keyInsights = extractKeyInsights(changes, categorized, pattern);
        
        // 计算复杂度
        int complexity = calculateComplexity(changes);
        
        return new AnalysisResult(categorized, pattern, keyInsights, complexity);
    }
    
    /**
     * 文件变更分类
     * 消除if-else地狱，用数据结构驱动
     */
    @NotNull
    private Map<ChangeCategory, List<FileChange>> categorizeChanges(@NotNull List<FileChange> changes) {
        Map<ChangeCategory, List<FileChange>> result = new EnumMap<>(ChangeCategory.class);
        
        // 初始化所有分类
        for (ChangeCategory category : ChangeCategory.values()) {
            result.put(category, new ArrayList<>());
        }
        
        // 分类逻辑：简单直接
        for (FileChange change : changes) {
            ChangeCategory category = determineCategory(change);
            result.get(category).add(change);
        }
        
        return result;
    }
    
    /**
     * 确定文件变更类别
     * 一个函数，清晰的逻辑
     */
    @NotNull
    private ChangeCategory determineCategory(@NotNull FileChange change) {
        String path = change.getPath();
        String extension = change.getExtension();
        
        // 测试文件
        if (TEST_PATTERN.matcher(path).matches()) {
            return ChangeCategory.TEST;
        }
        
        // 配置文件
        if (CONFIG_EXTENSIONS.contains(extension)) {
            return ChangeCategory.CONFIG;
        }
        
        // 文档文件
        if (DOC_EXTENSIONS.contains(extension)) {
            return ChangeCategory.DOCUMENTATION;
        }
        
        // 构建文件
        if (isBuildFile(path)) {
            return ChangeCategory.BUILD;
        }
        
        // 默认为源码
        return ChangeCategory.SOURCE_CODE;
    }
    
    /**
     * 检测变更模式
     * 识别常见的开发模式
     */
    @NotNull
    private ChangePattern detectChangePattern(@NotNull List<FileChange> changes) {
        int totalFiles = changes.size();
        
        if (totalFiles == 1) {
            return ChangePattern.SINGLE_FILE;
        }
        
        // 检查是否为重构
        if (isRefactoring(changes)) {
            return ChangePattern.REFACTORING;
        }
        
        // 检查是否为功能开发
        if (isFeatureDevelopment(changes)) {
            return ChangePattern.FEATURE_DEVELOPMENT;
        }
        
        // 检查是否为bug修复
        if (isBugFix(changes)) {
            return ChangePattern.BUG_FIX;
        }
        
        return ChangePattern.MIXED;
    }
    
    /**
     * 提取关键洞察
     * 为AI提供最有价值的信息
     */
    @NotNull
    private List<String> extractKeyInsights(@NotNull List<FileChange> changes,
                                           @NotNull Map<ChangeCategory, List<FileChange>> categorized,
                                           @NotNull ChangePattern pattern) {
        List<String> insights = new ArrayList<>();
        
        // 模式洞察
        insights.add("变更模式: " + pattern.getDescription());
        
        // 文件分布洞察
        for (Map.Entry<ChangeCategory, List<FileChange>> entry : categorized.entrySet()) {
            List<FileChange> categoryChanges = entry.getValue();
            if (!categoryChanges.isEmpty()) {
                insights.add(String.format("%s: %d个文件", 
                    entry.getKey().getDescription(), categoryChanges.size()));
            }
        }
        
        // 复杂度洞察
        int totalLines = changes.stream()
            .mapToInt(change -> change.getLinesAdded() + change.getLinesDeleted())
            .sum();
        
        if (totalLines > 500) {
            insights.add("大规模变更: " + totalLines + "行代码");
        }
        
        return insights;
    }
    
    /**
     * 计算变更复杂度
     * 简单的启发式算法
     */
    private int calculateComplexity(@NotNull List<FileChange> changes) {
        int complexity = 0;
        
        for (FileChange change : changes) {
            // 基础复杂度：文件数量
            complexity += 1;
            
            // 代码行数复杂度
            int lines = change.getLinesAdded() + change.getLinesDeleted();
            complexity += lines / 10; // 每10行增加1点复杂度
            
            // 文件类型复杂度
            if (change.getChangeType() == FileChangeType.MOVED) {
                complexity += 2; // 重命名增加复杂度
            }
        }
        
        return complexity;
    }
    
    // 辅助方法：简单直接
    
    private boolean isBuildFile(@NotNull String path) {
        return path.contains("build.gradle") || 
               path.contains("pom.xml") || 
               path.contains("package.json") ||
               path.contains("Makefile");
    }
    
    private boolean isRefactoring(@NotNull List<FileChange> changes) {
        // 简单启发式：多个文件，主要是修改
        long modifiedCount = changes.stream()
            .filter(c -> c.getChangeType() == FileChangeType.MODIFIED)
            .count();
        return modifiedCount > changes.size() * 0.7;
    }
    
    private boolean isFeatureDevelopment(@NotNull List<FileChange> changes) {
        // 简单启发式：有新文件创建
        return changes.stream()
            .anyMatch(c -> c.getChangeType() == FileChangeType.ADDED);
    }
    
    private boolean isBugFix(@NotNull List<FileChange> changes) {
        // 简单启发式：少量文件，主要是修改
        return changes.size() <= 3 && 
               changes.stream().allMatch(c -> c.getChangeType() == FileChangeType.MODIFIED);
    }
    
    /**
     * 变更分类枚举
     */
    public enum ChangeCategory {
        SOURCE_CODE("源代码"),
        TEST("测试代码"),
        CONFIG("配置文件"),
        DOCUMENTATION("文档"),
        BUILD("构建文件");
        
        private final String description;
        
        ChangeCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 变更模式枚举
     */
    public enum ChangePattern {
        SINGLE_FILE("单文件变更"),
        FEATURE_DEVELOPMENT("功能开发"),
        BUG_FIX("错误修复"),
        REFACTORING("代码重构"),
        MIXED("混合变更");
        
        private final String description;
        
        ChangePattern(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 分析结果数据结构
     */
    public static class AnalysisResult {
        private final Map<ChangeCategory, List<FileChange>> categorizedChanges;
        private final ChangePattern pattern;
        private final List<String> keyInsights;
        private final int complexity;
        
        public AnalysisResult(@NotNull Map<ChangeCategory, List<FileChange>> categorizedChanges,
                            @NotNull ChangePattern pattern,
                            @NotNull List<String> keyInsights,
                            int complexity) {
            this.categorizedChanges = new EnumMap<>(categorizedChanges);
            this.pattern = pattern;
            this.keyInsights = new ArrayList<>(keyInsights);
            this.complexity = complexity;
        }
        
        @NotNull
        public Map<ChangeCategory, List<FileChange>> getCategorizedChanges() {
            return new EnumMap<>(categorizedChanges);
        }
        
        @NotNull
        public ChangePattern getPattern() {
            return pattern;
        }
        
        @NotNull
        public List<String> getKeyInsights() {
            return new ArrayList<>(keyInsights);
        }
        
        public int getComplexity() {
            return complexity;
        }
        
        @Override
        public String toString() {
            return String.format("AnalysisResult{pattern=%s, complexity=%d, insights=%d}",
                pattern, complexity, keyInsights.size());
        }
    }
}