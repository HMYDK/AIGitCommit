package com.hmydk.aigit.context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hmydk.aigit.analyzer.ContextAnalyzer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 */
public class AIPromptBuilder {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_FOCUSED_CHANGES = 6;
    private static final int MAX_DIFF_LINES = 60;
    private static final int MAX_DIFF_CHARACTERS = 1200;
    private static final String TRUNCATED_SUFFIX = "\n…truncated";

    private final ContextAnalyzer.AnalysisResult analysis;

    public enum InputMode {
        FULL("full"),
        COMPACT("compact"),
        SUMMARY("summary");

        private final String code;

        InputMode(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public AIPromptBuilder(ContextAnalyzer.AnalysisResult analysis) {
        this.analysis = analysis;
    }

    /**
     * 构建智能AI提示
     * 基于分析结果优化输出
     */
    @NotNull
    public String buildIntelligent(CommitContext context) {
        return buildIntelligent(context, InputMode.FULL, null);
    }

    @NotNull
    public String buildIntelligent(CommitContext context, InputMode mode) {
        return buildIntelligent(context, mode, null);
    }

    @NotNull
    public String buildIntelligent(CommitContext context, InputMode mode, @Nullable Integer maxSummaryFiles) {
        Map<String, Object> data = buildStructuredData(context, mode, maxSummaryFiles);
        String jsonData = gson.toJson(data);
        return buildIntelligentPrompt(jsonData, mode);
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

        data.put("project", buildProjectData(context.getProject()));
        data.put("statistics", buildStatisticsData(context.getStatistics()));
        data.put("changes", buildChangesData(context.getChanges()));

        if (!context.getMetadata().isEmpty()) {
            data.put("metadata", context.getMetadata());
        }

        String jsonData = gson.toJson(data);
        return buildFinalPrompt(jsonData);
    }

    @NotNull
    private Map<String, Object> buildStructuredData(CommitContext context, InputMode mode, @Nullable Integer maxSummaryFiles) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<FileChange> allChanges = context.getChanges();
        List<FileChange> rankedChanges = rankChanges(allChanges);
        List<FileChange> summaryChanges = selectSummaryChanges(allChanges, rankedChanges, mode, maxSummaryFiles);

        data.put("input_mode", mode.getCode());
        data.put("truncated", mode != InputMode.FULL);
        data.put("analysis", buildAnalysisData());
        data.put("project", buildProjectData(context.getProject()));
        data.put("statistics", buildStatisticsData(context.getStatistics()));
        data.put("categorized_changes", buildCategorizedChangesData(allChanges, summaryChanges, mode));

        if (mode == InputMode.COMPACT) {
            List<Map<String, Object>> focusedChanges = buildFocusedChangesData(rankedChanges);
            data.put("focused_changes", focusedChanges);
            data.put("omitted_full_diff_files", Math.max(0, allChanges.size() - focusedChanges.size()));
        } else {
            data.put("omitted_full_diff_files", mode == InputMode.SUMMARY ? allChanges.size() : 0);
        }

        if (mode == InputMode.SUMMARY && summaryChanges.size() < allChanges.size()) {
            data.put("omitted_files", allChanges.size() - summaryChanges.size());
        }

        if (!context.getMetadata().isEmpty()) {
            data.put("metadata", context.getMetadata());
        }

        return data;
    }

    @NotNull
    private List<FileChange> selectSummaryChanges(List<FileChange> allChanges,
                                                  List<FileChange> rankedChanges,
                                                  InputMode mode,
                                                  @Nullable Integer maxSummaryFiles) {
        if (mode != InputMode.SUMMARY || maxSummaryFiles == null || maxSummaryFiles <= 0) {
            return allChanges;
        }

        int limit = Math.min(maxSummaryFiles, rankedChanges.size());
        return new ArrayList<>(rankedChanges.subList(0, limit));
    }

