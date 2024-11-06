package com.hmydk.aigit.config;

import com.hmydk.aigit.constant.Constants;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ModuleConfigDialog extends DialogWrapper {
    private JTextField urlField;
    private JBPasswordField apiKeyField;
    private final String client;
    private final String module;
    //文字提示
    private JLabel helpLabel;

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

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(apiKeyField, gbc);

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

    // 保留原有的 init() 和 doOKAction() 方法
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
        moduleConfig.setUrl(urlField.getText());
        moduleConfig.setApiKey(new String(apiKeyField.getPassword()));
        super.doOKAction();
    }
}