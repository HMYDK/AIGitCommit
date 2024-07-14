package com.hmydk.aigit.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsHistorySession;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}
