package com.hmydk.aigit.context;

import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 统一的文件变更处理类
 * Linus式设计：消除版本控制/未版本控制的特殊情况
 * 一个类处理所有文件变更类型
 */
public class FileChange {
    private final String path;
    private final FileChangeType type;
    private final String language;
    private final String extension;
    private final int linesAdded;
    private final int linesDeleted;
    private final String diffContent;
    private final String summary;
    
    public FileChange(String path, FileChangeType type, String language, String extension,
                     int linesAdded, int linesDeleted, String diffContent, String summary) {
        this.path = path;
        this.type = type;
        this.language = language;
        this.extension = extension;
        this.linesAdded = linesAdded;
        this.linesDeleted = linesDeleted;
        this.diffContent = diffContent;
        this.summary = summary;
    }
    
    /**
     * 统一处理：从Git Change创建FileChange
     * 消除特殊情况，统一处理逻辑
     */
    public static List<FileChange> fromGitChanges(List<Change> changes, List<FilePath> unversionedFiles) {
        List<FileChange> result = new ArrayList<>();
        
        // 处理版本控制的文件
        for (Change change : changes) {
            FileChange fileChange = fromGitChange(change);
            if (fileChange != null) {
                result.add(fileChange);
            }
        }
        
        // 处理未版本控制的文件（统一处理，无特殊情况）
        for (FilePath filePath : unversionedFiles) {
            FileChange fileChange = fromUnversionedFile(filePath);
            if (fileChange != null) {
                result.add(fileChange);
            }
        }
        
        return result;
    }
    
    private static FileChange fromGitChange(Change change) {
        try {
            String path = getChangePath(change);
            if (path == null) return null;
            
            FileChangeType type = determineChangeType(change);
            String extension = extractExtension(path);
            String language = determineLanguage(path, extension);
            
            // 计算行数变更
            int[] lineChanges = calculateLineChanges(change);
            int linesAdded = lineChanges[0];
            int linesDeleted = lineChanges[1];
            
            // 生成diff内容
            String diffContent = generateDiffContent(change, type);
            
            // 生成摘要
            String summary = generateSummary(path, type, linesAdded, linesDeleted);
            
            return new FileChange(path, type, language, extension, 
                                linesAdded, linesDeleted, diffContent, summary);
        } catch (Exception e) {
            // 静默处理异常，返回null
            return null;
        }
    }
    
