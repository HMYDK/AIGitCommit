package com.hmydk.aigit.config;

import com.intellij.openapi.ui.DialogWrapper;
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

    public ModuleConfigDialog(Component parent, String client, String module) {
        super(parent, true);
        this.client = client;
        this.module = module;
        setTitle(client + " : " + module);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        urlField = new JTextField();
        apiKeyField = new JBPasswordField();

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("URL:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(urlField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("API Key:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(apiKeyField, gbc);

        return panel;
    }

    /**
     * 弹窗每次打开时，都会调用这个初始化方法
     */
    @Override
    protected void init() {
        //先让上层组件初始化，以便获取到默认值
        super.init();

        // 获取当前配置实例
        ApiKeySettings settings = ApiKeySettings.getInstance();
        // 获取或创建模块配置
        String configKey = client;
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs()
                .computeIfAbsent(configKey, k -> new ApiKeySettings.ModuleConfig());
        urlField.setText(moduleConfig.getUrl());
        apiKeyField.setText(moduleConfig.getApiKey());
    }

    @Override
    protected void doOKAction() {
        // 获取当前配置实例
        ApiKeySettings settings = ApiKeySettings.getInstance();

        // 获取或创建模块配置
        String configKey = client;
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs()
                .computeIfAbsent(configKey, k -> new ApiKeySettings.ModuleConfig());

        // 保存配置
        moduleConfig.setUrl(urlField.getText());
        moduleConfig.setApiKey(new String(apiKeyField.getPassword()));

        // 调用父类方法关闭对话框
        super.doOKAction();
    }


}
