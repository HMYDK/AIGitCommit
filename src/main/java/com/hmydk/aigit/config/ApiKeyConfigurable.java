package com.hmydk.aigit.config;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.table.DefaultTableModel;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.pojo.PromptInfo;
import com.intellij.openapi.options.Configurable;

public class ApiKeyConfigurable implements Configurable {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyConfigurable.class);
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
        // return
        // !settings.getSelectedClient().equals(ui.getClientComboBox().getSelectedItem())
        // ||
        // !settings.getSelectedModule().equals(ui.getModuleComboBox().getSelectedItem())
        // ||
        // !settings.getCommitLanguage().equals(ui.getLanguageComboBox().getSelectedItem())
        // || isCustomPromptsModified() || isCustomPromptModified() ||
        // isPromptTypeModified();
        return true;
    }

    @Override
    public void apply() {
        if (ui == null) {
            return; // 如果UI已经被销毁，直接返回
        }

        // 保存当前设置到临时变量
        String selectedClient = (String) ui.getClientComboBox().getSelectedItem();
        String selectedModule = (String) ui.getModuleComboBox().getSelectedItem();
        String commitLanguage = (String) ui.getLanguageComboBox().getSelectedItem();

        // 应用设置
        settings.setSelectedClient(selectedClient);
        settings.setSelectedModule(selectedModule);
        settings.setCommitLanguage(commitLanguage);

        // 保存prompt内容
        Object selectedPromptType = ui.getPromptTypeComboBox().getSelectedItem();
        if (Constants.CUSTOM_PROMPT.equals((String) selectedPromptType)) {
            saveCustomPromptsAndChoosedPrompt();
        }
        // 保存prompt类型
        settings.setPromptType((String) selectedPromptType);

        // 保存文件忽略设置
        saveFileExclusionSettings();

        // 保存网络设置
        saveNetworkSettings();
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
            ui.getClientComboBox().setSelectedItem(settings.getSelectedClient());
            ui.getModuleComboBox().setSelectedItem(settings.getSelectedModule());
            ui.getLanguageComboBox().setSelectedItem(settings.getCommitLanguage());

            // 设置表格数据
            loadCustomPrompts();
            // 设置下拉框选中项
            loadChoosedPrompt();

            // 设置提示类型
            ui.getPromptTypeComboBox().setSelectedItem(settings.getPromptType());

            // 加载文件忽略设置
            loadFileExclusionSettings();

            // 加载网络设置
            loadNetworkSettings();
        }
    }

    private void loadCustomPrompts() {
        DefaultTableModel model = (DefaultTableModel) ui.getCustomPromptsTable().getModel();
        model.setRowCount(0);
        for (PromptInfo prompt : settings.getCustomPrompts()) {
            if (prompt != null) {
                model.addRow(new String[] { prompt.getDescription(), prompt.getPrompt() });
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

            // 处理选中的行数据作为新的prompt
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

    private void saveFileExclusionSettings() {
        boolean enableFileExclusion = ui.getEnableFileExclusionCheckBox().isSelected();
        String excludePatternsText = ui.getExcludePatternsTextArea().getText();

        settings.setEnableFileExclusion(enableFileExclusion);

        // 解析文本区域的内容为列表
        List<String> excludePatterns = new ArrayList<>();
        if (excludePatternsText != null && !excludePatternsText.trim().isEmpty()) {
            String[] lines = excludePatternsText.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    excludePatterns.add(trimmed);
                }
            }
        }
        settings.setExcludePatterns(excludePatterns);
    }

    private void saveNetworkSettings() {
        if (ui != null && ui.getUseSystemProxyCheckBox() != null) {
            settings.setUseSystemProxy(ui.getUseSystemProxyCheckBox().isSelected());
        }
    }

    private void loadNetworkSettings() {
        if (ui != null && ui.getUseSystemProxyCheckBox() != null) {
            ui.getUseSystemProxyCheckBox().setSelected(settings.isUseSystemProxy());
        }
    }

    private void loadFileExclusionSettings() {
        ui.getEnableFileExclusionCheckBox().setSelected(settings.isEnableFileExclusion());

        List<String> excludePatterns = settings.getExcludePatterns();
        if (excludePatterns != null && !excludePatterns.isEmpty()) {
            ui.getExcludePatternsTextArea().setText(String.join("\n", excludePatterns));
        } else {
            ui.getExcludePatternsTextArea().setText(String.join("\n", Constants.DEFAULT_EXCLUDE_PATTERNS));
        }

        // 根据复选框状态设置组件启用状态
        boolean enabled = settings.isEnableFileExclusion();
        ui.getExcludePatternsTextArea().setEnabled(enabled);
        ui.getExcludePatternsTextArea().getParent().getParent().setEnabled(enabled); // 设置滚动面板
    }
}