package com.hmydk.aigit.config;

import com.hmydk.aigit.service.CommitMessageService;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class ApiKeyConfigurable implements Configurable {
    private final CommitMessageService commitMessageService = new CommitMessageService();

    private ComboBox<String> modelComboBox;
    private JBPasswordField apiKeyField;
    private JCheckBox showPasswordCheckBox;
    private ComboBox<String> languageComboBox;
    private JButton verifyButton;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "AI Git Commit";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        modelComboBox = new ComboBox<>(new String[]{"Gemini"});
        apiKeyField = new JBPasswordField();
        showPasswordCheckBox = new JCheckBox("Show Key");
        languageComboBox = new ComboBox<>(new String[]{"English", "中文 (Chinese)", "日本語 (Japanese)", "Deutsch (German)", "Français (French)"});
        verifyButton = new JButton("Verify Config");
        JLabel hintLabel = createHintLabel();

        showPasswordCheckBox.addActionListener(e -> togglePasswordVisibility());
        verifyButton.addActionListener(e -> verifyConfig());

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Add AI model selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JBLabel("LLM Client"), gbc);
        gbc.gridx = 1;
        formPanel.add(createSizedComboBox(modelComboBox), gbc);

        // Add API Key section
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JBLabel("API Key"), gbc);
        gbc.gridx = 1;
        formPanel.add(createApiKeyPanel(), gbc);

        // Add commit message language selection
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JBLabel("Language"), gbc);
        gbc.gridx = 1;
        formPanel.add(createSizedComboBox(languageComboBox), gbc);

        // Add verify button (now in a separate panel)
        JPanel verifyButtonPanel = new JPanel(new BorderLayout());
        verifyButton.setPreferredSize(new Dimension(120, 30)); // Set a preferred size
        verifyButtonPanel.add(verifyButton, BorderLayout.EAST);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        formPanel.add(verifyButtonPanel, gbc);

        // Add hint label
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        formPanel.add(hintLabel, gbc);

        mainPanel.add(formPanel, BorderLayout.NORTH);

        return mainPanel;
    }

    private JLabel createHintLabel() {
        JLabel label = new JLabel("<html><li><a href=\"https://aistudio.google.com/app/apikey\">Visit AI Studio API Key Page to get <strong>gemini</strong> api key</a></li></html>");
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

        boolean isValid = commitMessageService.validateConfig(model, apiKey, language);

        if (isValid) {
            JOptionPane.showMessageDialog(null, "Configuration is valid!", "Verification Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, "Configuration is invalid. Please check your settings.", "Verification Failed", JOptionPane.ERROR_MESSAGE);
        }
    }


    @Override
    public boolean isModified() {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        return !Objects.equals(modelComboBox.getSelectedItem(), settings.getAiModel())
                || !String.valueOf(apiKeyField.getPassword()).equals(settings.getApiKey())
                || !Objects.equals(languageComboBox.getSelectedItem(), settings.getCommitLanguage());
    }

    @Override
    public void apply() throws ConfigurationException {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        settings.setAiModel((String) modelComboBox.getSelectedItem());
        settings.setApiKey(String.valueOf(apiKeyField.getPassword()));
        settings.setCommitLanguage((String) languageComboBox.getSelectedItem());
    }

    @Override
    public void reset() {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        modelComboBox.setSelectedItem(settings.getAiModel());
        apiKeyField.setText(settings.getApiKey());
        showPasswordCheckBox.setSelected(false);
        apiKeyField.setEchoChar('•');
        languageComboBox.setSelectedItem(settings.getCommitLanguage());
    }
}