package com.hmydk.aigit.util;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * PromptDialogUIUtil
 *
 * @author hmydk
 */
public class PromptDialogUIUtil {

    public static class PromptDialogUI {
        private JPanel panel;
        private JTextField descriptionField;
        private JTextArea contentArea;

        public JTextArea getContentArea() {
            return contentArea;
        }

        public void setContentArea(JTextArea contentArea) {
            this.contentArea = contentArea;
        }

        public JTextField getDescriptionField() {
            return descriptionField;
        }

        public void setDescriptionField(JTextField descriptionField) {
            this.descriptionField = descriptionField;
        }

        public JPanel getPanel() {
            return panel;
        }

        public void setPanel(JPanel panel) {
            this.panel = panel;
        }
    }

    public static PromptDialogUI showPromptDialog(boolean isAdd, String description, String content) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JTextField descriptionField = new JTextField(isAdd ? "" : description, 30);
        JTextArea contentArea = new JTextArea(isAdd ? "" : content, 15, 70); // 增加行数和列数
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);

        // 添加描述标签和输入框
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Description:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        panel.add(descriptionField, gbc);

        // 添加提示标签和文本区域
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Prompt:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        JBScrollPane scrollPane = new JBScrollPane(contentArea);
        scrollPane.setPreferredSize(new Dimension(800, 600)); // 增加滚动面板的首选大小
        panel.add(scrollPane, gbc);

        // 添加 Inline help text
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel helpTextLabel = new JLabel("<html>Supported placeholders:<br>" +
                "• {diff} - The code changes in diff format.<br>" +
                "• {language} - The language of the generated commit message.<br>"+
                "Note: If you want to output the branch name in the final result, just write your requirements in the prompt.</html>");
        helpTextLabel.setForeground(JBColor.GRAY);
        helpTextLabel.setFont(helpTextLabel.getFont().deriveFont(Font.ITALIC));
        panel.add(helpTextLabel, gbc);

        PromptDialogUI promptDialogUI = new PromptDialogUI();
        promptDialogUI.setPanel(panel);
        promptDialogUI.setDescriptionField(descriptionField);
        promptDialogUI.setContentArea(contentArea);
        return promptDialogUI;
    }
}
