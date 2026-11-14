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
import java.util.function.Consumer;

/**
 * Renders the plugin settings panel off-screen inside the real IntelliJ Platform test application
 * and writes a PNG that CI uploads as a build artifact.
 *
 * <p>The screenshot is produced without a GUI session: Swing components are laid out and painted
 * into a {@link BufferedImage} in headless mode. It gives reviewers a visual check of the settings
 * UI (for example, that a new provider or request option appears in the settings) next to the test,
 * build, and CI evidence attached to a pull request.</p>
 */
public class SettingsUiScreenshotTest extends BasePlatformTestCase {

    private static final int SCREENSHOT_WIDTH = 760;
    private static final int SCREENSHOT_HEIGHT = 560;

    public void testRendersAnthropicProviderSettingsScreenshot() throws Exception {
        writeScreenshot("settings-anthropic.png", configurableUI -> {
            configurableUI.getClientComboBox().setSelectedItem(Constants.Anthropic);
            configurableUI.getLanguageComboBox().setSelectedItem("Chinese");
            assertFalse("the Anthropic Messages API does not use reasoning_effort",
                    configurableUI.getReasoningEffortComboBox().isEnabled());
        });
    }

    public void testRendersReasoningEffortSettingsScreenshot() throws Exception {
        writeScreenshot("settings-reasoning-effort.png", configurableUI -> {
            configurableUI.getClientComboBox().setSelectedItem(Constants.OpenAI_Compatible);
            configurableUI.getLanguageComboBox().setSelectedItem("English");
            configurableUI.getReasoningEffortComboBox().setSelectedItem("high");
            assertTrue("OpenAI-format providers must expose the reasoning effort control",
                    configurableUI.getReasoningEffortComboBox().isEnabled());
            assertEquals("high", configurableUI.getReasoningEffortComboBox().getSelectedItem());
        });
    }

    public void testRendersRequestLimitsSettingsScreenshot() throws Exception {
        writeScreenshot("settings-request-limits.png", configurableUI -> {
            configurableUI.getClientComboBox().setSelectedItem(Constants.OpenAI_Compatible);
            configurableUI.getLanguageComboBox().setSelectedItem("English");
            configurableUI.getRequestTimeoutSpinner().setValue(120);
            configurableUI.getMaxPromptCharactersSpinner().setValue(40_000);
            assertEquals("the timeout spinner must accept a value in its supported range",
                    120, ((Number) configurableUI.getRequestTimeoutSpinner().getValue()).intValue());
            assertEquals("the prompt size spinner must accept a value in its supported range",
                    40_000, ((Number) configurableUI.getMaxPromptCharactersSpinner().getValue()).intValue());
        });
    }

    private void writeScreenshot(String fileName, Consumer<ApiKeyConfigurableUI> customizer) throws Exception {
        File outputDir = screenshotDirectory();
        AtomicReference<BufferedImage> imageRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                ApiKeyConfigurableUI configurableUI = new ApiKeyConfigurableUI();
                customizer.accept(configurableUI);

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

        File output = new File(outputDir, fileName);
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
