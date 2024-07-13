package com.hmydk.aigit.ui;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


/**
 * AICommitMessageSettingsConfigurable
 *
 * @author hmydk
 */
public class AICommitMessageSettingsConfigurable implements Configurable {
    private JPanel myMainPanel;
    private JTextField apiKeyField;
    private JCheckBox enableAutoGenerationCheckBox;

    @Nullable
    @Override
    public JComponent createComponent() {
        // Create and return the settings UI
        return myMainPanel;
    }

    @Override
    public boolean isModified() {
        // Check if settings have been modified
        return false;
    }

    @Override
    public void apply() {
        // Apply the settings
    }

    @Override
    public String getDisplayName() {
        return "AI Commit Message";
    }
}
