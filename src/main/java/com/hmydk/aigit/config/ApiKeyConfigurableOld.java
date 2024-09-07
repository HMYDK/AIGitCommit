package com.hmydk.aigit.config;

import com.hmydk.aigit.service.CommitMessageService;
import com.hmydk.aigit.util.PromptUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class ApiKeyConfigurableOld implements Configurable {
    
    private final CommitMessageService commitMessageService = new CommitMessageService();
    
    private ComboBox<String> modelComboBox;
    
    private JBPasswordField apiKeyField;
    
    private JCheckBox showPasswordCheckBox;
    
    private ComboBox<String> languageComboBox;
    
    private JButton verifyButton;
    
    private JBTextArea customPromptArea;
    
    private JButton resetPromptButton;
    
    private JButton validatePromptButton;
    
    @Nullable
    @Override
    public JComponent createComponent() {
        modelComboBox = new ComboBox<>(new String[] {"Gemini"});
        apiKeyField = new JBPasswordField();
        showPasswordCheckBox = new JCheckBox("Show Key");
        languageComboBox = new ComboBox<>(
                new String[] {"English", "中文 (Chinese)", "日本語 (Japanese)", "Deutsch (German)",
                        "Français (French)"});
        verifyButton = new JButton("Verify Config");
        customPromptArea = new JBTextArea(5, 30);
        resetPromptButton = new JButton("Reset Prompt");
        validatePromptButton = new JButton("Validate Prompt");
        JLabel hintLabel = createHintLabel();
        
        showPasswordCheckBox.addActionListener(e -> togglePasswordVisibility());
        verifyButton.addActionListener(e -> verifyConfig());
        resetPromptButton.addActionListener(e -> resetPrompt());
        validatePromptButton.addActionListener(e -> validatePrompt());
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        addFormComponent(formPanel, gbc, "LLM Client", createSizedComboBox(modelComboBox));
        addFormComponent(formPanel, gbc, "API Key", createApiKeyPanel());
        addFormComponent(formPanel, gbc, "Language", createSizedComboBox(languageComboBox));
        
        // Add custom prompt area
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        formPanel.add(createCustomPromptPanel(), gbc);
        
        // Add buttons panel
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.0;
        formPanel.add(createButtonsPanel(), gbc);
        
        // Add hint label
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(hintLabel, gbc);
        
        mainPanel.add(new JScrollPane(formPanel), BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    private void addFormComponent(JPanel panel, GridBagConstraints gbc, String labelText, JComponent component) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JBLabel(labelText), gbc);
        gbc.gridx = 1;
        panel.add(component, gbc);
    }
    
    private JPanel createCustomPromptPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Custom Prompt"));
        customPromptArea.setLineWrap(true);
        customPromptArea.setWrapStyleWord(true);
        customPromptArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(new JScrollPane(customPromptArea), BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(resetPromptButton);
        panel.add(validatePromptButton);
        panel.add(verifyButton);
        return panel;
    }
    
    private void resetPrompt() {
        customPromptArea.setText(PromptUtil.DEFAULT_PROMPT);
    }
    
    private void validatePrompt() {
        String prompt = customPromptArea.getText();
        if (prompt.contains("{diff}") && prompt.contains("{local}")) {
            JOptionPane.showMessageDialog(null, "Prompt is valid!", "Validation Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, "Prompt is invalid. It must contain both {diff} and {local}.",
                    "Validation Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JLabel createHintLabel() {
        JLabel label = new JLabel(
                "<html><li><a href=\"https://aistudio.google.com/app/apikey\">Visit AI Studio API Key Page to get <strong>gemini</strong> api key</a></li></html>");
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                BrowserUtil.browse("https://aistudio.google.com/app/apikey");
            }
        });
        return label;
    }
    
    private JPanel createApiKeyPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(apiKeyField, BorderLayout.CENTER);
        panel.add(showPasswordCheckBox, BorderLayout.EAST);
        return panel;
    }
    
    private void togglePasswordVisibility() {
        apiKeyField.setEchoChar(showPasswordCheckBox.isSelected() ? (char) 0 : '•');
    }
    
    private JComboBox<String> createSizedComboBox(JComboBox<String> comboBox) {
        Dimension dimension = new Dimension(50, comboBox.getPreferredSize().height);
        comboBox.setPreferredSize(dimension);
        comboBox.setMaximumSize(dimension);
        comboBox.setMinimumSize(dimension);
        return comboBox;
    }
    
    private void verifyConfig() {
        String model = (String) modelComboBox.getSelectedItem();
        String apiKey = String.valueOf(apiKeyField.getPassword());
        String language = (String) languageComboBox.getSelectedItem();
        
        // 创建一个模态对话框来显示加载消息
        Window parentWindow = SwingUtilities.getWindowAncestor(verifyButton);
        JDialog loadingDialog = new JDialog(parentWindow, "Verifying", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Checking configuration..."), BorderLayout.CENTER);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        panel.add(progressBar, BorderLayout.SOUTH);
        loadingDialog.add(panel);
        loadingDialog.pack();
        loadingDialog.setLocationRelativeTo(parentWindow);
        
        // 在新线程中执行验证，以避免UI冻结
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return commitMessageService.validateConfig(model, apiKey, language);
            }
            
            @Override
            protected void done() {
                loadingDialog.dispose();
                try {
                    boolean isValid = get();
                    if (isValid) {
                        JOptionPane.showMessageDialog(parentWindow, "Configuration is valid!", "Verification Success",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(parentWindow,
                                "Configuration is invalid. Please check your settings.", "Verification Failed",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(parentWindow,
                            "An error occurred during verification: " + e.getMessage(), "Verification Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
        loadingDialog.setVisible(true);
    }
    
    @Override
    public boolean isModified() {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        return !Objects.equals(modelComboBox.getSelectedItem(), settings.getAiModel()) || !String.valueOf(
                apiKeyField.getPassword()).equals(settings.getApiKey()) || !Objects.equals(
                languageComboBox.getSelectedItem(), settings.getCommitLanguage()) || !Objects.equals(
                customPromptArea.getText(), settings.getCustomPrompt());
    }
    
    @Override
    public void apply() throws ConfigurationException {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        settings.setAiModel((String) modelComboBox.getSelectedItem());
        settings.setApiKey(String.valueOf(apiKeyField.getPassword()));
        settings.setCommitLanguage((String) languageComboBox.getSelectedItem());
//        settings.setCustomPrompt(customPromptArea.getText());
    }
    
    @Override
    public void reset() {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        modelComboBox.setSelectedItem(settings.getAiModel());
        apiKeyField.setText(settings.getApiKey());
        showPasswordCheckBox.setSelected(false);
        apiKeyField.setEchoChar('•');
        languageComboBox.setSelectedItem(settings.getCommitLanguage());
//        customPromptArea.setText(settings.getCustomPrompt());
    }
    
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "AIGit Commit";
    }
}