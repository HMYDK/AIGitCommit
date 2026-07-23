package com.hmydk.aigit.compat;

import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.impl.PartialChangesUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The only class allowed to call IntelliJ VCS implementation APIs.
 *
 * <p>If an IDE release changes one of these APIs, Plugin Verifier and the compatibility CI point
 * to this class. We fail generation instead of falling back to the whole file, because that
 * fallback would include hunks the user deliberately left unchecked.</p>
 */
public final class DefaultIdeVcsAdapter implements IdeVcsAdapter {

    @Override
    public @NotNull List<Change> prepareSelectedChanges(@NotNull Project project,
                                                        @NotNull List<Change> includedChanges) {
        if (includedChanges.isEmpty()) {
            return List.of();
        }

        try {
            return List.copyOf(PartialChangesUtil.wrapPartialChanges(project, includedChanges));
        } catch (RuntimeException | LinkageError error) {
            throw new IdeVcsCompatibilityException(
                    "The current IDE could not provide the selected commit hunks. " +
                            "AI Git Commit stopped to avoid including unchecked code.",
                    error
            );
        }
    }

    @Override
    public @NotNull String buildVersionedDiff(@NotNull Project project,
                                              @NotNull List<Change> preparedChanges) {
        GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
        StringBuilder diffBuilder = new StringBuilder();

        Map<GitRepository, List<Change>> changesByRepository = preparedChanges.stream()
                .map(change -> {
                    GitRepository repository = findRepository(repositoryManager, change);
                    return repository == null
                            ? null
                            : new AbstractMap.SimpleEntry<>(repository, change);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));

        try {
            for (Map.Entry<GitRepository, List<Change>> entry : changesByRepository.entrySet()) {
                List<Change> changes = entry.getValue();
                List<FilePatch> filePatches = IdeaTextPatchBuilder.buildPatch(
                        project,
                        changes,
                        entry.getKey().getRoot().toNioPath(),
                        false,
                        true
                );

                for (FilePatch patch : filePatches) {
                    String filePath = patch.getAfterName() != null
                            ? patch.getAfterName()
                            : patch.getBeforeName();
                    diffBuilder.append(resolveChangeType(changes, filePath))
                            .append(": ")
                            .append(filePath)
                            .append("\n");

                    StringWriter writer = new StringWriter();
                    UnifiedDiffWriter.write(project, List.of(patch), writer, "\n", null);
                    diffBuilder.append(writer).append("\n");
                }
            }
        } catch (Exception | LinkageError error) {
            throw new IdeVcsCompatibilityException(
                    "The current IDE could not build the selected Git diff.",
                    error
            );
        }

        return diffBuilder.toString();
    }

    private GitRepository findRepository(GitRepositoryManager repositoryManager, Change change) {
        if (change.getVirtualFile() != null) {
            return repositoryManager.getRepositoryForFileQuick(change.getVirtualFile());
        }
        if (change.getBeforeRevision() != null) {
            return repositoryManager.getRepositoryForFile(change.getBeforeRevision().getFile());
        }
        return null;
    }

    private String resolveChangeType(List<Change> changes, String filePath) {
        Change matchingChange = changes.stream()
                .filter(change -> matchesPath(change, filePath))
                .findFirst()
                .orElse(changes.isEmpty() ? null : changes.get(0));

        if (matchingChange == null) {
            return "[UNKNOWN]";
        }

        return switch (matchingChange.getType()) {
            case NEW -> "[ADD]";
            case DELETED -> "[DELETE]";
            case MOVED -> "[MOVE]";
            case MODIFICATION -> "[MODIFY]";
        };
    }

    private boolean matchesPath(Change change, String filePath) {
        if (filePath == null) {
            return false;
        }
        if (change.getAfterRevision() != null &&
                filePath.equals(change.getAfterRevision().getFile().getPath())) {
            return true;
        }
        return change.getBeforeRevision() != null &&
                filePath.equals(change.getBeforeRevision().getFile().getPath());
    }
}
