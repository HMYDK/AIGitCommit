package com.hmydk.aigit.config;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.util.PromptDialogUIUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
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
    private JPasswordField apiKeyField;
    private ComboBox<String> modelComboBox;
    private ComboBox<String> languageComboBox;

    private ComboBox<String> promptTypeComboBox;
    private JBTable customPromptsTable;
    private DefaultTableModel customPromptsTableModel;
    private JPanel customPromptPanel;
    private JPanel projectPromptPanel;


    // 记录当前选中的行
    private int SELECTED_ROW = 0;

    public ApiKeyConfigurableUI() {
        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        apiKeyField = new JPasswordField();
        modelComboBox = new ComboBox<>(new String[]{"Gemini"});
        languageComboBox = new ComboBox<>(Constants.languages);
        promptTypeComboBox = new ComboBox<>(Constants.getAllPromptTypes());
        customPromptsTableModel = new DefaultTableModel(new String[]{"Description", "Prompt"}, 0);
        customPromptsTable = new JBTable(customPromptsTableModel);
        customPromptPanel = createCustomPromptPanel();
        projectPromptPanel = createProjectPromptPanel();
    }


    private void layoutComponents() {
        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addComponent(new JBLabel("LLM client:"), gbc, 0, 0, 0.0);
        addComponent(modelComboBox, gbc, 1, 0, 1.0);

        JPanel apiKeyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        apiKeyPanel.add(new JBLabel("API key:"));
        addComponent(apiKeyPanel, gbc, 0, 1, 0.0);

        JPanel apiKeyInputPanel = new JPanel(new BorderLayout(5, 0));
        apiKeyInputPanel.add(apiKeyField, BorderLayout.CENTER);
        JLabel linkLabel = getjLabel();
        apiKeyInputPanel.add(linkLabel, BorderLayout.EAST);
        addComponent(apiKeyInputPanel, gbc, 1, 1, 1.0);

        addComponent(new JBLabel("Language:"), gbc, 0, 2, 0.0);
        addComponent(languageComboBox, gbc, 1, 2, 1.0);

        addComponent(new JBLabel("Prompt type:"), gbc, 0, 3, 0.0);
        addComponent(promptTypeComboBox, gbc, 1, 3, 1.0);

        gbc.gridx = 0;
        gbc.gridy = 4;
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
        JBLabel infoLabel = new JBLabel("Click on the data in the table to use it as the prompt.");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.PLAIN, 12));
        infoLabel.setForeground(JBColor.GRAY);
        panel.add(infoLabel, BorderLayout.NORTH);

        JPanel customPromptsPanel = ToolbarDecorator.createDecorator(customPromptsTable)
                .setAddAction(button -> addCustomPrompt())
                .setRemoveAction(button -> removeCustomPrompt())
                .setEditAction(button -> editCustomPrompt(customPromptsTable.getSelectedRow()))
                .createPanel();
        panel.add(customPromptsPanel, BorderLayout.CENTER);

        customPromptsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                SELECTED_ROW = Math.max(customPromptsTable.getSelectedRow(), 0);
            }
        });

        return panel;
    }

    private JPanel createProjectPromptPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel("Using project-specific prompt from '" + Constants.PROJECT_PROMPT_FILE_NAME + "' in the project root.");
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
            gbc.gridy = 4;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            mainPanel.add(customPromptPanel, gbc);
        } else {
            mainPanel.remove(customPromptPanel);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            mainPanel.add(projectPromptPanel, gbc);
        }
        mainPanel.revalidate();
        mainPanel.repaint();
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

    private void addCustomPrompt() {
        PromptDialogUIUtil.PromptDialogUI promptDialogUI = PromptDialogUIUtil.showPromptDialog(true, null, null);

        SwingUtilities.invokeLater(() -> {
            JOptionPane optionPane = new JOptionPane(promptDialogUI.getPanel(), JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
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
        int selectedRow = customPromptsTable.getSelectedRow();
        if (selectedRow != -1) {
            int confirm = JOptionPane.showConfirmDialog(
                    mainPanel,
                    "Are you sure you want to remove this prompt?",
                    "Confirm Removal",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                customPromptsTableModel.removeRow(selectedRow);
                if (customPromptsTableModel.getRowCount() > 0) {
                    int newSelectedRow = Math.min(selectedRow, customPromptsTableModel.getRowCount() - 1);
                    customPromptsTable.setRowSelectionInterval(newSelectedRow, newSelectedRow);
                    SELECTED_ROW = newSelectedRow;
                } else {
                    SELECTED_ROW = -1;
                }
            }
        } else {
            JOptionPane.showMessageDialog(mainPanel, "Please select a prompt to remove.", "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void editCustomPrompt(int row) {
        ApplicationManager.getApplication().invokeLater(() -> {
            String description = (String) customPromptsTableModel.getValueAt(row, 0);
            String content = (String) customPromptsTableModel.getValueAt(row, 1);

            PromptDialogUIUtil.PromptDialogUI promptDialogUI = PromptDialogUIUtil.showPromptDialog(false, description,
                    content);

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
        });
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public JPasswordField getApiKeyField() {
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

    public int getSELECTED_ROW() {
        return SELECTED_ROW;
    }

    public ComboBox<String> getPromptTypeComboBox() {
        return promptTypeComboBox;
    }

    public void setPromptTypeComboBox(ComboBox<String> promptTypeComboBox) {
        this.promptTypeComboBox = promptTypeComboBox;
    }
}