    @NotNull
    private List<Map<String, Object>> buildFocusedChangesData(List<FileChange> rankedChanges) {
        int limit = Math.min(MAX_FOCUSED_CHANGES, rankedChanges.size());
        List<Map<String, Object>> focusedChanges = new ArrayList<>(limit);

        for (int i = 0; i < limit; i++) {
            FileChange change = rankedChanges.get(i);
            Map<String, Object> changeData = new LinkedHashMap<>();
            changeData.put("path", change.getPath());
            changeData.put("type", change.getType().name());
            changeData.put("summary", change.getSummary());
            changeData.put("lines_added", change.getLinesAdded());
            changeData.put("lines_deleted", change.getLinesDeleted());
            changeData.put("diff_excerpt", buildDiffExcerpt(change.getDiffContent()));
            focusedChanges.add(changeData);
        }

        return focusedChanges;
    }

    @NotNull
    private List<FileChange> rankChanges(List<FileChange> changes) {
        return changes.stream()
                .sorted(Comparator
                        .comparingInt((FileChange change) -> change.getLinesAdded() + change.getLinesDeleted())
                        .reversed()
                        .thenComparing(FileChange::getPath))
                .collect(Collectors.toList());
    }

    /**
     * 构建智能分析数据 - 新增
     */
    @NotNull
    private Map<String, Object> buildAnalysisData() {
        if (analysis == null) {
            return new HashMap<>();
        }

        Map<String, Object> analysisData = new LinkedHashMap<>();
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
    private Map<String, Object> buildCategorizedChangesData(List<FileChange> allChanges,
                                                            List<FileChange> selectedChanges,
                                                            InputMode mode) {
        Map<String, Object> categorizedData = new LinkedHashMap<>();
        Map<ContextAnalyzer.ChangeCategory, List<FileChange>> categorized = getCategorizedChanges(allChanges);
        Set<FileChange> selectedSet = Set.copyOf(selectedChanges);

        for (ContextAnalyzer.ChangeCategory category : ContextAnalyzer.ChangeCategory.values()) {
            List<FileChange> categorySource = categorized.getOrDefault(category, List.of());
            List<Map<String, Object>> categoryChanges = categorySource.stream()
                    .filter(selectedSet::contains)
                    .map(change -> buildSingleChangeData(change, mode))
                    .collect(Collectors.toList());

            if (!categoryChanges.isEmpty()) {
                categorizedData.put(category.name().toLowerCase(), categoryChanges);
            }
        }

        return categorizedData;
    }

    @NotNull
    private Map<ContextAnalyzer.ChangeCategory, List<FileChange>> getCategorizedChanges(List<FileChange> allChanges) {
        if (analysis != null) {
            return analysis.getCategorizedChanges();
        }

        Map<ContextAnalyzer.ChangeCategory, List<FileChange>> categorized = new EnumMap<>(ContextAnalyzer.ChangeCategory.class);
        for (ContextAnalyzer.ChangeCategory category : ContextAnalyzer.ChangeCategory.values()) {
            categorized.put(category, new ArrayList<>());
        }

        for (FileChange change : allChanges) {
            categorized.get(inferCategory(change)).add(change);
        }

        return categorized;
    }

    @NotNull
    private ContextAnalyzer.ChangeCategory inferCategory(FileChange change) {
        String path = change.getPath();
        String extension = change.getExtension();

        if (path.matches(".*[Tt]est.*|.*[Ss]pec.*|.*\\.test\\.|.*\\.spec\\.")) {
            return ContextAnalyzer.ChangeCategory.TEST;
        }
        if (List.of("json", "xml", "yml", "yaml", "properties", "conf", "config", "ini").contains(extension)) {
            return ContextAnalyzer.ChangeCategory.CONFIG;
        }
        if (List.of("md", "txt", "rst", "adoc", "doc", "docx").contains(extension)) {
            return ContextAnalyzer.ChangeCategory.DOCUMENTATION;
        }
        if (path.contains("build.gradle") || path.contains("pom.xml") || path.contains("package.json") || path.contains("Makefile")) {
            return ContextAnalyzer.ChangeCategory.BUILD;
        }
        return ContextAnalyzer.ChangeCategory.SOURCE_CODE;
    }

    private static Map<String, Object> buildProjectData(ProjectInfo project) {
        Map<String, Object> projectData = new LinkedHashMap<>();
        projectData.put("name", project.getName());
        projectData.put("branch", project.getBranch());
        projectData.put("is_git_repository", project.isGitRepository());
        return projectData;
    }

    private static Map<String, Object> buildStatisticsData(ChangeStatistics stats) {
        Map<String, Object> statsData = new LinkedHashMap<>();
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
        return changes.stream()
                .map(change -> buildSingleChangeDataStatic(change, true, true))
                .collect(Collectors.toList());
    }

    private static Map<String, Object> buildSingleChangeDataStatic(FileChange change,
                                                                   boolean includeDiffSummary,
                                                                   boolean includeFullDiffContent) {
        Map<String, Object> changeData = new LinkedHashMap<>();
        changeData.put("path", change.getPath());
        changeData.put("type", change.getType().name());
        changeData.put("language", change.getLanguage());
        changeData.put("extension", change.getExtension());
        changeData.put("lines_added", change.getLinesAdded());
        changeData.put("lines_deleted", change.getLinesDeleted());
        changeData.put("summary", change.getSummary());

        if (change.getDiffContent() != null && !change.getDiffContent().isEmpty()) {
            if (includeDiffSummary) {
                changeData.put("diff_summary", extractDiffSummary(change.getDiffContent()));
            }
            if (includeFullDiffContent) {
                changeData.put("full_diff_content", change.getDiffContent());
            }
        }

        return changeData;
    }

    @NotNull
    private Map<String, Object> buildSingleChangeData(FileChange change, InputMode mode) {
        return switch (mode) {
            case FULL -> buildSingleChangeDataStatic(change, true, true);
            case COMPACT -> buildSingleChangeDataStatic(change, true, false);
            case SUMMARY -> buildSingleChangeDataStatic(change, false, false);
        };
    }

    private static String extractDiffSummary(String diffContent) {
        String[] lines = diffContent.split("\n");
        StringBuilder summary = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("[ADD]:") || line.startsWith("[MODIFY]:") ||
                    line.startsWith("[DELETE]:") || line.startsWith("[MOVE]:")) {
                if (!summary.isEmpty()) {
                    summary.append("\n");
                }
                summary.append(line);
            }
        }

