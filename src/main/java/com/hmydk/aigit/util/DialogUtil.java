package com.hmydk.aigit.util;

import com.intellij.icons.AllIcons;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class DialogUtil {

    public static final String CONFIGURATION_ERROR_TITLE = "Configuration Error";
    public static final String GENERATE_COMMIT_MESSAGE_ERROR_TITLE = "Generate Commit Message Error";

    public static void showErrorDialog(Component parentComponent, String errorDetails, String title) {
        new ConfigurationErrorDialog(parentComponent, errorDetails,title).show();
    }

    private static class ConfigurationErrorDialog extends DialogWrapper {
        private final String errorDetails;

        protected ConfigurationErrorDialog(Component parent, String errorDetails,String title) {
            super(parent, true);
            this.errorDetails = errorDetails;
            setTitle(title);
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JBTabbedPane tabbedPane = new JBTabbedPane();

            // General Tab
            JPanel generalTab = JBUI.Panels.simplePanel()
                    .withBorder(JBUI.Borders.empty(10));
            
            JEditorPane generalMessage = new JEditorPane("text/html", 
                    "<html><body style='margin: 10px;'>" +
                    "<div style='font-family: " + UIUtil.getLabelFont().getFamily() + "; font-size: " +
                    UIUtil.getLabelFont().getSize() + "pt;'>" +
                    "<b style='color: " + ColorUtil.toHex(JBColor.RED) + ";'>" +
                    "• Please check your API Key and URL<br>" +
                    "• Please check your network connection<br>" +
                    "• Some models may be unstable, you can try multiple times" +
                    "</div></body></html>");
            generalMessage.setEditable(false);
            generalMessage.setBackground(UIUtil.getPanelBackground());
            generalMessage.setBorder(null);
            generalTab.add(generalMessage);

            // Error Details Tab
            JPanel detailsTab = JBUI.Panels.simplePanel()
                    .withBorder(JBUI.Borders.empty(10));
            
            String formattedJson = formatJson(errorDetails);
            EditorTextField errorTextField = new EditorTextField(formattedJson,
                    null,
                    JsonFileType.INSTANCE);
            errorTextField.setOneLineMode(false);
            errorTextField.setViewer(true);
            errorTextField.setPreferredSize(JBUI.size(450, 200));
            errorTextField.addSettingsProvider(editor -> {
                editor.setVerticalScrollbarVisible(true);
                editor.setHorizontalScrollbarVisible(true);
                editor.getSettings().setUseSoftWraps(true);
            });
            
            detailsTab.add(errorTextField);

            // 添加选项卡
            tabbedPane.addTab("General", AllIcons.General.Information, generalTab);
            tabbedPane.addTab("Error Details From LLM", AllIcons.General.Error, detailsTab);

            return JBUI.Panels.simplePanel()
                    .withPreferredSize(450, 250)
                    .addToCenter(tabbedPane);
        }

        @Override
        protected Action @NotNull [] createActions() {
            return new Action[]{getOKAction()};
        }
    }

    private static String formatJson(String jsonStr) {
        try {
            if (jsonStr == null || jsonStr.isEmpty()) {
                return "No error details available";
            }
            
            com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
            com.google.gson.JsonElement jsonElement = parser.parse(jsonStr);
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            return gson.toJson(jsonElement);
        } catch (Exception e) {
            return jsonStr;
        }
    }
}
