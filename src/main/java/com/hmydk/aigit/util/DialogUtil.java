package com.hmydk.aigit.util;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextPane;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.icons.AllIcons;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

public class DialogUtil {

    public static final String CONFIGURATION_ERROR_TITLE = "Configuration Error";
    public static final String GENERATE_COMMIT_MESSAGE_ERROR_TITLE = "Generate Commit Message Error";

    public static void showErrorDialog(Component parentComponent, String errorDetails, String title) {
        new ErrorDialog(parentComponent, errorDetails, title).show();
    }

    private static class ErrorDialog extends DialogWrapper {
        private final String errorDetails;
        private final String title;

        protected ErrorDialog(Component parent, String errorDetails, String title) {
            super(parent, true);
            this.errorDetails = errorDetails;
            this.title = title;
            setTitle(title);
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JBTabbedPane tabbedPane = new JBTabbedPane();

            // 问题概述选项卡
            JBPanel<?> overviewPanel = new JBPanel<>(new BorderLayout());
            overviewPanel.setBorder(JBUI.Borders.empty(12));
            
            // 添加图标和标题
            JBPanel<?> headerPanel = new JBPanel<>(new BorderLayout());
            headerPanel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
            
            JBLabel iconLabel = new JBLabel(AllIcons.General.Error);
            iconLabel.setBorder(JBUI.Borders.empty(0, 0, 0, 10));
            
            JBLabel titleLabel = new JBLabel(title);
            titleLabel.setFont(JBUI.Fonts.label().biggerOn(2).asBold());
            
            headerPanel.add(iconLabel, BorderLayout.WEST);
            headerPanel.add(titleLabel, BorderLayout.CENTER);
            
            // 添加错误提示信息
            JBPanel<?> messagePanel = new JBPanel<>(new BorderLayout());
            messagePanel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
            
            JTextPane messagePane = new JTextPane();
            messagePane.setContentType("text/html");
            messagePane.setText(
                    "<html><body style='font-family: " + UIUtil.getLabelFont().getFamily() + "; font-size: " +
                    UIUtil.getLabelFont().getSize() + "pt;'>" +
                    "Possible causes:<br><br>" +
                    "• API Key or URL configuration error<br>" +
                    "• Network connection issues(check your proxy)<br>" +
                    "• The model may be temporarily unstable, please try again later<br><br>" +
                    "Please check your configuration and try again." +
                    "</body></html>");
            messagePane.setEditable(false);
            messagePane.setBackground(UIUtil.getPanelBackground());
            messagePane.setBorder(null);
            
            messagePanel.add(messagePane, BorderLayout.CENTER);
            
            // 组装概述面板
            overviewPanel.add(headerPanel, BorderLayout.NORTH);
            overviewPanel.add(messagePanel, BorderLayout.CENTER);

            // 错误详情选项卡
            JBPanel<?> detailsPanel = new JBPanel<>(new BorderLayout());
            detailsPanel.setBorder(JBUI.Borders.empty(12));
            
            JBLabel detailsLabel = new JBLabel("Error details from LLM:");
            detailsLabel.setBorder(JBUI.Borders.empty(0, 0, 6, 0));
            
            String formattedJson = formatJson(errorDetails);
            EditorTextField errorTextField = new EditorTextField(formattedJson, null, JsonFileType.INSTANCE) {
                @Override
                protected @NotNull EditorEx createEditor() {
                    EditorEx editor = super.createEditor();
                    editor.setBackgroundColor(UIUtil.getPanelBackground());
                    return editor;
                }
            };
            errorTextField.setOneLineMode(false);
            errorTextField.setViewer(true);
            errorTextField.setPreferredSize(JBUI.size(500, 250));
            errorTextField.addSettingsProvider(editor -> {
                editor.setVerticalScrollbarVisible(true);
                editor.setHorizontalScrollbarVisible(true);
                editor.getSettings().setUseSoftWraps(true);
                editor.getSettings().setLineNumbersShown(true);
                editor.getSettings().setFoldingOutlineShown(true);
            });
            
            JBScrollPane scrollPane = new JBScrollPane(errorTextField);
            scrollPane.setBorder(JBUI.Borders.empty());
            
            detailsPanel.add(detailsLabel, BorderLayout.NORTH);
            detailsPanel.add(scrollPane, BorderLayout.CENTER);

            // 添加选项卡
            tabbedPane.addTab("Overview", AllIcons.Actions.Help, overviewPanel);
            tabbedPane.addTab("Error Details", AllIcons.Debugger.Console, detailsPanel);

            return JBUI.Panels.simplePanel()
                    .withPreferredSize(500, 300)
                    .addToCenter(tabbedPane);
        }

        @Override
        protected Action @NotNull [] createActions() {
            return new Action[]{getOKAction()};
        }
        
        @Override
        protected void createDefaultActions() {
            super.createDefaultActions();
            getOKAction().putValue(Action.NAME, "Close");
        }
    }

    private static String formatJson(String jsonStr) {
        try {
            if (jsonStr == null || jsonStr.isEmpty()) {
                return "No error details available";
            }
            
            com.google.gson.JsonElement jsonElement = com.google.gson.JsonParser.parseString(jsonStr);
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            return gson.toJson(jsonElement);
        } catch (Exception e) {
            return jsonStr;
        }
    }
}
