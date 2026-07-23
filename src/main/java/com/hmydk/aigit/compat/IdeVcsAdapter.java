package com.hmydk.aigit.compat;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Boundary around IntelliJ VCS implementation APIs that do not have stable public equivalents.
 */
public interface IdeVcsAdapter {

    /**
     * Replaces file-level changes with revisions containing only the hunks selected for commit.
     */
    @NotNull List<Change> prepareSelectedChanges(@NotNull Project project,
                                                 @NotNull List<Change> includedChanges);

    /**
     * Produces the legacy unified-diff representation used by the deprecated prompt path.
     */
    @NotNull String buildVersionedDiff(@NotNull Project project,
                                       @NotNull List<Change> preparedChanges);
}
