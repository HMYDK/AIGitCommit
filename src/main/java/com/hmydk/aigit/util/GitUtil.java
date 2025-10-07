package com.hmydk.aigit.util;

import com.hmydk.aigit.context.CommitContext;
import com.hmydk.aigit.context.FileChange;
import com.hmydk.aigit.config.ApiKeySettings;
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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
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
    
    // 缓存PathMatcher以提高性能
    private static final Map<String, PathMatcher> pathMatcherCache = new HashMap<>();

    /**
     * 检查文件是否应该被排除
     * 
     * @param filePath 文件路径
     * @return 如果文件应该被排除则返回true
     */
    public static boolean shouldExcludeFile(String filePath) {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        
        // 如果文件排除功能未启用，则不排除任何文件
        if (!settings.isEnableFileExclusion()) {
            return false;
        }
        
        List<String> excludePatterns = settings.getExcludePatterns();
        if (excludePatterns == null || excludePatterns.isEmpty()) {
            return false;
        }
        
        // 标准化文件路径（使用正斜杠）
        String normalizedPath = filePath.replace('\\', '/');
        Path path = Paths.get(normalizedPath);
        String fileName = path.getFileName() != null ? path.getFileName().toString() : "";
        
        for (String pattern : excludePatterns) {
            if (pattern == null || pattern.trim().isEmpty()) {
                continue;
            }
            
            String trimmedPattern = pattern.trim();
            
            try {
                // 检查是否匹配文件名模式
                if (matchesFileName(fileName, trimmedPattern)) {
                    log.debug("File excluded by filename pattern '{}': {}", trimmedPattern, filePath);
                    return true;
                }
                
                // 检查是否匹配路径模式
                if (matchesPathPattern(normalizedPath, trimmedPattern)) {
                    log.debug("File excluded by path pattern '{}': {}", trimmedPattern, filePath);
                    return true;
                }
                
                // 检查是否匹配glob模式
                if (matchesGlobPattern(normalizedPath, trimmedPattern)) {
                    log.debug("File excluded by glob pattern '{}': {}", trimmedPattern, filePath);
                    return true;
                }
            } catch (Exception e) {
                log.warn("Error matching pattern '{}' against file '{}': {}", trimmedPattern, filePath, e.getMessage());
            }
        }
        
        return false;
    }
    
    /**
     * 检查文件名是否匹配模式
     */
    private static boolean matchesFileName(String fileName, String pattern) {
        // 简单的文件名匹配（支持*通配符）
        if (pattern.contains("/")) {
            return false; // 包含路径分隔符的不是文件名模式
        }
        
        return matchesSimplePattern(fileName, pattern);
    }
    
    /**
     * 检查路径是否匹配模式
     */
    private static boolean matchesPathPattern(String filePath, String pattern) {
        // 精确路径匹配
        if (filePath.equals(pattern)) {
            return true;
        }
        
        // 目录匹配（以/结尾的模式）
        if (pattern.endsWith("/")) {
            String dirPattern = pattern.substring(0, pattern.length() - 1);
            return filePath.startsWith(dirPattern + "/") || filePath.equals(dirPattern);
        }
        
        // 路径包含匹配
        return filePath.contains(pattern);
    }
    
    /**
     * 检查是否匹配glob模式
     */
    private static boolean matchesGlobPattern(String filePath, String pattern) {
        try {
            // 使用缓存的PathMatcher
            PathMatcher matcher = pathMatcherCache.computeIfAbsent(pattern, p -> {
                try {
                    return FileSystems.getDefault().getPathMatcher("glob:" + p);
                } catch (Exception e) {
                    log.debug("Invalid glob pattern '{}': {}", p, e.getMessage());
                    return null;
                }
            });
            
            if (matcher != null) {
                Path path = Paths.get(filePath);
                return matcher.matches(path) || matcher.matches(path.getFileName());
            }
        } catch (Exception e) {
            log.debug("Error matching glob pattern '{}' against '{}': {}", pattern, filePath, e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 简单的通配符匹配（支持*和?）
     */
    private static boolean matchesSimplePattern(String text, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        
        // 转换为正则表达式
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
        
        try {
            return text.matches(regex);
        } catch (Exception e) {
            log.debug("Error matching simple pattern '{}' against '{}': {}", pattern, text, e.getMessage());
            return false;
        }
    }

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
        
        // 过滤需要排除的文件
        List<Change> filteredChanges = includedChanges.stream()
                .filter(change -> {
                    String filePath = getFilePathFromChange(change);
                    return filePath == null || !shouldExcludeFile(filePath);
                })
                .collect(Collectors.toList());
        
        List<FilePath> filteredUnversionedFiles = unversionedFiles.stream()
                .filter(filePath -> !shouldExcludeFile(filePath.getPath()))
                .collect(Collectors.toList());
        
        // 基本差异信息
        String rawDiff = computeDiff(filteredChanges, filteredUnversionedFiles, project);
        result.put("rawDiff", rawDiff);
        
        // 收集变更文件的相关信息
        List<Map<String, Object>> fileContexts = new ArrayList<>();
        
        // 处理已版本控制的变更
        for (Change change : filteredChanges) {
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
        for (FilePath unversionedFile : filteredUnversionedFiles) {
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
    
    /**
     * 从Change对象中提取文件路径
     */
    private static String getFilePathFromChange(Change change) {
        if (change.getVirtualFile() != null) {
            return change.getVirtualFile().getPath();
        } else if (change.getBeforeRevision() != null) {
            return change.getBeforeRevision().getFile().getPath();
        } else if (change.getAfterRevision() != null) {
            return change.getAfterRevision().getFile().getPath();
        }
        return null;
    }

    public static String computeDiff(@NotNull List<Change> includedChanges,
                                     @NotNull List<FilePath> unversionedFiles,
                                     @NotNull Project project) {
        StringBuilder diffBuilder = new StringBuilder();

        // 过滤需要排除的文件
        List<Change> filteredChanges = includedChanges.stream()
                .filter(change -> {
                    String filePath = getFilePathFromChange(change);
                    return filePath == null || !shouldExcludeFile(filePath);
                })
                .collect(Collectors.toList());
        
        List<FilePath> filteredUnversionedFiles = unversionedFiles.stream()
                .filter(filePath -> !shouldExcludeFile(filePath.getPath()))
                .collect(Collectors.toList());

        // 处理已版本控制的变更
        String existingDiff = computeDiff(filteredChanges, project);
        diffBuilder.append(existingDiff);

        // 处理未版本控制的文件
        for (FilePath unversionedFile : filteredUnversionedFiles) {
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

        // 过滤需要排除的文件
        List<Change> filteredChanges = includedChanges.stream()
                .filter(change -> {
                    String filePath = getFilePathFromChange(change);
                    return filePath == null || !shouldExcludeFile(filePath);
                })
                .collect(Collectors.toList());

        // 按仓库分组处理变更
        Map<GitRepository, List<Change>> changesByRepository = filteredChanges.stream()
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
        // 过滤需要排除的文件
        List<Change> filteredChanges = includedChanges.stream()
                .filter(change -> {
                    String filePath = getFilePathFromChange(change);
                    return filePath == null || !shouldExcludeFile(filePath);
                })
                .toList();
        
        List<FilePath> filteredUnversionedFiles = unversionedFiles.stream()
                .filter(filePath -> !shouldExcludeFile(filePath.getPath()))
                .toList();
        //如果两个集合都为空，应该直接抛出异常
        if (filteredChanges.isEmpty() && filteredUnversionedFiles.isEmpty()) {
            //异常中要补充信息，也许是因为用户设置了排除规则，导致所有文件都被排除
            throw new IllegalArgumentException("Both includedChanges and unversionedFiles are empty after exclusion. " +
                    "Perhaps all files are excluded by the exclusion rules?");  
        }
        
        // 统一处理所有文件变更，消除特殊情况
        List<FileChange> changes = FileChange.fromGitChanges(filteredChanges, filteredUnversionedFiles);
        
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
