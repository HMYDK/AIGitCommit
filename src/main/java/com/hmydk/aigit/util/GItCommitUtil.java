package com.hmydk.aigit.util;

import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsHistorySession;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;


/**
 * GItCommitUtil
 *
 * @author hmydk
 */
public class GItCommitUtil {
    private static final Logger log = LoggerFactory.getLogger(GItCommitUtil.class);

    public static List<String> computeGitHistoryMsg(Project project, int number) {
        GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
        List<GitRepository> repositories = repositoryManager.getRepositories();
        if (repositories.isEmpty()) {
            return Collections.emptyList();
        }

        GitRepository repository = repositories.get(0);
        var filePath = VcsUtil.getFilePath(repository.getRoot());
        var vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(filePath);
        if (vcs == null) {
            return Collections.emptyList();
        }

        VcsHistoryProvider vcsHistoryProvider = vcs.getVcsHistoryProvider();
        if (vcsHistoryProvider == null) {
            return Collections.emptyList();
        }

        VcsHistorySession vcsHistorySession = null;
        try {
            vcsHistorySession = vcsHistoryProvider.createSessionFor(filePath);
        } catch (VcsException e) {
            log.error("Failed to create history session", e);
            return Collections.emptyList();
        }
        if (vcsHistorySession == null) {
            return Collections.emptyList();
        }

        List<VcsFileRevision> vcsHistory = vcsHistorySession.getRevisionList();
        if (vcsHistory == null || vcsHistory.isEmpty()) {
            return Collections.emptyList();
        }

        List<VcsFileRevision> vcsHistoryList = vcsHistory.subList(0, Math.min(number, vcsHistory.size()));
        List<String> historyList = new ArrayList<>();

        for (VcsFileRevision revision : vcsHistoryList) {
            historyList.add(revision.getCommitMessage() != null ? revision.getCommitMessage().trim() : "");
        }

        return historyList;
    }

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


    public static String commonBranch(List<Change> changes, Project project) {
        GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);

        // 获取每个变更文件对应的当前分支名称，并统计每个分支的出现次数
        Map<String, Long> branchCount = changes.stream()
                .map(change -> {
                    GitRepository repository = repositoryManager.getRepositoryForFileQuick(change.getVirtualFile());
                    return repository != null ? repository.getCurrentBranchName() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(branch -> branch, Collectors.counting()));

        // 找到出现次数最多的分支名称
        Optional<Map.Entry<String, Long>> maxEntry = branchCount.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        String branch = maxEntry.map(Map.Entry::getKey).orElse(null);

        if (branch == null) {
            // 硬编码的回退分支
            branch = "main";
        }
        return branch;
    }
}
