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

        // 通过包括的更改，创建仓库到更改的映射，并丢弃 nulls
        Map<GitRepository, List<Change>> changesByRepository = includedChanges.stream()
                .map(change -> {
                    if (change.getVirtualFile() != null) {
                        GitRepository repository = gitRepositoryManager.getRepositoryForFileQuick(change.getVirtualFile());
                        if (repository != null) {
                            return new AbstractMap.SimpleEntry<>(repository, change);
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        // 计算每个仓库的差异
        return changesByRepository.entrySet().stream()
                .map(entry -> {
                    GitRepository repository = entry.getKey();
                    List<Change> changes = entry.getValue();
                    if (repository != null) {
                        try {
                            List<FilePatch> filePatches = IdeaTextPatchBuilder.buildPatch(
                                    project,
                                    changes,
                                    repository.getRoot().toNioPath(),
                                    false,
                                    true
                            );
                            StringWriter stringWriter = new StringWriter();
                            stringWriter.write("Repository: " + repository.getRoot().getPath() + "\n");
                            UnifiedDiffWriter.write(project, filePatches, stringWriter, "\n", null);
                            return stringWriter.toString();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }
}
