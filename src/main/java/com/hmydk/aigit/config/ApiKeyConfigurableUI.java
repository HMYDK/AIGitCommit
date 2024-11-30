package com.hmydk.aigit.config;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.AIService;
import com.hmydk.aigit.service.CommitMessageService;
import com.hmydk.aigit.util.PromptDialogUIUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

public class ApiKeyConfigurableUI {

    private JPanel mainPanel;
    private ComboBox<String> clientComboBox;
    private ComboBox<String> moduleComboBox;
    private ComboBox<String> languageComboBox;

    private ComboBox<String> promptTypeComboBox;
    private JBTable customPromptsTable;
    private DefaultTableModel customPromptsTableModel;
    private JPanel customPromptPanel;
    private JPanel projectPromptPanel;

    private JButton configButton;

    // 记录当前选中的行
    private int SELECTED_ROW = 0;

    private JPanel clientPanel;

    public ApiKeyConfigurableUI() {
        initComponents();
        layoutComponents();
        setupListeners();
    }

    private void initComponents() {
        clientComboBox = new ComboBox<>(Constants.LLM_CLIENTS);
        moduleComboBox = new ComboBox<>();
        moduleComboBox.setEditable(true);
        languageComboBox = new ComboBox<>(Constants.languages);
        promptTypeComboBox = new ComboBox<>(Constants.getAllPromptTypes());
        customPromptsTableModel = new DefaultTableModel(new String[]{"Description", "Prompt"}, 0);
        customPromptsTable = new JBTable(customPromptsTableModel);

        // 设置 Description 列的首选宽度和最大宽度
        customPromptsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        customPromptsTable.getColumnModel().getColumn(0).setMaxWidth(200);

        // 设置 Prompt 列可以自由伸展
        customPromptsTable.getColumnModel().getColumn(1).setPreferredWidth(400);

        customPromptPanel = createCustomPromptPanel();
        projectPromptPanel = createProjectPromptPanel();

        configButton = new JButton(AllIcons.General.Settings);
        configButton.setToolTipText("Configure Module Settings");

        // 创建包含Stream支持状态的面板
        clientPanel = new JPanel(new BorderLayout(5, 0));
        clientPanel.add(clientComboBox, BorderLayout.CENTER);

        // 添加Stream状态标签
        JLabel streamLabel = new JLabel();
        streamLabel.setForeground(JBColor.GRAY);
        clientPanel.add(streamLabel, BorderLayout.EAST);

        // 更新Stream状态显示
        updateStreamStatus(streamLabel, (String) clientComboBox.getSelectedItem());

        // 添加客户端选择监听器
        clientComboBox.addActionListener(e -> {
            String selectedClient = (String) clientComboBox.getSelectedItem();
            updateModuleComboBox(selectedClient);
            updateStreamStatus(streamLabel, selectedClient);
        });

        // 初始化模块下拉框
        updateModuleComboBox((String) clientComboBox.getSelectedItem());
    }

    private void layoutComponents() {
        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addComponent(new JBLabel("LLM client:"), gbc, 0, 0, 0.0);
        addComponent(clientPanel, gbc, 1, 0, 1.0);

        JPanel modulePanel = new JPanel(new BorderLayout(5, 0));
        modulePanel.add(moduleComboBox, BorderLayout.CENTER);
        modulePanel.add(configButton, BorderLayout.EAST);

        addComponent(new JBLabel("Module:"), gbc, 0, 1, 0.0);
        addComponent(modulePanel, gbc, 1, 1, 1.0);

        addComponent(new JBLabel("Language:"), gbc, 0, 3, 0.0);
        addComponent(languageComboBox, gbc, 1, 3, 1.0);

        addComponent(new JBLabel("Prompt type:"), gbc, 0, 4, 0.0);
        addComponent(promptTypeComboBox, gbc, 1, 4, 1.0);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(customPromptPanel, gbc);

        promptTypeComboBox.addActionListener(e -> updatePromptPanelVisibility());
    }

