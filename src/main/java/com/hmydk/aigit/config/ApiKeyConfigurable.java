package com.hmydk.aigit.config;

import com.hmydk.aigit.pojo.PromptInfo;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class ApiKeyConfigurable implements Configurable {

    private ApiKeyConfigurableUI ui;
    private final ApiKeySettings settings = ApiKeySettings.getInstance();

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "AI Git Commit 设置";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        ui = new ApiKeyConfigurableUI();
        loadSettings();
        return ui.getMainPanel();
    }

    @Override
    public boolean isModified() {
        return !settings.getAiModel().equals(ui.getModelComboBox().getSelectedItem())
                || !settings.getApiKey().equals(ui.getApiKeyField().getText())
                || !settings.getCommitLanguage().equals(ui.getLanguageComboBox().getSelectedItem())
                || isCustomPromptsModified();
    }

    @Override
    public void apply() {
        settings.setAiModel((String) ui.getModelComboBox().getSelectedItem());
        settings.setApiKey(ui.getApiKeyField().getText());
        settings.setCommitLanguage((String) ui.getLanguageComboBox().getSelectedItem());
        saveCustomPrompts();
    }

    @Override
    public void reset() {
        loadSettings();
    }

    @Override
    public void disposeUIResources() {
        ui = null;
    }

    private void loadSettings() {
        if (ui != null) {
            ui.getModelComboBox().setSelectedItem(settings.getAiModel());
            ui.getApiKeyField().setText(settings.getApiKey());
            ui.getLanguageComboBox().setSelectedItem(settings.getCommitLanguage());
            //设置表格数据
            loadCustomPrompts();
            //设置下拉框选中项
        }
    }

    private void loadCustomPrompts() {
        DefaultTableModel model = (DefaultTableModel) ui.getCustomPromptsTable().getModel();
        model.setRowCount(0);
        for (PromptInfo prompt : settings.getCustomPrompts()) {
            if (prompt != null) {
                model.addRow(new String[]{prompt.getDescription(), prompt.getPrompt()});
            }
        }
    }

    private void saveCustomPrompts() {
        DefaultTableModel model = (DefaultTableModel) ui.getCustomPromptsTable().getModel();
        int rowCount = model.getRowCount();
        List<PromptInfo> customPrompts = settings.getCustomPrompts();
        for (int i = 0; i < rowCount; i++) {
            String description = (String) model.getValueAt(i, 0);
            String prompt = (String) model.getValueAt(i, 1);
            PromptInfo promptInfo = new PromptInfo(description, prompt);
            customPrompts.set(i, promptInfo);
        }
        settings.setCustomPrompts(customPrompts);
    }

    private boolean isCustomPromptsModified() {
        DefaultTableModel model = (DefaultTableModel) ui.getCustomPromptsTable().getModel();
        int rowCount = model.getRowCount();
        if (rowCount != settings.getCustomPrompts().size()) {
            return true;
        }
        for (int i = 0; i < rowCount; i++) {
            if (!model.getValueAt(i, 0).equals(settings.getCustomPrompts().get(i).getDescription())
                    || !model.getValueAt(i, 1).equals(settings.getCustomPrompts().get(i).getDescription())) {
                return true;
            }
        }
        return false;
    }
}