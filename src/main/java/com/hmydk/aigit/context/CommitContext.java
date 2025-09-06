package com.hmydk.aigit.context;

import com.hmydk.aigit.analyzer.ContextAnalyzer;
import com.intellij.openapi.project.Project;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Linus式设计：一个数据结构包含所有commit相关信息
 * 消除特殊情况，统一处理逻辑
 * 
 * "Bad programmers worry about the code. Good programmers worry about data structures."
 */
/**
 * Linus式重构：CommitContext应该是一个完整的上下文
 * 包含Project引用以支持智能分析
 */
public class CommitContext {
    private final ProjectInfo project;
    private final Project ideaProject;  // 添加Project引用用于分析
    private final List<FileChange> changes;
    private final ChangeStatistics statistics;
    private final Map<String, Object> metadata;
    
    public CommitContext(ProjectInfo project, Project ideaProject, List<FileChange> changes, 
                        ChangeStatistics statistics, Map<String, Object> metadata) {
        this.project = project;
        this.ideaProject = ideaProject;
        this.changes = changes;
        this.statistics = statistics;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    
    // 简洁的构造器
    public static CommitContext create(Project ideaProject, List<FileChange> changes) {
        ProjectInfo projectInfo = ProjectInfo.from(ideaProject);
        ChangeStatistics stats = ChangeStatistics.from(changes);
        return new CommitContext(projectInfo, ideaProject, changes, stats, null);
    }
    
    /**
     * 生成智能AI输入
     * 集成分析结果的结构化数据
     */
    /**
     * 生成智能AI提示
     * 集成分析结果的结构化数据
     */
    public String toAIPrompt() {
        // 使用智能分析器
        ContextAnalyzer analyzer = new ContextAnalyzer(ideaProject);
        ContextAnalyzer.AnalysisResult analysis = analyzer.analyze(this);
        
        // 使用智能提示构建器
        AIPromptBuilder builder = new AIPromptBuilder(analysis);
        return builder.buildIntelligent(this);
    }
    
    /**
     * 生成简单AI输入（向后兼容）
     */
    public String toSimplePrompt() {
        return AIPromptBuilder.build(this);
    }
    
    /**
     * 向后兼容：生成旧格式输出
     * 保持现有接口不变
     */
    public String toLegacyFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("项目信息:\n");
        sb.append("- 项目名称: ").append(project.getName()).append("\n");
        sb.append("- 当前分支: ").append(project.getBranch()).append("\n\n");
        
        sb.append("文件变更:\n");
        for (FileChange change : changes) {
            sb.append(change.toLegacyFormat());
        }
        
        sb.append("\n详细变更:\n");
        for (FileChange change : changes) {
            sb.append(change.getDiffContent());
        }
        
        return sb.toString();
    }
    
    // Getters
    public ProjectInfo getProject() { return project; }
    public Project getIdeaProject() { return ideaProject; }
    public List<FileChange> getChanges() { return changes; }
    public ChangeStatistics getStatistics() { return statistics; }
    public Map<String, Object> getMetadata() { return metadata; }
    
    // 便利方法
    public int getFileCount() { return changes.size(); }
    public boolean hasChanges() { return !changes.isEmpty(); }
    public boolean isLargeChange() { return statistics.getTotalLines() > 100; }
    
    @Override
    public String toString() {
        return String.format("CommitContext{project=%s, files=%d, lines=%d}", 
                           project.getName(), getFileCount(), statistics.getTotalLines());
    }
}