    private static FileChange fromUnversionedFile(FilePath filePath) {
        try {
            String path = filePath.getPath();
            String extension = extractExtension(path);
            String language = determineLanguage(path, extension);
            
            // 未版本控制文件都是新增
            FileChangeType type = FileChangeType.ADDED;
            
            // 计算文件行数
            int linesAdded = countFileLines(path);
            int linesDeleted = 0;
            
            // 生成diff内容
            String diffContent = String.format("[ADD]: %s\n+++ %s\n", path, path);
            
            // 生成摘要
            String summary = String.format("Add new %s file", language);
            
            return new FileChange(path, type, language, extension,
                                linesAdded, linesDeleted, diffContent, summary);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String getChangePath(Change change) {
        if (change.getAfterRevision() != null) {
            return change.getAfterRevision().getFile().getPath();
        }
        if (change.getBeforeRevision() != null) {
            return change.getBeforeRevision().getFile().getPath();
        }
        return null;
    }
    
    private static FileChangeType determineChangeType(Change change) {
        if (change.getBeforeRevision() == null) {
            return FileChangeType.ADDED;
        }
        if (change.getAfterRevision() == null) {
            return FileChangeType.DELETED;
        }
        // 检查是否是移动
        String beforePath = change.getBeforeRevision().getFile().getPath();
        String afterPath = change.getAfterRevision().getFile().getPath();
        if (!beforePath.equals(afterPath)) {
            return FileChangeType.MOVED;
        }
        return FileChangeType.MODIFIED;
    }
    
    private static String extractExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0 && lastDot < path.length() - 1) {
            return path.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
    
    private static String determineLanguage(String path, String extension) {
        // 基于扩展名确定语言
        switch (extension) {
            case "java": return "Java";
            case "js": case "jsx": return "JavaScript";
            case "ts": case "tsx": return "TypeScript";
            case "py": return "Python";
            case "cpp": case "cc": case "cxx": return "C++";
            case "c": return "C";
            case "go": return "Go";
            case "rs": return "Rust";
            case "php": return "PHP";
            case "rb": return "Ruby";
            case "swift": return "Swift";
            case "kt": return "Kotlin";
            case "scala": return "Scala";
            case "html": return "HTML";
            case "css": return "CSS";
            case "scss": case "sass": return "SCSS";
            case "json": return "JSON";
            case "xml": return "XML";
            case "yml": case "yaml": return "YAML";
            case "md": return "Markdown";
            case "txt": return "Text";
            case "sh": return "Shell";
            case "sql": return "SQL";
            default: return "Unknown";
        }
    }
    
    private static int[] calculateLineChanges(Change change) {
        // 简化的行数计算，实际项目中可以使用更精确的diff算法
        try {
            ContentRevision before = change.getBeforeRevision();
            ContentRevision after = change.getAfterRevision();
            
            if (before == null && after != null) {
                // 新增文件
                String content = after.getContent();
                int lines = content != null ? content.split("\n").length : 0;
                return new int[]{lines, 0};
            }
            
            if (before != null && after == null) {
                // 删除文件
                String content = before.getContent();
                int lines = content != null ? content.split("\n").length : 0;
                return new int[]{0, lines};
            }
            
            if (before != null && after != null) {
                // 修改文件 - 简化计算
                String beforeContent = before.getContent();
                String afterContent = after.getContent();
                
                if (beforeContent == null || afterContent == null) {
                    return new int[]{0, 0};
                }
                
                int beforeLines = beforeContent.split("\n").length;
                int afterLines = afterContent.split("\n").length;
                
                if (afterLines > beforeLines) {
                    return new int[]{afterLines - beforeLines, 0};
                } else if (beforeLines > afterLines) {
                    return new int[]{0, beforeLines - afterLines};
                } else {
                    // 行数相同，假设有修改
                    return new int[]{1, 1};
                }
            }
        } catch (Exception e) {
            // 异常时返回默认值
        }
        
        return new int[]{0, 0};
    }
    
    private static String generateDiffContent(Change change, FileChangeType type) {
        String path = getChangePath(change);
        if (path == null) return "";
        
        try {
            // Linus修复：生成真正的diff内容，不只是文件头！
            // "Bad programmers worry about the code. Good programmers worry about data structures."
            // 这里我们需要真正的数据（代码变更），不是假数据（文件头）
            
            ContentRevision before = change.getBeforeRevision();
            ContentRevision after = change.getAfterRevision();
            
            StringBuilder diffBuilder = new StringBuilder();
            
            switch (type) {
                case ADDED:
                    diffBuilder.append(String.format("[ADD]: %s\n", path));
                    if (after != null) {
                        String content = after.getContent();
                        if (content != null) {
                            diffBuilder.append("+++ ").append(path).append("\n");
                            for (String line : content.split("\n")) {
                                diffBuilder.append("+").append(line).append("\n");
                            }
                        }
                    }
                    break;
                    
                case DELETED:
                    diffBuilder.append(String.format("[DELETE]: %s\n", path));
                    if (before != null) {
                        String content = before.getContent();
                        if (content != null) {
                            diffBuilder.append("--- ").append(path).append("\n");
                            for (String line : content.split("\n")) {
                                diffBuilder.append("-").append(line).append("\n");
                            }
                        }
                    }
                    break;
                    
                case MOVED:
                    String beforePath = change.getBeforeRevision().getFile().getPath();
                    diffBuilder.append(String.format("[MOVE]: %s -> %s\n", beforePath, path));
                    // 对于移动，也显示内容变化（如果有的话）
                    if (before != null && after != null) {
                        String beforeContent = before.getContent();
                        String afterContent = after.getContent();
                        if (beforeContent != null && afterContent != null && !beforeContent.equals(afterContent)) {
                            diffBuilder.append(generateSimpleDiff(beforeContent, afterContent, path));
                        }
                    }
                    break;
                    
                case MODIFIED:
                default:
                    diffBuilder.append(String.format("[MODIFY]: %s\n", path));
                    if (before != null && after != null) {
                        String beforeContent = before.getContent();
                        String afterContent = after.getContent();
                        if (beforeContent != null && afterContent != null) {
                            diffBuilder.append(generateSimpleDiff(beforeContent, afterContent, path));
                        }
                    }
                    break;
            }
            
            return diffBuilder.toString();
            
        } catch (Exception e) {
            // 异常时返回基本信息，但不能返回空
            return String.format("[%s]: %s\n(Error generating diff content: %s)\n", 
                               type.name(), path, e.getMessage());
        }
    }
    
    /**
     * 生成简单的diff内容
     * Linus式实现：简单、直接、有效
     */
    private static String generateSimpleDiff(String beforeContent, String afterContent, String path) {
        StringBuilder diff = new StringBuilder();
        diff.append("--- a/").append(path).append("\n");
        diff.append("+++ b/").append(path).append("\n");
        
        String[] beforeLines = beforeContent.split("\n");
        String[] afterLines = afterContent.split("\n");
        
        // 简化的diff算法：显示所有变更
        int maxLines = Math.max(beforeLines.length, afterLines.length);
        
        for (int i = 0; i < maxLines; i++) {
            String beforeLine = i < beforeLines.length ? beforeLines[i] : null;
            String afterLine = i < afterLines.length ? afterLines[i] : null;
            
            if (beforeLine != null && afterLine != null) {
                if (!beforeLine.equals(afterLine)) {
                    diff.append("-").append(beforeLine).append("\n");
                    diff.append("+").append(afterLine).append("\n");
                }
            } else if (beforeLine != null) {
                diff.append("-").append(beforeLine).append("\n");
            } else if (afterLine != null) {
                diff.append("+").append(afterLine).append("\n");
            }
        }
        
        return diff.toString();
    }
    
    private static String generateSummary(String path, FileChangeType type, int added, int deleted) {
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        
        switch (type) {
            case ADDED:
                return String.format("Add %s (+%d lines)", fileName, added);
            case DELETED:
                return String.format("Delete %s (-%d lines)", fileName, deleted);
            case MOVED:
                return String.format("Move %s", fileName);
            case MODIFIED:
            default:
                if (added > 0 && deleted > 0) {
                    return String.format("Update %s (+%d/-%d lines)", fileName, added, deleted);
                } else if (added > 0) {
                    return String.format("Update %s (+%d lines)", fileName, added);
                } else if (deleted > 0) {
                    return String.format("Update %s (-%d lines)", fileName, deleted);
                } else {
                    return String.format("Update %s", fileName);
                }
        }
    }
    
    private static int countFileLines(String path) {
        try {
            return (int) Files.lines(Paths.get(path)).count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    // 向后兼容：生成旧格式
    public String toLegacyFormat() {
        return String.format("- 文件: %s\n  类型: %s\n  文件类型: %s\n  扩展名: %s\n  编程语言: %s\n\n",
                           path, type.name(), language.toUpperCase(), extension, language);
    }
    
    // Getters
    public String getPath() { return path; }
    public FileChangeType getType() { return type; }
    public FileChangeType getChangeType() { return type; }  // 别名方法，保持API一致性
    public String getLanguage() { return language; }
    public String getExtension() { return extension; }
    public int getLinesAdded() { return linesAdded; }
    public int getLinesDeleted() { return linesDeleted; }
    public String getDiffContent() { return diffContent; }
    public String getSummary() { return summary; }
    
    @Override
    public String toString() {
        return String.format("FileChange{path='%s', type=%s, +%d/-%d}", 
                           path, type, linesAdded, linesDeleted);
    }
}