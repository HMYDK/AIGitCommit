package com.hmydk.aigit.config;

import com.hmydk.aigit.pojo.PromptInfo;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

public class ApiKeyConfigurable implements Configurable {

    private ApiKeyConfigurableUI ui;
    private final ApiKeySettings settings = ApiKeySettings.getInstance();

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "AI Git Commit";
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
                || isCustomPromptsModified() || isCustomPromptModified();
    }


    @Override
    public void apply() {
        settings.setAiModel((String) ui.getModelComboBox().getSelectedItem());
        settings.setApiKey(ui.getApiKeyField().getText());
        settings.setCommitLanguage((String) ui.getLanguageComboBox().getSelectedItem());
        saveCustomPromptsAndChoosedPrompt();
    }

    @Override
    public void reset() {
        settings.setCustomPrompts(new ArrayList<>());
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
            loadChoosedPrompt();
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

    private void loadChoosedPrompt() {
        if (settings.getCustomPrompt() != null) {
            DefaultTableModel model = (DefaultTableModel) ui.getCustomPromptsTable().getModel();
            int rowCount = model.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                String description = (String) model.getValueAt(i, 0);
                String prompt = (String) model.getValueAt(i, 1);
                if (settings.getCustomPrompt().getDescription().equals(description)
                    && settings.getCustomPrompt().getPrompt().equals(prompt)) {
                    ui.getCustomPromptsTable().setRowSelectionInterval(i, i);
                }
            }
        }
    }

    private void saveCustomPromptsAndChoosedPrompt() {
        DefaultTableModel model = (DefaultTableModel) ui.getCustomPromptsTable().getModel();
        int selectedRow = ui.getSELECTED_ROW();
        int rowCount = model.getRowCount();
        List<PromptInfo> customPrompts = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            String description = (String) model.getValueAt(i, 0);
            String prompt = (String) model.getValueAt(i, 1);
            PromptInfo promptInfo = new PromptInfo(description, prompt);
            customPrompts.add(i, promptInfo);

            //处理选中的行数据作为新的prompt
            if (selectedRow == i) {
                settings.setCustomPrompt(promptInfo);
            }
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

    private boolean isCustomPromptModified() {
        int selectedRow = ui.getSELECTED_ROW();
        DefaultTableModel model = (DefaultTableModel) ui.getCustomPromptsTable().getModel();
        int tableRowCount = model.getRowCount();

        if (selectedRow >= tableRowCount) {
            return true;
        }

        return !model.getValueAt(selectedRow, 0).equals(settings.getCustomPrompt().getDescription())
                || !model.getValueAt(selectedRow, 1).equals(settings.getCustomPrompt().getDescription());
    }
}