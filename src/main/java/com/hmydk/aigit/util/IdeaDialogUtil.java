package com.hmydk.aigit.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

/**
 * IdeaDialogUtil
 *
 * @author hmydk
 */
public class IdeaDialogUtil {

    public static void showWarning(Project project, String message, String title) {
        ApplicationManager.getApplication().invokeLater(() -> Messages.showWarningDialog(project, message, title));
    }

    public static void showError(Project project, String message, String title) {
        ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(project, message, title));
    }

    public static void handleModuleNecessaryConfigIsWrong(Project project) {
        Messages.showWarningDialog(project, "Please check the necessary configuration.",
                "Necessary Configuration Error");
    }

    public static void handleNoChangesSelected(Project project) {
        Messages.showWarningDialog(project, "No changes selected. Please select files to commit.",
                "No Changes Selected");
    }

    public static void handleGenerationError(Project project, String errorMessage) {
        IdeaDialogUtil.showError(project, "Error generating commit message: " + errorMessage, "Error");
    }

}
