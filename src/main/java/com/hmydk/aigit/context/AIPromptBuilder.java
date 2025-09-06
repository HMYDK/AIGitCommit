package com.hmydk.aigit.context;

import com.hmydk.aigit.analyzer.ContextAnalyzer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Linus式AI提示构建器 - 升级版
 * "The key insight is that you have to design your data structures right."
 * 
 * 这个类的职责：
 * 1. 将CommitContext转换为结构化的AI输入
 * 2. 集成智能分析结果
 * 3. 消除文本解析的噪音
 * 4. 让AI专注于代码变更的语义理解
 * 
 * 设计原则：
 * - 结构化数据，不是文本拼接
 * - 智能分析驱动输出
 * - 消除特殊情况
 * - 一个方法做一件事
 */
public class AIPromptBuilder {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private final ContextAnalyzer.AnalysisResult analysis;
    
    /**
     * 构建智能AI提示
     * 基于分析结果优化输出
     */
    @NotNull
    public String buildIntelligent(CommitContext context) {
        Map<String, Object> data = new HashMap<>();
        
        // 智能分析结果
        data.put("analysis", buildAnalysisData());
        
        // 项目信息
        data.put("project", buildProjectData(context.getProject()));
        
        // 统计信息
        data.put("statistics", buildStatisticsData(context.getStatistics()));
        
        // 分类变更信息
        data.put("categorized_changes", buildCategorizedChangesData(context.getChanges()));
        
        // 元数据
        if (!context.getMetadata().isEmpty()) {
            data.put("metadata", context.getMetadata());
        }
        
        // 生成结构化JSON
        String jsonData = gson.toJson(data);
        
        // 构建最终提示词
        return buildIntelligentPrompt(jsonData);
    }
    
    /**
     * 构建结构化的AI输入（向后兼容）
     * 替代混乱的文本格式
     */
    public static String build(CommitContext context) {
        return new AIPromptBuilder(null).buildLegacy(context);
    }
    
    private String buildLegacy(CommitContext context) {
        Map<String, Object> data = new HashMap<>();
        
        // 项目信息
        data.put("project", buildProjectData(context.getProject()));
        
        // 统计信息
        data.put("statistics", buildStatisticsData(context.getStatistics()));
        
        // 变更信息
        data.put("changes", buildChangesData(context.getChanges()));
        
        // 元数据
        if (!context.getMetadata().isEmpty()) {
            data.put("metadata", context.getMetadata());
        }
        
        // 生成结构化JSON
        String jsonData = gson.toJson(data);
        
        // 构建最终提示词
        return buildFinalPrompt(jsonData);
    }
    
    public AIPromptBuilder(ContextAnalyzer.AnalysisResult analysis) {
        this.analysis = analysis;
    }
    
