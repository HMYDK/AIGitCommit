package com.hmydk.aigit.config;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.pojo.PromptInfo;
import com.hmydk.aigit.util.FileUtil;
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
                || !settings.getApiKey().equals(new String(ui.getApiKeyField().getPassword()))
                || !settings.getCommitLanguage().equals(ui.getLanguageComboBox().getSelectedItem())
                || isCustomPromptsModified() || isCustomPromptModified() || isPromptTypeModified();
    }


    @Override
    public void apply() {
        settings.setAiModel((String) ui.getModelComboBox().getSelectedItem());
        settings.setApiKey(new String(ui.getApiKeyField().getPassword()));
        settings.setCommitLanguage((String) ui.getLanguageComboBox().getSelectedItem());

        Object selectedPromptType = ui.getPromptTypeComboBox().getSelectedItem();
        if (Constants.CUSTOM_PROMPT.equals((String) selectedPromptType)) {
            saveCustomPromptsAndChoosedPrompt();
        }
        settings.setPromptType((String) selectedPromptType);

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
            loadChoosedPrompt();

            //设置提示类型
            ui.getPromptTypeComboBox().setSelectedItem(settings.getPromptType());
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


    private boolean isPromptTypeModified() {
        Object selectedPromptType = ui.getPromptTypeComboBox().getSelectedItem();
        return !settings.getPromptType().equals(selectedPromptType);
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