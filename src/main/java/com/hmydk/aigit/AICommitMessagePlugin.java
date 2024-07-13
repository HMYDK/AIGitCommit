package com.hmydk.aigit;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;

/**
 * AICommitMessagePlugin
 *
 * @author hmydk
 */
public class AICommitMessagePlugin extends CheckinHandlerFactory {

    @Override
    public @NotNull CheckinHandler createHandler(@NotNull CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
        return new AICommitMessageHandler(panel);
    }
}
