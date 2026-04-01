package com.hmydk.aigit.config;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.LastPromptService;
import com.hmydk.aigit.util.PromptDialogUIUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.net.HttpProxyConfigurable;
import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

public class ApiKeyConfigurableUI {

    private JPanel mainPanel;
    private JBTabbedPane tabbedPane;

    // 基本设置标签页组件
    private ComboBox<String> clientComboBox;
    private ComboBox<String> moduleComboBox;
    private ComboBox<String> languageComboBox;
    private JBCheckBox useSystemProxyCheckBox;
    private JButton configButton;
    private JPanel clientPanel;

    // 提示词设置标签页组件
    private ComboBox<String> promptTypeComboBox;
    private JBTable customPromptsTable;
    private DefaultTableModel customPromptsTableModel;
    private JPanel customPromptPanel;
    private JPanel projectPromptPanel;

    // 文件忽略功能相关组件
    private JBCheckBox enableFileExclusionCheckBox;
    private JBTextArea excludePatternsTextArea;
    private JButton resetToDefaultButton;
    private JPanel fileExclusionPanel;

    // Recent Prompt 标签页组件
    private JBTextArea recentPromptTextArea;
    private JButton copyPromptButton;
    private JButton refreshPromptButton;
    private JBLabel promptStatusLabel;

    // 记录当前选中的行
    private int SELECTED_ROW = 0;

    public ApiKeyConfigurableUI() {
        initComponents();
        layoutComponents();
        setupListeners();
    }

    private void initComponents() {
        // Initialize tabbed pane
        tabbedPane = new JBTabbedPane();

        // Initialize basic settings components
        clientPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        clientComboBox = new ComboBox<>(Constants.LLM_CLIENTS);
        clientComboBox.setSelectedItem("OpenAI");
        clientPanel.add(clientComboBox);

        moduleComboBox = new ComboBox<>();
        moduleComboBox.setEditable(true);
        configButton = new JButton(AllIcons.General.Settings);
        configButton.setToolTipText("Configure Module Settings");

        languageComboBox = new ComboBox<>(Constants.languages);
        languageComboBox.setEditable(true);
        languageComboBox.setSelectedItem("English");

        useSystemProxyCheckBox = new JBCheckBox("Use System Proxy (Default Off)");

        // Initialize prompt settings components
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

        // Initialize file exclusion components
        enableFileExclusionCheckBox = new JBCheckBox("Enable File Filtering");
        excludePatternsTextArea = new JBTextArea(8, 50);
        excludePatternsTextArea.setLineWrap(true);
        excludePatternsTextArea.setWrapStyleWord(true);
        excludePatternsTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        resetToDefaultButton = new JButton("Reset to Default");
        resetToDefaultButton.setToolTipText("Reset exclusion patterns to default values");

        fileExclusionPanel = createFileExclusionPanel();

        // Initialize recent prompt components
        recentPromptTextArea = new JBTextArea(15, 60);
        recentPromptTextArea.setEditable(false);
        recentPromptTextArea.setLineWrap(true);
        recentPromptTextArea.setWrapStyleWord(true);
        recentPromptTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        recentPromptTextArea.setBackground(UIManager.getColor("Panel.background"));

        copyPromptButton = new JButton("Copy Prompt");
        copyPromptButton.setToolTipText("Copy the recent prompt to clipboard");

        refreshPromptButton = new JButton("Refresh");
        refreshPromptButton.setToolTipText("Refresh the recent prompt display");

        promptStatusLabel = new JBLabel("No recent prompt available");
        promptStatusLabel.setForeground(JBColor.GRAY);
    }