        return summary.toString().trim();
    }

    @NotNull
    private String buildDiffExcerpt(@Nullable String diffContent) {
        if (diffContent == null || diffContent.isEmpty()) {
            return "";
        }

        StringBuilder excerpt = new StringBuilder();
        String[] lines = diffContent.split("\n");
        int diffLineCount = 0;
        boolean truncated = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean isFirstLine = i == 0 && (line.startsWith("[ADD]:") || line.startsWith("[MODIFY]:")
                    || line.startsWith("[DELETE]:") || line.startsWith("[MOVE]:"));
            boolean isHeader = line.startsWith("--- ") || line.startsWith("+++ ");
            boolean isDiffLine = isActualDiffLine(line);

            if (!isFirstLine && !isHeader && !isDiffLine) {
                continue;
            }

            if (isDiffLine) {
                if (diffLineCount >= MAX_DIFF_LINES) {
                    truncated = true;
                    continue;
                }
                diffLineCount++;
            }

            if (!appendLineWithinLimit(excerpt, line)) {
                truncated = true;
                break;
            }
        }

        if (excerpt.isEmpty()) {
            int maxLength = Math.max(0, MAX_DIFF_CHARACTERS - TRUNCATED_SUFFIX.length());
            excerpt.append(diffContent, 0, Math.min(diffContent.length(), maxLength));
            truncated = diffContent.length() > maxLength;
        }

        if (truncated) {
            appendTruncatedSuffix(excerpt);
        }

