package com.hmydk.aigit.util;

import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * GItCommitUtil
 *
 * @author hmydk
 */
public class GItCommitUtil {
    private static final Logger log = LoggerFactory.getLogger(GItCommitUtil.class);

    public static String computeDiff(@NotNull List<Change> includedChanges, @NotNull Project project) {
        GitRepositoryManager gitRepositoryManager = GitRepositoryManager.getInstance(project);
        StringBuilder diffBuilder = new StringBuilder();

        // 按仓库分组处理变更
        Map<GitRepository, List<Change>> changesByRepository = includedChanges.stream()
                .map(change -> {
                    if (change.getVirtualFile() != null) {
                        GitRepository repository = gitRepositoryManager
                                .getRepositoryForFileQuick(change.getVirtualFile());
                        if (repository != null) {
                            return new AbstractMap.SimpleEntry<>(repository, change);
                        }
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
            if (change.getVirtualFile() != null &&
                    change.getVirtualFile().getPath().endsWith(filePath)) {
                return switch (change.getType()) {
                    case NEW -> "[ADD]";
                    case DELETED -> "[DELETE]";
                    case MOVED -> "[MOVE]";
                    case MODIFICATION -> "[MODIFY]";
                };
            }
        }
        return "[UNKNOWN]";
    }
}
