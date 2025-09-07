package com.hmydk.aigit;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * WelcomeNotification 类实现了 ProjectActivity 接口，
 * 用于在项目启动时显示欢迎通知。
 *
 * <p>此通知仅在插件首次安装或版本更新后显示一次。</p>
 */
public class WelcomeNotification implements ProjectActivity {

    private static final String NOTIFICATION_GROUP_ID = "AI Git Commit Notifications";
    private static final String PLUGIN_NAME = "AI Git Commit";
    private static final String WELCOME_TITLE_INSTALL = "Welcome to AI Git Commit!";
    private static final String WELCOME_CONTENT_INSTALL = "Thank you for installing AI Git Commit. " +
            "To get started, please configure the plugin in the settings.";
    private static final String WELCOME_TITLE_UPDATE = "AI Git Commit Updated!";
    private static final String WELCOME_CONTENT_UPDATE = "AI Git Commit has been updated to a new version. " +
            "Check out the latest features and improvements in the settings.";
    private static final String PLUGIN_VERSION_PROPERTY = "com.hmydk.aigit.version";

    /**
     * 当项目启动时，IntelliJ Platform 会调用此方法。
     *
     * <p>此方法检查插件是否是新安装或已更新。如果是，则显示欢迎通知并更新存储的版本号。
     * 这是 {@link ProjectActivity} 接口的实现，用于执行后台启动任务。</p>
     *
     * @param project 当前项目
     * @param continuation 用于协程的延续（在此未使用）
     * @return Unit.INSTANCE 表示操作完成
     */
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        if (isFirstInstall()) {
            showWelcomeNotification(project, true);
            updateStoredVersion();
        } else if (isUpdatedVersion()) {
            showWelcomeNotification(project, false);
            updateStoredVersion();
        }
        return Unit.INSTANCE;
    }

    /**
     * 检查插件是否是首次安装。
     *
     * <p>此方法通过检查存储在 {@link PropertiesComponent} 中的版本是否为空来确定。
     * 如果存储的版本为空，则表示首次安装。</p>
     *
     * @return 如果是首次安装，则返回 true；否则返回 false
     */
    private boolean isFirstInstall() {
        String storedVersion = PropertiesComponent.getInstance().getValue(PLUGIN_VERSION_PROPERTY);
        return storedVersion == null;
    }

    /**
     * 检查插件是否已更新版本。
     *
     * <p>此方法通过比较当前插件版本和存储在 {@link PropertiesComponent} 中的版本来确定。
     * 如果存储的版本不为空且与当前版本不同，则表示插件已更新。</p>
     *
     * @return 如果是更新版本，则返回 true；否则返回 false
     */
    private boolean isUpdatedVersion() {
        String storedVersion = PropertiesComponent.getInstance().getValue(PLUGIN_VERSION_PROPERTY);
        String currentVersion = getCurrentPluginVersion();
        return storedVersion != null && !storedVersion.equals(currentVersion);
    }

    /**
     * 更新存储的插件版本号。
     *
     * <p>此方法将当前插件的版本号保存到 {@link PropertiesComponent} 中，
     * 以便在下次启动时可以进行比较。</p>
     */
    private void updateStoredVersion() {
        PropertiesComponent.getInstance().setValue(PLUGIN_VERSION_PROPERTY, getCurrentPluginVersion());
    }

    /**
     * 获取当前插件的版本号。
     *
     * <p>此方法从插件的描述符（plugin.xml）中检索版本号。</p>
     *
     * @return 当前插件的版本字符串
     */
    private String getCurrentPluginVersion() {
        return PluginManagerCore.getPlugin(PluginId.getId("com.hmydk.aigit")).getVersion();
    }

    /**
     * 显示欢迎通知。
     *
     * <p>此方法创建一个信息类型的通知，其中包含欢迎消息和一个"配置"操作，
     * 用户可以点击该操作直接跳转到插件的设置页面。根据是否为首次安装显示不同的消息。</p>
     *
     * @param project 显示通知的当前项目
     * @param isFirstInstall 是否为首次安装
     */
    private void showWelcomeNotification(@NotNull Project project, boolean isFirstInstall) {
        String title = isFirstInstall ? WELCOME_TITLE_INSTALL : WELCOME_TITLE_UPDATE;
        String content = isFirstInstall ? WELCOME_CONTENT_INSTALL : WELCOME_CONTENT_UPDATE;
        
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(title, content, NotificationType.INFORMATION)
                .setIcon(null) // You can set a custom icon here if you have one
                .addAction(new ConfigureAction())
                .notify(project);
    }

    /**
     * ConfigureAction 是一个内部类，表示通知中的“配置”操作。
     *
     * <p>当用户点击此操作时，会打开 IntelliJ IDEA 的设置对话框，并直接导航到
     * “AI Git Commit” 插件的配置页面。</p>
     */
    private static class ConfigureAction extends com.intellij.openapi.actionSystem.AnAction {
        /**
         * 构造函数，设置操作的显示文本。
         */
        ConfigureAction() {
            super("Configure");
        }

        /**
         * 当用户点击“配置”按钮时调用此方法。
         *
         * <p>此方法会打开设置对话框，并显示与 {@link WelcomeNotification#PLUGIN_NAME}
         * 关联的配置页面。</p>
         *
         * @param e 包含有关当前上下文信息的事件对象
         */
        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), PLUGIN_NAME);
        }
    }
}