    private void addComponent(Component component, GridBagConstraints gbc, int gridx, int gridy, double weightx) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.weightx = weightx;
        mainPanel.add(component, gbc);
    }

    private JPanel createCustomPromptPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 创建一个包含原有 infoLabel 和新超链接的面板
        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.setBorder(JBUI.Borders.empty(0, 0, 5, 0)); // 添加底部间距

        JBLabel infoLabel = new JBLabel(
                "Select a prompt from the table below to use it as your commit message template.");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.PLAIN, 12));
        infoLabel.setForeground(JBColor.GRAY);
        labelPanel.add(infoLabel, BorderLayout.WEST);

        JLabel linkLabel = new JLabel(
                "<html><a href='https://github.com/HMYDK/AIGitCommit/discussions/23'>More Prompts ↗</a></html>");
        linkLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        linkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/HMYDK/AIGitCommit/discussions/23"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        labelPanel.add(linkLabel, BorderLayout.EAST);

        panel.add(labelPanel, BorderLayout.NORTH);

        // 创建表格面板
        JPanel customPromptsPanel = ToolbarDecorator.createDecorator(customPromptsTable)
                .setAddAction(button -> addCustomPrompt())
                .setRemoveAction(button -> removeCustomPrompt())
                .setEditAction(button -> editCustomPrompt(customPromptsTable.getSelectedRow()))
                .createPanel();
        panel.add(customPromptsPanel, BorderLayout.CENTER);

        // 添加表格选择监听器
        customPromptsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                SELECTED_ROW = Math.max(customPromptsTable.getSelectedRow(), 0);
            }
        });

        return panel;
    }

    private JPanel createProjectPromptPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel(
                "Using project-specific prompt from '" + Constants.PROJECT_PROMPT_FILE_NAME + "' in the project root.");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.PLAIN, 12));
        infoLabel.setForeground(JBColor.GRAY);
        panel.add(infoLabel, BorderLayout.CENTER);
        return panel;
    }

    private void updatePromptPanelVisibility() {
        String selectedPromptType = (String) promptTypeComboBox.getSelectedItem();
        if (Constants.CUSTOM_PROMPT.equals(selectedPromptType)) {
            mainPanel.remove(projectPromptPanel);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 5;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            mainPanel.add(customPromptPanel, gbc);
        } else {
            mainPanel.remove(customPromptPanel);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 5;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            mainPanel.add(projectPromptPanel, gbc);
        }
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void addCustomPrompt() {
        PromptDialogUIUtil.PromptDialogUI promptDialogUI = PromptDialogUIUtil.showPromptDialog(true, null, null);

        SwingUtilities.invokeLater(() -> {
            UIManager.put("OptionPane.okButtonText", "OK");
            UIManager.put("OptionPane.cancelButtonText", "Cancel");
            JOptionPane optionPane = new JOptionPane(promptDialogUI.getPanel(), JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
            JDialog dialog = optionPane.createDialog(mainPanel, "Add Prompt");
            dialog.setVisible(true);

            int result = (Integer) optionPane.getValue();
            if (result == JOptionPane.OK_OPTION) {
                String description = promptDialogUI.getDescriptionField().getText().trim();
                String content = promptDialogUI.getContentArea().getText().trim();
                if (!description.isEmpty() && !content.isEmpty()) {
                    customPromptsTableModel.addRow(new Object[]{description, content});
                }
            }
        });
    }

    private void removeCustomPrompt() {
        ApplicationManager.getApplication().invokeLater(() -> {
            int selectedRow = customPromptsTable.getSelectedRow();
            if (selectedRow != -1) {
                int result = Messages.showYesNoDialog(
                    "Are you sure you want to delete this custom prompt?",
                    "Confirm Deletion",
                    Messages.getQuestionIcon()
                );
                if (result == Messages.YES) {
                    customPromptsTableModel.removeRow(selectedRow);
                }
            }
        });
    }

    private void editCustomPrompt(int row) {
        SwingUtilities.invokeLater(() -> {
            String description = (String) customPromptsTableModel.getValueAt(row, 0);
            String content = (String) customPromptsTableModel.getValueAt(row, 1);

            ApplicationManager.getApplication().invokeAndWait(() -> {
                PromptDialogUIUtil.PromptDialogUI promptDialogUI = PromptDialogUIUtil.showPromptDialog(false, description,
                        content);

                UIManager.put("OptionPane.okButtonText", "OK");
                UIManager.put("OptionPane.cancelButtonText", "Cancel");
                
                int result = JOptionPane.showConfirmDialog(mainPanel, promptDialogUI.getPanel(), "Update Your Prompt",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (result == JOptionPane.OK_OPTION) {
                    String newDescription = promptDialogUI.getDescriptionField().getText().trim();
                    String newContent = promptDialogUI.getContentArea().getText().trim();
                    if (!newDescription.isEmpty() && !newContent.isEmpty()) {
                        customPromptsTableModel.setValueAt(newDescription, row, 0);
                        customPromptsTableModel.setValueAt(newContent, row, 1);
                    }
                }
            }, ModalityState.defaultModalityState());
        });
    }

    private void setupListeners() {
        clientComboBox.addActionListener(e -> {
            String selectedClient = (String) clientComboBox.getSelectedItem();
            updateModuleComboBox(selectedClient);
        });

        configButton.addActionListener(e -> showModuleConfigDialog());
    }

    private void updateModuleComboBox(String selectedClient) {
        moduleComboBox.removeAllItems();
        String[] modules = Constants.CLIENT_MODULES.get(selectedClient);
        if (modules != null) {
            for (String module : modules) {
                moduleComboBox.addItem(module);
            }
        }
    }

    private void showModuleConfigDialog() {
        String selectedClient = (String) clientComboBox.getSelectedItem();
        String selectedModule = (String) moduleComboBox.getSelectedItem();

        ModuleConfigDialog dialog = new ModuleConfigDialog(
                mainPanel,
                selectedClient,
                selectedModule);
        dialog.show();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public ComboBox<String> getLanguageComboBox() {
        return languageComboBox;
    }

    public JBTable getCustomPromptsTable() {
        return customPromptsTable;
    }

    public DefaultTableModel getCustomPromptsTableModel() {
        return customPromptsTableModel;
    }

    public int getSELECTED_ROW() {
        return SELECTED_ROW;
    }

    public ComboBox<String> getPromptTypeComboBox() {
        return promptTypeComboBox;
    }

    public void setPromptTypeComboBox(ComboBox<String> promptTypeComboBox) {
        this.promptTypeComboBox = promptTypeComboBox;
    }

    public ComboBox<String> getModuleComboBox() {
        return moduleComboBox;
    }

    public void setModuleComboBox(ComboBox<String> moduleComboBox) {
        this.moduleComboBox = moduleComboBox;
    }

    public JComboBox<String> getClientComboBox() {
        return clientComboBox;
    }

    private void updateStreamStatus(JLabel streamLabel, String selectedClient) {
        AIService aiService = CommitMessageService.getAIService(selectedClient);
        boolean supportsStream = aiService.generateByStream();
        streamLabel.setText(supportsStream ? "(Supports Stream)" : "(No Stream)");
        streamLabel.setForeground(supportsStream ? new JBColor(new Color(0, 128, 0), new Color(0, 128, 0))
                : JBColor.GRAY);
    }
}