    /**
     * 构建智能分析数据 - 新增
     */
    @NotNull
    private Map<String, Object> buildAnalysisData() {
        if (analysis == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> analysisData = new HashMap<>();
        analysisData.put("pattern", analysis.getPattern().name());
        analysisData.put("pattern_description", analysis.getPattern().getDescription());
        analysisData.put("complexity", analysis.getComplexity());
        analysisData.put("complexity_level", getComplexityLevel());
        analysisData.put("key_insights", analysis.getKeyInsights());
        return analysisData;
    }
    
    /**
     * 构建分类变更数据 - 新增
     */
    @NotNull
    private Map<String, Object> buildCategorizedChangesData(List<FileChange> changes) {
        if (analysis == null) {
            return buildChangesData(changes).stream()
                .collect(Collectors.toMap(
                    change -> ((Map<String, Object>) change).get("path").toString(),
                    change -> change
                ));
        }
        
        Map<String, Object> categorizedData = new HashMap<>();
        Map<ContextAnalyzer.ChangeCategory, List<FileChange>> categorized = analysis.getCategorizedChanges();
        
        for (Map.Entry<ContextAnalyzer.ChangeCategory, List<FileChange>> entry : categorized.entrySet()) {
            String categoryName = entry.getKey().name().toLowerCase();
            List<Map<String, Object>> categoryChanges = entry.getValue().stream()
                .map(this::buildSingleChangeData)
                .collect(Collectors.toList());
            categorizedData.put(categoryName, categoryChanges);
        }
        
        return categorizedData;
    }
    
    private static Map<String, Object> buildProjectData(ProjectInfo project) {
        Map<String, Object> projectData = new HashMap<>();
        projectData.put("name", project.getName());
        projectData.put("branch", project.getBranch());
        projectData.put("is_git_repository", project.isGitRepository());
        return projectData;
    }
    
    private static Map<String, Object> buildStatisticsData(ChangeStatistics stats) {
        Map<String, Object> statsData = new HashMap<>();
        statsData.put("files_changed", stats.getFilesChanged());
        statsData.put("lines_added", stats.getLinesAdded());
        statsData.put("lines_deleted", stats.getLinesDeleted());
        statsData.put("total_lines", stats.getTotalLines());
        statsData.put("change_type", stats.getPrimaryType().getCode());
        statsData.put("scope", stats.getScope());
        statsData.put("complexity", stats.getComplexity());
        statsData.put("language_distribution", stats.getLanguageDistribution());
        return statsData;
    }
    
    private static List<Map<String, Object>> buildChangesData(List<FileChange> changes) {
        return changes.stream().map(AIPromptBuilder::buildSingleChangeDataStatic).collect(Collectors.toList());
    }
    
    private static Map<String, Object> buildSingleChangeDataStatic(FileChange change) {
        Map<String, Object> changeData = new HashMap<>();
        changeData.put("path", change.getPath());
        changeData.put("type", change.getType().name());
        changeData.put("language", change.getLanguage());
        changeData.put("extension", change.getExtension());
        changeData.put("lines_added", change.getLinesAdded());
        changeData.put("lines_deleted", change.getLinesDeleted());
        changeData.put("summary", change.getSummary());
        
        // Linus修复：AI需要看到完整的代码变动，不只是摘要！
        // "Never break userspace" - AI就是我们的用户空间
        if (change.getDiffContent() != null && !change.getDiffContent().isEmpty()) {
            changeData.put("diff_summary", extractDiffSummary(change.getDiffContent()));
            changeData.put("full_diff_content", change.getDiffContent()); // 关键修复：提供完整diff
        }
        
        return changeData;
    }
    
    @NotNull
    private Map<String, Object> buildSingleChangeData(FileChange change) {
        return buildSingleChangeDataStatic(change);
    }
    
    private static String extractDiffSummary(String diffContent) {
        // 提取diff的关键信息，去除格式化噪音
        String[] lines = diffContent.split("\n");
        StringBuilder summary = new StringBuilder();
        
        for (String line : lines) {
            if (line.startsWith("[ADD]:") || line.startsWith("[MODIFY]:") || 
                line.startsWith("[DELETE]:") || line.startsWith("[MOVE]:")) {
                summary.append(line).append("\n");
            }
        }
        
        return summary.toString().trim();
    }
    
    @NotNull
    private String buildIntelligentPrompt(String jsonData) {
        return String.format(
            "Analyze this intelligently structured commit data and generate a conventional commit message:\n\n" +
            "%s\n\n" +
            "Enhanced Requirements:\n" +
            "1. Use conventional commit format: type(scope): description\n" +
            "2. Leverage the analysis.pattern and complexity_level for better type selection\n" +
            "3. Use categorized_changes to understand the change structure\n" +
            "4. Consider key_insights for important context\n" +
            "5. IMPORTANT: Use full_diff_content to see actual code changes and understand developer intent\n" +
            "6. Write clear, concise description focusing on WHAT changed\n" +
            "7. Keep description under 50 characters if possible\n" +
            "8. Use present tense (\"add\" not \"added\")\n\n" +
            "The analysis section provides intelligent insights and full_diff_content shows actual code changes - use both to generate more accurate commit messages.",
            jsonData
        );
    }
    
    private static String buildFinalPrompt(String jsonData) {
        return String.format(
            "Analyze this structured commit data and generate a conventional commit message:\n\n" +
            "%s\n\n" +
            "Requirements:\n" +
            "1. Use conventional commit format: type(scope): description\n" +
            "2. Choose appropriate type based on change_type and file analysis\n" +
            "3. Use scope from statistics or infer from file paths\n" +
            "4. IMPORTANT: Use full_diff_content to see actual code changes and understand developer intent\n" +
            "5. Write clear, concise description focusing on WHAT changed\n" +
            "6. Keep description under 50 characters if possible\n" +
            "7. Use present tense (\"add\" not \"added\")\n\n" +
            "Focus on the change statistics, file summaries, and full_diff_content to determine the appropriate type and scope.",
            jsonData
        );
    }
    
    /**
     * 构建简化版本的提示词（用于快速测试）
     */
    public static String buildSimple(CommitContext context) {
        return new AIPromptBuilder(null).buildSimpleInternal(context);
    }
    
    @NotNull
    private String buildSimpleInternal(CommitContext context) {
        ChangeStatistics stats = context.getStatistics();
        List<FileChange> changes = context.getChanges();
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a conventional commit message for these changes:\n\n");
        
        // 智能分析信息（如果可用）
        if (analysis != null) {
            prompt.append(String.format("Analysis: %s (complexity: %s)\n\n",
                analysis.getPattern().getDescription(), getComplexityLevel()));
        }
        
        // 统计信息
        prompt.append(String.format("Statistics: %d files, +%d/-%d lines, type: %s, scope: %s\n\n",
                                  stats.getFilesChanged(), stats.getLinesAdded(), stats.getLinesDeleted(),
                                  stats.getPrimaryType().getCode(), stats.getScope()));
        
        // 文件变更摘要
        prompt.append("Changes:\n");
        for (FileChange change : changes) {
            prompt.append(String.format("- %s\n", change.getSummary()));
        }
        
        prompt.append("\nFormat: type(scope): description");
        
        return prompt.toString();
    }
    
    // 辅助方法
    
    @NotNull
    private String getComplexityLevel() {
        if (analysis == null) return "未知";
        
        int complexity = analysis.getComplexity();
        if (complexity <= 5) return "简单";
        if (complexity <= 15) return "中等";
        if (complexity <= 30) return "复杂";
        return "非常复杂";
    }
}