        return excerpt.toString();
    }

    private boolean appendLineWithinLimit(StringBuilder excerpt, String line) {
        String addition = excerpt.isEmpty() ? line : "\n" + line;
        if (excerpt.length() + addition.length() > MAX_DIFF_CHARACTERS) {
            return false;
        }
        excerpt.append(addition);
        return true;
    }

    private void appendTruncatedSuffix(StringBuilder excerpt) {
        int maxContentLength = Math.max(0, MAX_DIFF_CHARACTERS - TRUNCATED_SUFFIX.length());
        if (excerpt.length() > maxContentLength) {
            excerpt.setLength(maxContentLength);
        }
        excerpt.append(TRUNCATED_SUFFIX);
    }

    private boolean isActualDiffLine(String line) {
        return (line.startsWith("+") || line.startsWith("-"))
                && !line.startsWith("+++ ")
                && !line.startsWith("--- ");
    }

    @NotNull
    private String buildIntelligentPrompt(String jsonData, InputMode mode) {
        return switch (mode) {
            case FULL -> String.format(
                    "Analyze this intelligently structured commit data and generate a conventional commit message:\n\n" +
                            "%s\n\n" +
                            "Enhanced Requirements:\n" +
                            "1. Use conventional commit format: type(scope): description\n" +
                            "2. Leverage analysis.pattern and complexity_level for better type selection\n" +
                            "3. Use categorized_changes to understand the change structure\n" +
                            "4. Consider key_insights for important context\n" +
                            "5. Use full_diff_content to see actual code changes and understand developer intent\n" +
                            "6. Write clear, concise description focusing on what changed\n" +
                            "7. Keep description under 50 characters if possible\n" +
                            "8. Use present tense (\"add\" not \"added\")",
                    jsonData
            );
            case COMPACT -> String.format(
                    "Analyze this compact commit data and generate a conventional commit message:\n\n" +
                            "%s\n\n" +
                            "Enhanced Requirements:\n" +
                            "1. Use conventional commit format: type(scope): description\n" +
                            "2. Prioritize statistics, categorized_changes, and analysis to decide type and scope\n" +
                            "3. Use focused_changes as the main code detail reference\n" +
                            "4. The input is intentionally truncated, so do not expect a full diff\n" +
                            "5. Write clear, concise description focusing on what changed\n" +
                            "6. Keep description under 50 characters if possible\n" +
                            "7. Use present tense (\"add\" not \"added\")",
                    jsonData
            );
            case SUMMARY -> String.format(
                    "Analyze this summarized commit data and generate a conventional commit message:\n\n" +
                            "%s\n\n" +
                            "Enhanced Requirements:\n" +
                            "1. Use conventional commit format: type(scope): description\n" +
                            "2. Prioritize statistics, categorized_changes, and analysis to decide type and scope\n" +
                            "3. The input is intentionally truncated, so do not expect raw diff details\n" +
                            "4. Write clear, concise description focusing on what changed\n" +
                            "5. Keep description under 50 characters if possible\n" +
                            "6. Use present tense (\"add\" not \"added\")",
                    jsonData
            );
        };
    }

    private static String buildFinalPrompt(String jsonData) {
        return String.format(
                "Analyze this structured commit data and generate a conventional commit message:\n\n" +
                        "%s\n\n" +
                        "Requirements:\n" +
                        "1. Use conventional commit format: type(scope): description\n" +
                        "2. Choose appropriate type based on change_type and file analysis\n" +
                        "3. Use scope from statistics or infer from file paths\n" +
                        "4. Use full_diff_content to see actual code changes and understand developer intent\n" +
                        "5. Write clear, concise description focusing on what changed\n" +
                        "6. Keep description under 50 characters if possible\n" +
                        "7. Use present tense (\"add\" not \"added\")",
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

        if (analysis != null) {
            prompt.append(String.format("Analysis: %s (complexity: %s)\n\n",
                    analysis.getPattern().getDescription(), getComplexityLevel()));
        }

        prompt.append(String.format("Statistics: %d files, +%d/-%d lines, type: %s, scope: %s\n\n",
                stats.getFilesChanged(), stats.getLinesAdded(), stats.getLinesDeleted(),
                stats.getPrimaryType().getCode(), stats.getScope()));

        prompt.append("Changes:\n");
        for (FileChange change : changes) {
            prompt.append(String.format("- %s\n", change.getSummary()));
        }

        prompt.append("\nFormat: type(scope): description");

        return prompt.toString();
    }

    @NotNull
    private String getComplexityLevel() {
        if (analysis == null) {
            return "未知";
        }

        int complexity = analysis.getComplexity();
        if (complexity <= 5) return "简单";
        if (complexity <= 15) return "中等";
        if (complexity <= 30) return "复杂";
        return "非常复杂";
    }
}
