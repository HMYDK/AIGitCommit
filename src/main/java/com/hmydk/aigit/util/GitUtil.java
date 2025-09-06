package com.hmydk.aigit.util;

import com.hmydk.aigit.context.CommitContext;
import com.hmydk.aigit.context.FileChange;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GItCommitUtil
 *
 * @author hmydk
 */
public class GitUtil {
    private static final Logger log = LoggerFactory.getLogger(GitUtil.class);

    /**
     * 计算差异并收集丰富的上下文信息，用于生成更准确的commit message
     * 
     * @param includedChanges 包含的变更
     * @param unversionedFiles 未版本控制的文件
     * @param project 项目
     * @return 包含丰富上下文的差异信息
     */
    public static Map<String, Object> computeEnhancedDiff(@NotNull List<Change> includedChanges,
                                     @NotNull List<FilePath> unversionedFiles,
                                     @NotNull Project project) {
        Map<String, Object> result = new HashMap<>();
        
        // 基本差异信息
        String rawDiff = computeDiff(includedChanges, unversionedFiles, project);
        result.put("rawDiff", rawDiff);
        
        // 收集变更文件的相关信息
        List<Map<String, Object>> fileContexts = new ArrayList<>();
        
        // 处理已版本控制的变更
        for (Change change : includedChanges) {
            Map<String, Object> fileContext = new HashMap<>();
            
            VirtualFile vFile;
            String filePath = null;
            
            if (change.getVirtualFile() != null) {
                vFile = change.getVirtualFile();
                filePath = vFile.getPath();
            } else {
                vFile = null;
                if (change.getBeforeRevision() != null) {
                    filePath = change.getBeforeRevision().getFile().getPath();
                }
            }
            
            if (filePath == null) continue;
            
            fileContext.put("filePath", filePath);
            fileContext.put("changeType", change.getType().toString());
            
            // 收集文件类型信息
            if (vFile != null) {
                // 检查是否为二进制文件
                boolean isBinary = vFile.getFileType().isBinary();
                fileContext.put("isBinary", isBinary);
                
                // 在read action中执行PSI操作
                ReadAction.run(() -> {
                    fileContext.put("fileType", vFile.getFileType().getName());
                    fileContext.put("fileExtension", vFile.getExtension());
                    
                    // 只对非二进制文件提取PSI信息
                    if (!isBinary) {
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
                        if (psiFile != null) {
                            fileContext.put("language", psiFile.getLanguage().getDisplayName());
                            fileContext.put("fileName", psiFile.getName());
                        }
                    }
                });
            }
            
            fileContexts.add(fileContext);
        }
        
        // 处理未版本控制的文件
        for (FilePath unversionedFile : unversionedFiles) {
            Map<String, Object> fileContext = new HashMap<>();
            fileContext.put("filePath", unversionedFile.getPath());
            fileContext.put("changeType", "NEW");
            
            // 在read action中执行PSI操作
            ReadAction.run(() -> {
                if (unversionedFile.getVirtualFile() != null) {
                    VirtualFile vFile = unversionedFile.getVirtualFile();
                    fileContext.put("fileType", vFile.getFileType().getName());
                    fileContext.put("fileExtension", vFile.getExtension());
                }
            });
            
            fileContexts.add(fileContext);
        }
        
        result.put("fileContexts", fileContexts);
        
        // 添加项目级别的上下文
        Map<String, Object> projectContext = new HashMap<>();
        GitRepositoryManager gitRepositoryManager = GitRepositoryManager.getInstance(project);
        List<GitRepository> repositories = gitRepositoryManager.getRepositories();
        if (!repositories.isEmpty()) {
            GitRepository mainRepo = repositories.get(0);
            projectContext.put("currentBranch", mainRepo.getCurrentBranch() != null ? 
                    mainRepo.getCurrentBranch().getName() : "unknown");
        }
        projectContext.put("projectName", project.getName());
        
        result.put("projectContext", projectContext);
        
        return result;
    }

    public static String computeDiff(@NotNull List<Change> includedChanges,
                                     @NotNull List<FilePath> unversionedFiles,
                                     @NotNull Project project) {
        StringBuilder diffBuilder = new StringBuilder();

        // 处理已版本控制的变更
        String existingDiff = computeDiff(includedChanges, project);
        diffBuilder.append(existingDiff);

        // 处理未版本控制的文件
        for (FilePath unversionedFile : unversionedFiles) {
            VirtualFile vFile = unversionedFile.getVirtualFile();
            diffBuilder.append("[ADD]: ")
                    .append(unversionedFile.getPath())
                    .append("\n");

            // 检查是否为二进制文件
            if (vFile != null && vFile.getFileType().isBinary()) {
                diffBuilder.append("二进制文件，内容已省略\n\n");
                continue;
            }

            try {
                // 读取新文件的内容
                String content = new String(Files.readAllBytes(Paths.get(unversionedFile.getPath())));
                diffBuilder.append("+++ ")
                        .append(unversionedFile.getPath())
                        .append("\n");
                // 将整个文件内容作为新增的内容
                for (String line : content.split("\n")) {
                    diffBuilder.append("+ ").append(line).append("\n");
                }
                diffBuilder.append("\n");
            } catch (IOException e) {
                log.error("Error reading unversioned file: {}", unversionedFile.getPath(), e);
            }
        }

        return diffBuilder.toString();
    }

