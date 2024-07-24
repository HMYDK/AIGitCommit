package com.hmydk.aigit;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.Key;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WelcomeNotification implements ProjectActivity {

    private static final String NOTIFICATION_GROUP_ID = "AI Git Commit Notifications";
    private static final String PLUGIN_NAME = "AI Git Commit";
    private static final String WELCOME_TITLE = "Welcome to AI Git Commit!";
    private static final String WELCOME_CONTENT = "Thank you for installing AI Git Commit. " +
            "To get started, please configure your API key in the settings.";
    private static final Key<String> PLUGIN_VERSION_KEY = Key.create("com.hmydk.aigit.version");

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        if (isNewOrUpdatedVersion(project)) {
            showWelcomeNotification(project);
            updateStoredVersion(project);
        }
        return Unit.INSTANCE;
    }

    private boolean isNewOrUpdatedVersion(@NotNull Project project) {
        String storedVersion = project.getUserData(PLUGIN_VERSION_KEY);
        String currentVersion = getCurrentPluginVersion();
        return storedVersion == null || !storedVersion.equals(currentVersion);
    }

    private void updateStoredVersion(@NotNull Project project) {
        project.putUserData(PLUGIN_VERSION_KEY, getCurrentPluginVersion());
    }

    private String getCurrentPluginVersion() {
        return PluginManagerCore.getPlugin(PluginId.getId("com.hmydk.aigit")).getVersion();
    }

    private void showWelcomeNotification(@NotNull Project project) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(WELCOME_TITLE, WELCOME_CONTENT, NotificationType.INFORMATION)
                .setIcon(null)  // You can set a custom icon here if you have one
                .addAction(new ConfigureAction())
                .notify(project);
    }

    private static class ConfigureAction extends com.intellij.openapi.actionSystem.AnAction {
        ConfigureAction() {
            super("Configure");
        }

        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), PLUGIN_NAME);
        }
    }
}