package com.hmydk.aigit.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class ApiKeyConfigurable implements Configurable {
    private JBPasswordField apiKeyField;
    private JCheckBox showPasswordCheckBox;
    private JLabel placeholderLabel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "AI Git Commit";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insetsBottom(5);
        gbc.weightx = 1.0;

        // 添加提示信息
        JBLabel noticeLabel = new JBLabel("Currently only supports Gemini API", SwingConstants.CENTER);
        noticeLabel.setForeground(new Color(25, 25, 112)); // 深蓝色
        noticeLabel.setFont(noticeLabel.getFont().deriveFont(Font.BOLD));
        mainPanel.add(noticeLabel, gbc);

        gbc.gridy++;
        JLabel label = new JLabel("API Key:");
        mainPanel.add(label, gbc);

        gbc.gridy++;
        apiKeyField = new JBPasswordField();
        apiKeyField.setColumns(30);
        mainPanel.add(apiKeyField, gbc);

        // 添加占位符文本
        placeholderLabel = new JLabel("Enter Gemini API key");
        placeholderLabel.setForeground(JBColor.GRAY);
        apiKeyField.setLayout(new BorderLayout());
        apiKeyField.add(placeholderLabel, BorderLayout.WEST);

        apiKeyField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePlaceholder();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePlaceholder();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePlaceholder();
            }
        });

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        showPasswordCheckBox = new JCheckBox("Show Key");
        showPasswordCheckBox.addActionListener(e -> togglePasswordVisibility());
        mainPanel.add(showPasswordCheckBox, gbc);

        // 添加一个占位组件来推动其他组件到顶部
        gbc.gridy++;
        gbc.weighty = 1.0;
        mainPanel.add(new JPanel(), gbc);

        return mainPanel;
    }

    private void updatePlaceholder() {
        placeholderLabel.setVisible(apiKeyField.getPassword().length == 0);
    }

    private void togglePasswordVisibility() {
        if (showPasswordCheckBox.isSelected()) {
            apiKeyField.setEchoChar((char) 0);
        } else {
            apiKeyField.setEchoChar('•');
        }
    }

    @Override
    public boolean isModified() {
        String storedApiKey = ApiKeySettings.getInstance().getApiKey();
        return !String.valueOf(apiKeyField.getPassword()).equals(storedApiKey);
    }

    @Override
    public void apply() throws ConfigurationException {
        ApiKeySettings.getInstance().setApiKey(String.valueOf(apiKeyField.getPassword()));
    }

    @Override
    public void reset() {
        String storedApiKey = ApiKeySettings.getInstance().getApiKey();
        apiKeyField.setText(storedApiKey);
        showPasswordCheckBox.setSelected(false);
        apiKeyField.setEchoChar('•');
        updatePlaceholder();
    }
}