    public static String computeDiff(@NotNull List<Change> includedChanges, @NotNull Project project) {
        GitRepositoryManager gitRepositoryManager = GitRepositoryManager.getInstance(project);
        StringBuilder diffBuilder = new StringBuilder();

        // 按仓库分组处理变更
        Map<GitRepository, List<Change>> changesByRepository = includedChanges.stream()
                .map(change -> {
                    GitRepository repository = null;
                    if (change.getVirtualFile() != null) {
                        // 对于新增、修改、移动的文件，使用当前文件
                        repository = gitRepositoryManager.getRepositoryForFileQuick(change.getVirtualFile());
                    } else if (change.getBeforeRevision() != null && change.getBeforeRevision().getFile().getPath() != null) {
                        // 对于删除的文件，使用删除前的文件路径
                        repository = gitRepositoryManager.getRepositoryForFile(change.getBeforeRevision().getFile());
                    }

                    if (repository != null) {
                        return new AbstractMap.SimpleEntry<>(repository, change);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        // 处理每个仓库的变更
        for (Map.Entry<GitRepository, List<Change>> entry : changesByRepository.entrySet()) {
            GitRepository repository = entry.getKey();
            List<Change> changes = entry.getValue();

            if (repository != null) {
                try {
                    // 构建文件补丁
                    List<FilePatch> filePatches = IdeaTextPatchBuilder.buildPatch(
                            project,
                            changes,
                            repository.getRoot().toNioPath(),
                            false,
                            true);

                    // 添加仓库信息
//                    diffBuilder.append("Repository: ").append(repository.getRoot().getName()).append("\n\n");

                    // 处理每个文件的变更
                    for (FilePatch patch : filePatches) {
                        String filePath = patch.getBeforeName();
                        String changeType = getChangeType(changes, filePath);

                        diffBuilder.append(changeType)
                                .append(": ")
                                .append(filePath)
                                .append("\n");

                        // 使用StringWriter获取差异内容
                        StringWriter stringWriter = new StringWriter();
                        UnifiedDiffWriter.write(project, List.of(patch), stringWriter, "\n", null);
                        String diffContent = stringWriter.toString();

                        diffBuilder.append(diffContent).append("\n");
                    }
                } catch (Exception e) {
                    log.error("Error computing diff", e);
                }
            }
        }

        return diffBuilder.toString();
    }

    private static String getChangeType(List<Change> changes, String filePath) {
        for (Change change : changes) {
            return switch (change.getType()) {
                case NEW -> "[ADD]";
                case DELETED -> "[DELETE]";
                case MOVED -> "[MOVE]";
                case MODIFICATION -> "[MODIFY]";
            };
        }
        return "[UNKNOWN]";
    }

    /**
     * 将增强的差异信息转换为格式化的字符串
     * 
     * @param includedChanges 包含的变更
     * @param unversionedFiles 未版本控制的文件
     * @param project 项目
     * @return 格式化的差异信息字符串
     */
    /**
     * Linus式重构：新的优雅方法
     * 一个函数，一个数据结构，搞定所有事情
     * "Bad programmers worry about the code. Good programmers worry about data structures."
     */
    public static CommitContext buildCommitContext(@NotNull List<Change> includedChanges,
                                                  @NotNull List<FilePath> unversionedFiles,
                                                  @NotNull Project project) {
        // 统一处理所有文件变更，消除特殊情况
        List<FileChange> changes = FileChange.fromGitChanges(includedChanges, unversionedFiles);
        
        // 创建CommitContext - 一个数据结构包含所有信息
        return CommitContext.create(project, changes);
    }
    
    /**
     * 新的AI输入生成方法
     * 生成结构化的AI输入，而不是混乱的文本格式
     */
    public static String getOptimizedAIInput(@NotNull List<Change> includedChanges,
                                           @NotNull List<FilePath> unversionedFiles,
                                           @NotNull Project project) {
        CommitContext context = buildCommitContext(includedChanges, unversionedFiles, project);
        return context.toAIPrompt();
    }
    
    /**
     * 向后兼容：保持旧接口不变
     * 内部使用新的数据结构，但对外接口保持兼容
     */
    @Deprecated
    public static String getFormattedDiff(@NotNull List<Change> includedChanges,
                                        @NotNull List<FilePath> unversionedFiles,
                                        @NotNull Project project) {
        // 使用新的数据结构，但生成旧格式输出
        CommitContext context = buildCommitContext(includedChanges, unversionedFiles, project);
        return context.toLegacyFormat();
    }
    
    /**
     * 检查当前项目是否为Git仓库
     * 
     * @param project 项目
     * @return 如果是Git仓库则返回true，否则返回false
     */
    public static boolean isGitRepository(@NotNull Project project) {
        if (project == null) {
            return false;
        }
        
        GitRepositoryManager gitRepositoryManager = GitRepositoryManager.getInstance(project);
        if (gitRepositoryManager == null) {
            return false;
        }
        
        // 检查是否有Git仓库
        List<GitRepository> repositories = gitRepositoryManager.getRepositories();
        return !repositories.isEmpty();
    }
}
