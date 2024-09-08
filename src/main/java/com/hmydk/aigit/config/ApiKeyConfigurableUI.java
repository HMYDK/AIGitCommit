package com.hmydk.aigit.config;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.util.PromptDialogUIUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class ApiKeyConfigurableUI {

    private JPanel mainPanel;
    private JBTextField apiKeyField;
    private ComboBox<String> modelComboBox;
    private ComboBox<String> languageComboBox;
    private JBTable customPromptsTable;
    private DefaultTableModel customPromptsTableModel;

    // 记录当前选中的行
    private int SELECTED_ROW = 0;

    public ApiKeyConfigurableUI() {
        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        apiKeyField = new JBTextField();
        modelComboBox = new ComboBox<>(new String[] { "Gemini" });
        languageComboBox = new ComboBox<>(Constants.languages);
        customPromptsTableModel = new DefaultTableModel(new String[] { "Description", "Prompt" }, 0);
        customPromptsTable = new JBTable(customPromptsTableModel);
    }

    private void layoutComponents() {
        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        mainPanel.add(new JBLabel("LLM client:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        mainPanel.add(modelComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        JPanel apiKeyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        apiKeyPanel.add(new JBLabel("API key:"));
        mainPanel.add(apiKeyPanel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        JPanel apiKeyInputPanel = new JPanel(new BorderLayout(5, 0));
        apiKeyInputPanel.add(apiKeyField, BorderLayout.CENTER);
        JLabel linkLabel = getjLabel();
        apiKeyInputPanel.add(linkLabel, BorderLayout.EAST);
        mainPanel.add(apiKeyInputPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        mainPanel.add(new JBLabel("Language:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        mainPanel.add(languageComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weighty = 0.0; // 设置权重为0以适应新行
        JBLabel jbLabel = new JBLabel("Click on the data in the table to use it as the prompt.");
        jbLabel.setFont(jbLabel.getFont().deriveFont(Font.PLAIN, 12));
        jbLabel.setForeground(JBColor.GRAY);
        mainPanel.add(jbLabel, gbc); // 新增文本行

        gbc.gridy = 4; // 表格位置下移一行
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel customPromptsPanel = ToolbarDecorator.createDecorator(customPromptsTable)
                .setAddAction(button -> {
                    // 添加自定义提示的逻辑
                    PromptDialogUIUtil.PromptDialogUI promptDialogUI = PromptDialogUIUtil.showPromptDialog(true, null,
                            null);

                    JOptionPane optionPane = new JOptionPane(promptDialogUI.getPanel(), JOptionPane.PLAIN_MESSAGE,
                            JOptionPane.OK_CANCEL_OPTION);
                    JDialog dialog = optionPane.createDialog(mainPanel, "add prompt");
                    // dialog.setSize(600, 400); // 设置对话框大小
                    dialog.setVisible(true);

                    int result = (Integer) optionPane.getValue();
                    if (result == JOptionPane.OK_OPTION) {
                        // 将新提示添加到表格中
                        String description = promptDialogUI.getDescriptionField().getText().trim();
                        String content = promptDialogUI.getContentArea().getText().trim();
                        if (!description.isEmpty() && !content.isEmpty()) {
                            customPromptsTableModel.addRow(new Object[] {
                                    description,
                                    content
                            });
                        }
                    }
                })
                .setRemoveAction(button -> {
                    int selectedRow = customPromptsTable.getSelectedRow();
                    if (selectedRow != -1) {
                        customPromptsTableModel.removeRow(selectedRow);
                    }
                })
                .setEditAction(button -> {
                    editCustomPrompt(customPromptsTable.getSelectedRow());
                })
                .createPanel();
        mainPanel.add(customPromptsPanel, gbc);

        // 添加表格选择监听器
        customPromptsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = customPromptsTable.getSelectedRow();
                SELECTED_ROW = Math.max(selectedRow, 0);
            }
        });
    }

    private static @NotNull JLabel getjLabel() {
        JLabel linkLabel = new JLabel("<html><a href=''>Get Gemini Api Key</a></html>");
        linkLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        linkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://aistudio.google.com/app/apikey"));
                } catch (IOException | URISyntaxException ex) {
                    ex.printStackTrace();
                }
            }
        });
        return linkLabel;
    }

    private void editCustomPrompt(int row) {
        String description = (String) customPromptsTableModel.getValueAt(row, 0);
        String content = (String) customPromptsTableModel.getValueAt(row, 1);

        PromptDialogUIUtil.PromptDialogUI promptDialogUI = PromptDialogUIUtil.showPromptDialog(false, description,
                content);

        int result = JOptionPane.showConfirmDialog(mainPanel, promptDialogUI.getPanel(), "update your prompt",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newDescription = promptDialogUI.getDescriptionField().getText().trim();
            String newContent = promptDialogUI.getContentArea().getText().trim();
            if (!newDescription.isEmpty() && !newContent.isEmpty()) {
                customPromptsTableModel.setValueAt(newDescription, row, 0);
                customPromptsTableModel.setValueAt(newContent, row, 1);
            }
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public JBTextField getApiKeyField() {
        return apiKeyField;
    }

    public ComboBox<String> getModelComboBox() {
        return modelComboBox;
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

    public String getSelectedPrompt() {
        int selectedRow = customPromptsTable.getSelectedRow();
        if (selectedRow != -1) {
            return (String) customPromptsTableModel.getValueAt(selectedRow, 1);
        }
        return null;
    }

    public int getSELECTED_ROW() {
        return SELECTED_ROW;
    }
}
