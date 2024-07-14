package com.hmydk.aigit.service;


import com.hmydk.aigit.service.impl.GeminiService;
import com.hmydk.aigit.util.ChangeContentExtractor;
import com.hmydk.aigit.util.PromptUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.changes.Change;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CommitMessageService {
    private final GeminiService aiService;

    public CommitMessageService() {
        this.aiService = new GeminiService();
    }

    public String generateCommitMessage(Project project, Collection<Change> changes) {
        if (changes == null || changes.isEmpty()) {
            throw new IllegalArgumentException("No changes selected for commit.");
        }

        List<String> allChangeContents = new ArrayList<>();
        for (Change change : changes) {
            allChangeContents.addAll(ChangeContentExtractor.extractChangeContent(project, change));
        }

        if (allChangeContents.isEmpty()) {
            throw new IllegalArgumentException("No relevant changes found for commit message generation.");
        }

        String prompt = PromptUtil.generatePrompt(allChangeContents);
        return aiService.generateCommitMessage(prompt);
    }

    public boolean showCommitMessageDialog(Project project, String commitMessage) {
        int result = Messages.showYesNoDialog(
                project,
                "Use the following AI-generated commit message?\n\n" + commitMessage,
                "AI Commit Message",
                Messages.getQuestionIcon()
        );
        return result == Messages.YES;
    }
}
