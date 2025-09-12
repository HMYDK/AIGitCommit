package com.hmydk.aigit.context;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

/**
 * 项目信息数据结构
 * Linus式设计：简单、直接、无特殊情况
 */
public class ProjectInfo {
    private final String name;
    private final String path;
    private final String branch;
    private final boolean isGitRepository;
    
    public ProjectInfo(String name, String path, String branch, boolean isGitRepository) {
        this.name = name;
        this.path = path;
        this.branch = branch;
        this.isGitRepository = isGitRepository;
    }
    
    public static ProjectInfo from(Project project) {
        String name = project.getName();
        String path = project.getBasePath();
        String branch = "unknown";
        boolean isGit = false;
        
        try {
            // 获取Git信息
            GitRepositoryManager gitManager = GitRepositoryManager.getInstance(project);
            if (gitManager != null) {
                GitRepository repository = gitManager.getRepositoryForRoot(ProjectUtil.guessProjectDir(project));
                if (repository != null) {
                    isGit = true;
                    if (repository.getCurrentBranch() != null) {
                        branch = repository.getCurrentBranch().getName();
                    }
                }
            }
        } catch (Exception e) {
            // 静默处理，使用默认值
        }
        
        return new ProjectInfo(name, path, branch, isGit);
    }
    
    // Getters
    public String getName() { return name; }
    public String getPath() { return path; }
    public String getBranch() { return branch; }
    public boolean isGitRepository() { return isGitRepository; }
    
    @Override
    public String toString() {
        return String.format("ProjectInfo{name='%s', branch='%s', git=%s}", 
                           name, branch, isGitRepository);
    }
}