package com.hmydk.aigit.config;

import com.hmydk.aigit.constant.Constants;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Renders the plugin settings panel off-screen inside the real IntelliJ Platform test application
 * and writes a PNG that CI uploads as a build artifact.
 *
 * <p>The screenshot is produced without a GUI session: Swing components are laid out and painted
 * into a {@link BufferedImage} in headless mode. It gives reviewers a visual check of the settings
 * UI (for example, that a new provider appears in the client/module selectors) next to the test,
 * build, and CI evidence attached to a pull request.</p>
 */
public class SettingsUiScreenshotTest extends BasePlatformTestCase {

    private static final int SCREENSHOT_WIDTH = 760;
    private static final int SCREENSHOT_HEIGHT = 560;

    public void testRendersAnthropicProviderSettingsScreenshot() throws Exception {
        File outputDir = screenshotDirectory();
        AtomicReference<BufferedImage> imageRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                ApiKeyConfigurableUI configurableUI = new ApiKeyConfigurableUI();
                configurableUI.getClientComboBox().setSelectedItem(Constants.Anthropic);
                configurableUI.getLanguageComboBox().setSelectedItem("Chinese");

                JPanel panel = configurableUI.getMainPanel();
                panel.setSize(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT);
                layoutRecursively(panel);
                panel.validate();

                BufferedImage image = new BufferedImage(
                        SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    graphics.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    graphics.setRenderingHint(
                            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    panel.printAll(graphics);
                } finally {
                    graphics.dispose();
                }
                imageRef.set(image);
            } catch (Throwable t) {
                errorRef.set(t);
            }
        });

        if (errorRef.get() != null) {
            throw new AssertionError("Failed to render the settings UI", errorRef.get());
        }

        BufferedImage image = imageRef.get();
        assertNotNull("settings panel was not rendered", image);

        File output = new File(outputDir, "settings-anthropic.png");
        ImageIO.write(image, "png", output);
        assertTrue("screenshot file was not written: " + output.getAbsolutePath(), output.isFile());
        assertTrue("screenshot file is empty: " + output.getAbsolutePath(), output.length() > 0);
    }

    private static void layoutRecursively(java.awt.Container container) {
        container.doLayout();
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof java.awt.Container) {
                layoutRecursively((java.awt.Container) child);
            }
        }
    }

    private static File screenshotDirectory() {
        String configured = System.getProperty("aigit.ui.screenshots.dir");
        File directory = configured == null || configured.isBlank()
                ? new File("build/ui-screenshots")
                : new File(configured);
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IllegalStateException("Cannot create screenshot directory: " + directory);
        }
        return directory;
    }
}
