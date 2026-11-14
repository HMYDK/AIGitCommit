package com.hmydk.aigit.config;

import com.hmydk.aigit.constant.Constants;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless IntelliJ Platform integration test for the settings UI of issue #96.
 *
 * <p>It opens the real {@link ApiKeyConfigurable} in the test application, verifies that the
 * persisted request timeout and prompt size are loaded into the settings panel, changes both values
 * and applies them, then checks the values that would be written to {@code AIGitCommitSettings.xml}.
 * The panel itself is rendered for reviewers by {@code SettingsUiScreenshotTest}.</p>
 */
public class ApiKeyConfigurableTest extends BasePlatformTestCase {

    private int previousTimeoutSeconds;
    private int previousMaxPromptCharacters;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ApiKeySettings settings = ApiKeySettings.getInstance();
        assertNotNull("ApiKeySettings application service is not registered", settings);
        previousTimeoutSeconds = settings.getRequestTimeoutSeconds();
        previousMaxPromptCharacters = settings.getMaxPromptCharacters();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            ApiKeySettings settings = ApiKeySettings.getInstance();
            if (settings != null) {
                settings.setRequestTimeoutSeconds(previousTimeoutSeconds);
                settings.setMaxPromptCharacters(previousMaxPromptCharacters);
            }
        } finally {
            super.tearDown();
        }
    }

    public void testRequestLimitsAreLoadedAppliedAndPersisted() {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        settings.setRequestTimeoutSeconds(90);
        settings.setMaxPromptCharacters(30_000);

        ApiKeyConfigurable configurable = new ApiKeyConfigurable();
        JComponent panel = configurable.createComponent();
        assertNotNull(panel);

        List<JSpinner> spinners = new ArrayList<>();
        collectSpinners(panel, spinners);
        assertEquals("the basic settings panel should expose exactly two numeric request limits, but found: "
                + describe(spinners), 2, spinners.size());

        JSpinner timeoutSpinner = spinners.get(0);
        JSpinner promptSizeSpinner = spinners.get(1);
        assertEquals(90, ((Number) timeoutSpinner.getValue()).intValue());
        assertEquals(30_000, ((Number) promptSizeSpinner.getValue()).intValue());

        timeoutSpinner.setValue(120);
        promptSizeSpinner.setValue(40_000);
        configurable.apply();

        assertEquals(120, settings.getRequestTimeoutSeconds());
        assertEquals(120_000, settings.getRequestTimeoutMillis());
        assertEquals(40_000, settings.getMaxPromptCharacters());

        // Bounds come from the spinner model: values outside the documented range cannot be entered.
        assertEquals(Constants.MIN_REQUEST_TIMEOUT_SECONDS,
                ((Number) ((javax.swing.SpinnerNumberModel) timeoutSpinner.getModel()).getMinimum()).intValue());
        assertEquals(Constants.MAX_REQUEST_TIMEOUT_SECONDS,
                ((Number) ((javax.swing.SpinnerNumberModel) timeoutSpinner.getModel()).getMaximum()).intValue());
        assertEquals(Constants.MIN_MAX_PROMPT_CHARACTERS,
                ((Number) ((javax.swing.SpinnerNumberModel) promptSizeSpinner.getModel()).getMinimum()).intValue());
        assertEquals(Constants.MAX_MAX_PROMPT_CHARACTERS,
                ((Number) ((javax.swing.SpinnerNumberModel) promptSizeSpinner.getModel()).getMaximum()).intValue());
    }

    private static void collectSpinners(Component component, List<JSpinner> result) {
        if (component instanceof JSpinner) {
            result.add((JSpinner) component);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                collectSpinners(child, result);
            }
        }
    }

    private static String describe(List<JSpinner> spinners) {
        List<String> values = new ArrayList<>();
        for (JSpinner spinner : spinners) {
            values.add(spinner.getToolTipText());
        }
        return values.toString();
    }
}
