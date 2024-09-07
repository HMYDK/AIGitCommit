package com.hmydk.aigit.config;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ApiKeyConfigurable implements Configurable {

    private ApiKeyConfigurableUI ui;
    private ApiKeySettings settings = ApiKeySettings.getInstance();

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
        for (String[] prompt : settings.getCustomPrompts()) {
            model.addRow(prompt);
        }
    }

    private void saveCustomPrompts() {
        DefaultTableModel model = (DefaultTableModel) ui.getCustomPromptsTable().getModel();
        int rowCount = model.getRowCount();
        String[][] customPrompts = new String[rowCount][2];
        for (int i = 0; i < rowCount; i++) {
            customPrompts[i][0] = (String) model.getValueAt(i, 0);
            customPrompts[i][1] = (String) model.getValueAt(i, 1);
        }
        settings.setCustomPrompts(customPrompts);
    }

    private boolean isCustomPromptsModified() {
        DefaultTableModel model = (DefaultTableModel) ui.getCustomPromptsTable().getModel();
        int rowCount = model.getRowCount();
        if (rowCount != settings.getCustomPrompts().length) {
            return true;
        }
        for (int i = 0; i < rowCount; i++) {
            if (!model.getValueAt(i, 0).equals(settings.getCustomPrompts()[i][0])
                    || !model.getValueAt(i, 1).equals(settings.getCustomPrompts()[i][1])) {
                return true;
            }
        }
        return false;
    }
}