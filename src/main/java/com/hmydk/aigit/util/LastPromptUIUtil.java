package com.hmydk.aigit.util;

import com.hmydk.aigit.service.LastPromptService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;

import javax.swing.*;
import java.awt.*;

/**
 * 最近 Prompt 折叠弹窗展示
 */
public class LastPromptUIUtil {

    public static void showLastPromptPopup(Project project) {
        String prompt = LastPromptService.getLastPrompt(project);
        if (prompt == null || prompt.isEmpty()) return;

        JPanel panel = new JPanel(new BorderLayout());
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JBLabel title = new JBLabel("最近使用的 Prompt");
        title.setForeground(JBColor.GRAY);
        JButton copyBtn = new JButton("复制");
        JButton toggleBtn = new JButton("展开");
        header.add(title);
        header.add(copyBtn);
        header.add(toggleBtn);

        String preview = buildPreview(prompt, 160);
        JBTextArea textArea = new JBTextArea(preview);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(520, 180));
        panel.add(header, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        final boolean[] expanded = {false};

        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(prompt), null);
        });

        toggleBtn.addActionListener(e -> {
            expanded[0] = !expanded[0];
            toggleBtn.setText(expanded[0] ? "收起" : "展开");
            textArea.setText(expanded[0] ? prompt : buildPreview(prompt, 160));
            textArea.setCaretPosition(0);
        });

        Component anchor = WindowManager.getInstance().getFrame(project);

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, panel)
                .setTitle("最近使用的 Prompt")
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .setFocusable(true)
                .setMinSize(new Dimension(560, 220))
                .createPopup();

        if (anchor != null) {
            popup.showInCenterOf(anchor);
        } else {
            popup.showInFocusCenter();
        }
    }

    public static void showLastPromptPopup(Project project, Component anchorComponent) {
        String prompt = LastPromptService.getLastPrompt(project);
        if (prompt == null || prompt.isEmpty()) return;

        JPanel panel = new JPanel(new BorderLayout());
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JBLabel title = new JBLabel("最近使用的 Prompt");
        title.setForeground(JBColor.GRAY);
        JButton copyBtn = new JButton("复制");
        JButton toggleBtn = new JButton("展开");
        header.add(title);
        header.add(copyBtn);
        header.add(toggleBtn);

        String preview = buildPreview(prompt, 160);
        JBTextArea textArea = new JBTextArea(preview);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(520, 180));
        panel.add(header, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        final boolean[] expanded = {false};

        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(prompt), null);
        });

        toggleBtn.addActionListener(e -> {
            expanded[0] = !expanded[0];
            toggleBtn.setText(expanded[0] ? "收起" : "展开");
            textArea.setText(expanded[0] ? prompt : buildPreview(prompt, 160));
            textArea.setCaretPosition(0);
        });

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, panel)
                .setTitle("最近使用的 Prompt")
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .setFocusable(true)
                .setMinSize(new Dimension(560, 220))
                .createPopup();

        if (anchorComponent != null) {
            popup.showUnderneathOf(anchorComponent);
        } else {
            Component frame = WindowManager.getInstance().getFrame(project);
            if (frame != null) {
                popup.showInCenterOf(frame);
            } else {
                popup.showInFocusCenter();
            }
        }
    }

    private static String buildPreview(String text, int max) {
        if (text.length() <= max) return text;
        return text.substring(0, max) + " ...";
    }
}