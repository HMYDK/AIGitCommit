package com.hmydk.aigit.config;

import com.hmydk.aigit.constant.Constants;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.util.ui.JBUI;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ModuleConfigDialog extends DialogWrapper {
    private JTextField urlField;
    private JBPasswordField apiKeyField;
    private final String client;
    private final String module;
    // 文字提示
    private JLabel helpLabel;
    private JButton resetButton; // 新增重置按钮
    private ApiKeySettings.ModuleConfig originalConfig; // 保存原始配置
    private boolean isPasswordVisible = false;

    public ModuleConfigDialog(Component parent, String client, String module) {
        super(parent, true);
        this.client = client;
        this.module = module;
        setTitle(client + " : " + module);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        // 创建主面板
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(450, 200));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5, 10, 5, 10); // 增加左右间距
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 初始化组件
        urlField = new JTextField();
        apiKeyField = new JBPasswordField();
        helpLabel = new JLabel();
        helpLabel.setForeground(JBColor.GRAY);

        // URL 标签和输入框
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("URL:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(urlField, gbc);

        // API Key 标签和输入框
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("API Key:"), gbc);

        // 创建一个面板来容纳密码框和显示按钮
        JPanel apiKeyPanel = new JPanel(new BorderLayout());
        apiKeyPanel.add(apiKeyField, BorderLayout.CENTER);

        // 创建显示/隐藏密码的按钮
        JButton toggleButton = new JButton();
        toggleButton.setIcon(AllIcons.Actions.Show); // 使用IDEA内置图标
        toggleButton.setPreferredSize(new Dimension(28, 28));
        toggleButton.setBorder(BorderFactory.createEmptyBorder());
        toggleButton.setFocusable(false);
        toggleButton.addActionListener(e -> togglePasswordVisibility(apiKeyField, toggleButton));
        apiKeyPanel.add(toggleButton, BorderLayout.EAST);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(apiKeyPanel, gbc);

        // 帮助文本
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = JBUI.insets(15, 10, 5, 10);
        updateHelpText();
        panel.add(helpLabel, gbc);

        return panel;
    }

    private void updateHelpText() {
        helpLabel.setText(Constants.getHelpText(client));
    }

    @Override
    protected Action @NotNull [] createActions() {
        resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetFields());

        return new Action[]{
                getOKAction(),
                getCancelAction(),
                new DialogWrapperAction("Reset") {
                    @Override
                    protected void doAction(ActionEvent e) {
                        resetFields();
                    }
                }
        };
    }

    @Override
    protected void init() {
        super.init();
        ApiKeySettings settings = ApiKeySettings.getInstance();
        String configKey = client;
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs()
                .computeIfAbsent(configKey, k -> new ApiKeySettings.ModuleConfig());
        urlField.setText(moduleConfig.getUrl());
        apiKeyField.setText(moduleConfig.getApiKey());
    }

    @Override
    protected void doOKAction() {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        String configKey = client;
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs()
                .computeIfAbsent(configKey, k -> new ApiKeySettings.ModuleConfig());

        String url = urlField.getText().trim();
        if (StringUtils.isEmpty(url)) {
            Messages.showErrorDialog("URL cannot be empty", "Error");
            return;
        }

        moduleConfig.setApiKey(new String(apiKeyField.getPassword()));
        moduleConfig.setUrl(urlField.getText());

        super.doOKAction();
    }

    private void resetFields() {
        // 重置为默认配置
        ApiKeySettings.ModuleConfig defaultConfig = Constants.moduleConfigs.get(client);
        if (defaultConfig != null) {
            urlField.setText(defaultConfig.getUrl());
            apiKeyField.setText(defaultConfig.getApiKey());
        }
    }

    private void togglePasswordVisibility(JBPasswordField passwordField, JButton toggleButton) {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            // 显示密码
            String password = new String(passwordField.getPassword());
            passwordField.setEchoChar((char) 0); // 设置为不隐藏字符
            toggleButton.setIcon(AllIcons.Actions.ToggleVisibility); // 切换到"隐藏"图标
        } else {
            // 隐藏密码
            passwordField.setEchoChar('•'); // 恢复为密码字符
            toggleButton.setIcon(AllIcons.Actions.Show); // 切换到"显示"图标
        }
        passwordField.revalidate();
        passwordField.repaint();
    }
}