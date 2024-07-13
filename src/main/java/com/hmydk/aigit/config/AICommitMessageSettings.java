package com.hmydk.aigit.config;

import com.intellij.openapi.components.PersistentStateComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * AICommitMessageSettings
 *
 * @author hmydk
 */
public class AICommitMessageSettings implements PersistentStateComponent<AICommitMessageSettings.State> {
    State state = new State();

    public static class State {
        public String apiKey = "";
        public boolean enableAutoGeneration = true;
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }
}
