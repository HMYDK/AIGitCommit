package com.hmydk.aigit.config;

import com.hmydk.aigit.constant.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Plain JUnit tests for the configurable request timeout and prompt size (issue #96).
 *
 * <p>These values used to be hard-coded (30 s connect/read timeout, 20000-character prompt cap).
 * The getters keep those defaults for existing installations and clamp values that are persisted
 * outside the UI range, so a corrupted settings file cannot break generation.</p>
 */
class ApiKeySettingsTest {

    @Test
    void keepsPreviousDefaultsForNewAndExistingInstallations() {
        ApiKeySettings settings = new ApiKeySettings();

        assertEquals(Constants.DEFAULT_REQUEST_TIMEOUT_SECONDS, settings.getRequestTimeoutSeconds());
        assertEquals(60, settings.getRequestTimeoutSeconds());
        assertEquals(60_000, settings.getRequestTimeoutMillis());
        assertEquals(Constants.DEFAULT_MAX_PROMPT_CHARACTERS, settings.getMaxPromptCharacters());
        assertEquals(20_000, settings.getMaxPromptCharacters());
    }

    @Test
    void usesConfiguredValues() {
        ApiKeySettings settings = new ApiKeySettings();

        settings.setRequestTimeoutSeconds(120);
        assertEquals(120, settings.getRequestTimeoutSeconds());
        assertEquals(120_000, settings.getRequestTimeoutMillis());

        settings.setMaxPromptCharacters(40_000);
        assertEquals(40_000, settings.getMaxPromptCharacters());
    }

    @Test
    void clampsValuesOutsideTheSupportedRange() {
        ApiKeySettings settings = new ApiKeySettings();

        settings.setRequestTimeoutSeconds(10_000);
        assertEquals(Constants.MAX_REQUEST_TIMEOUT_SECONDS, settings.getRequestTimeoutSeconds());

        settings.setRequestTimeoutSeconds(0);
        assertEquals(Constants.DEFAULT_REQUEST_TIMEOUT_SECONDS, settings.getRequestTimeoutSeconds());

        settings.setMaxPromptCharacters(1);
        assertEquals(Constants.MIN_MAX_PROMPT_CHARACTERS, settings.getMaxPromptCharacters());

        settings.setMaxPromptCharacters(10_000_000);
        assertEquals(Constants.MAX_MAX_PROMPT_CHARACTERS, settings.getMaxPromptCharacters());

        settings.setMaxPromptCharacters(-5);
        assertEquals(Constants.DEFAULT_MAX_PROMPT_CHARACTERS, settings.getMaxPromptCharacters());
    }
}