    private void layoutComponents() {
        // Create main panel
        mainPanel = new JPanel(new BorderLayout());

        // Create tabbed pane
        tabbedPane = new JBTabbedPane();

        // Add tabs
        tabbedPane.addTab("Basic Settings", createBasicSettingsPanel());
        tabbedPane.addTab("Prompt Settings", createPromptSettingsPanel());
        tabbedPane.addTab("File Filtering", createFileFilterPanel());
        tabbedPane.addTab("Recent Prompt", createRecentPromptPanel());

        // Add tabbed pane to main panel
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
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
        panel.setPreferredSize(new Dimension(-1, 200)); // 设置与customPromptPanel相同的高度

        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.setBorder(JBUI.Borders.empty(0, 0, 5, 0));

        JLabel infoLabel = new JLabel(
                "Using project-specific prompt from '" + Constants.PROJECT_PROMPT_FILE_NAME + "' in the project root.");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.PLAIN, 12));
        infoLabel.setForeground(JBColor.GRAY);
        labelPanel.add(infoLabel, BorderLayout.CENTER);

        panel.add(labelPanel, BorderLayout.NORTH);

        // 添加一个空面板来占据剩余空间，保持布局一致性
        JPanel emptyPanel = new JPanel();
        emptyPanel.setBackground(UIManager.getColor("Panel.background"));
        panel.add(emptyPanel, BorderLayout.CENTER);

        return panel;
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
                        Messages.getQuestionIcon());
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
                PromptDialogUIUtil.PromptDialogUI promptDialogUI = PromptDialogUIUtil.showPromptDialog(false,
                        description,
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

        // Recent Prompt tab listeners
        copyPromptButton.addActionListener(e -> copyRecentPromptToClipboard());
        refreshPromptButton.addActionListener(e -> refreshRecentPrompt());

        // Load recent prompt when tab is selected
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 3) { // Recent Prompt tab index
                refreshRecentPrompt();
            }
        });
    }

    private JPanel createBasicSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Add Report Bug link in the top right corner
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel reportBugLabel = new JLabel(
                "<html><a href='https://github.com/HMYDK/AIGitCommit/issues'>Report Bug ↗</a></html>");
        reportBugLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        reportBugLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/HMYDK/AIGitCommit/issues"));
                } catch (Exception ignored) {
                }
            }
        });
        topPanel.add(reportBugLabel, BorderLayout.EAST);

        // Add proxy settings link
        // 使用 ActionLink 替代 JLabel
        ActionLink proxySettingsLink = new ActionLink("Manage HTTP proxy settings (requires restart)", e -> {
            // 触发点击后的动作
            ShowSettingsUtil.getInstance().showSettingsDialog(null, HttpProxyConfigurable.class);
        });

        topPanel.add(proxySettingsLink, BorderLayout.WEST);
        gbc.gridwidth = 2;
        addComponent(panel, topPanel, gbc, 0, 0, 1.0);

        // Reset gridwidth for subsequent components
        gbc.gridwidth = 1;

        addComponent(panel, new JBLabel("LLM client:"), gbc, 0, 1, 0.0);
        addComponent(panel, clientPanel, gbc, 1, 1, 1.0);

        JPanel modulePanel = new JPanel(new BorderLayout(5, 0));
        modulePanel.add(moduleComboBox, BorderLayout.CENTER);
        modulePanel.add(configButton, BorderLayout.EAST);

        // Create module label panel with help icon
        JPanel moduleLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JBLabel moduleLabel = new JBLabel("Module: ");
        JBLabel moduleHelpIcon = new JBLabel(AllIcons.General.ContextHelp);
        moduleHelpIcon.setToolTipText("You can input custom module name(this is a editable comboBox)");
        moduleLabelPanel.add(moduleLabel);
        moduleLabelPanel.add(moduleHelpIcon);

        addComponent(panel, moduleLabelPanel, gbc, 0, 2, 0.0);
        addComponent(panel, modulePanel, gbc, 1, 2, 1.0);

        JPanel languagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JBLabel languageLabel = new JBLabel("Language: ");
        JBLabel helpIcon = new JBLabel(AllIcons.General.ContextHelp);
        helpIcon.setToolTipText(
                "The language of the generated commit message. Note that the actual output language depends on the LLM model's language capabilities.");
        languagePanel.add(languageLabel);
        languagePanel.add(helpIcon);

        addComponent(panel, languagePanel, gbc, 0, 3, 0.0);
        addComponent(panel, languageComboBox, gbc, 1, 3, 1.0);

        addComponent(panel, new JBLabel("Network: "), gbc, 0, 4, 0.0);
        addComponent(panel, useSystemProxyCheckBox, gbc, 1, 4, 1.0);

        // 添加一个空的面板来填充剩余空间
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        addComponent(panel, new JPanel(), gbc, 0, 5, 1.0);

        return panel;
    }

    private JPanel createPromptSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addComponent(panel, new JBLabel("Prompt type:"), gbc, 0, 0, 0.0);
        addComponent(panel, promptTypeComboBox, gbc, 1, 0, 1.0);

        // Create a panel to maintain consistent height
        JPanel contentPanel = new JPanel(new CardLayout());
        contentPanel.setPreferredSize(new Dimension(-1, 300));
        contentPanel.add(customPromptPanel, "CUSTOM_PROMPT");
        contentPanel.add(projectPromptPanel, "PROJECT_PROMPT");

        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        addComponent(panel, contentPanel, gbc, 0, 1, 1.0);

        promptTypeComboBox.addActionListener(e -> {
            CardLayout cardLayout = (CardLayout) contentPanel.getLayout();
            if (Constants.CUSTOM_PROMPT.equals(promptTypeComboBox.getSelectedItem())) {
                cardLayout.show(contentPanel, "CUSTOM_PROMPT");
            } else {
                cardLayout.show(contentPanel, "PROJECT_PROMPT");
            }
        });

        return panel;
    }

    private JPanel createFileFilterPanel() {
        return createFileExclusionPanel();
    }

    private void addComponent(JPanel parent, Component component, GridBagConstraints gbc, int gridx, int gridy,
                              double weightx) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.weightx = weightx;
        parent.add(component, gbc);
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

    public JBCheckBox getUseSystemProxyCheckBox() {
        return useSystemProxyCheckBox;
    }

    // 文件忽略功能的getter方法
    public JBCheckBox getEnableFileExclusionCheckBox() {
        return enableFileExclusionCheckBox;
    }

    public JBTextArea getExcludePatternsTextArea() {
        return excludePatternsTextArea;
    }

    private JPanel createFileExclusionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.empty(10, 0, 5, 0),
                JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)));

        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        JBLabel titleLabel = new JBLabel("File Filtering Settings");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14));
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(enableFileExclusionCheckBox, BorderLayout.EAST);
        panel.add(titlePanel, BorderLayout.NORTH);

        // 内容面板
        JPanel contentPanel = new JPanel(new BorderLayout(0, 5));
        contentPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));

        // 说明文本
        JBLabel helpLabel = new JBLabel(Constants.EXCLUDE_PATTERNS_HELP_TEXT);
        helpLabel.setFont(helpLabel.getFont().deriveFont(Font.PLAIN, 11));
        helpLabel.setForeground(JBColor.GRAY);
        contentPanel.add(helpLabel, BorderLayout.NORTH);

        // 文本区域面板
        JPanel textAreaPanel = new JPanel(new BorderLayout(0, 5));
        JBScrollPane scrollPane = new JBScrollPane(excludePatternsTextArea);
        scrollPane.setPreferredSize(new Dimension(-1, 120));
        textAreaPanel.add(scrollPane, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.add(resetToDefaultButton);
        textAreaPanel.add(buttonPanel, BorderLayout.SOUTH);

        contentPanel.add(textAreaPanel, BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加事件监听器
        enableFileExclusionCheckBox.addActionListener(e -> {
            boolean enabled = enableFileExclusionCheckBox.isSelected();
            excludePatternsTextArea.setEnabled(enabled);
            resetToDefaultButton.setEnabled(enabled);
        });

        resetToDefaultButton.addActionListener(e -> {
            excludePatternsTextArea.setText(Constants.DEFAULT_EXCLUDE_PATTERNS_TEXT);
        });

        return panel;
    }

    private JPanel createRecentPromptPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));

        // Header panel with title and buttons
        JPanel headerPanel = new JPanel(new BorderLayout());

        JBLabel titleLabel = new JBLabel("Most Recent AI Prompt");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(refreshPromptButton);
        buttonPanel.add(copyPromptButton);
        headerPanel.add(buttonPanel, BorderLayout.EAST);

        // Status label
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        statusPanel.add(promptStatusLabel);

        // Create a combined north panel
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(headerPanel, BorderLayout.NORTH);
        northPanel.add(statusPanel, BorderLayout.SOUTH);

        panel.add(northPanel, BorderLayout.NORTH);

        // Text area with scroll pane
        JBScrollPane scrollPane = new JBScrollPane(recentPromptTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.GRAY, 1),
                JBUI.Borders.empty(5)));

        panel.add(scrollPane, BorderLayout.CENTER);

        // Info panel at bottom
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        JBLabel infoLabel = new JBLabel("This shows the most recent prompt sent to the AI for the current project.");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.PLAIN, 11));
        infoLabel.setForeground(JBColor.GRAY);
        infoPanel.add(infoLabel);
        panel.add(infoPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshRecentPrompt() {
        Project currentProject = getCurrentProject();
        if (currentProject != null) {
            String recentPrompt = LastPromptService.getLastPrompt(currentProject);
            if (recentPrompt != null && !recentPrompt.trim().isEmpty()) {
                recentPromptTextArea.setText(recentPrompt);
                promptStatusLabel.setText("Last updated: " + java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                promptStatusLabel.setForeground(JBColor.GRAY);
                copyPromptButton.setEnabled(true);
            } else {
                recentPromptTextArea.setText("");
                promptStatusLabel.setText("No recent prompt available for current project");
                promptStatusLabel.setForeground(JBColor.GRAY);
                copyPromptButton.setEnabled(false);
            }
        } else {
            recentPromptTextArea.setText("");
            promptStatusLabel.setText("No project currently open");
            promptStatusLabel.setForeground(JBColor.GRAY);
            copyPromptButton.setEnabled(false);
        }
        recentPromptTextArea.setCaretPosition(0);
    }

    private void copyRecentPromptToClipboard() {
        String promptText = recentPromptTextArea.getText();
        if (promptText != null && !promptText.trim().isEmpty()) {
            StringSelection selection = new StringSelection(promptText);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);

            // Show a brief confirmation
            promptStatusLabel.setText("Prompt copied to clipboard!");
            promptStatusLabel.setForeground(JBColor.GREEN);

            // Reset status after 2 seconds
            Timer timer = new Timer(2000, e -> {
                refreshRecentPrompt(); // This will reset the status label
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    private Project getCurrentProject() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length > 0) {
            // Return the first open project, or you could implement logic to detect the
            // "current" project
            return openProjects[0];
        }
        return null;
    }

}
