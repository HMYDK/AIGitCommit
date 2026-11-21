package com.hmydk.aigit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WelcomeNotificationTest {

    @Test
    void readsVersionGeneratedFromGradleProjectVersion() {
        String expected = System.getProperty("aigit.plugin.version");
        String actual = WelcomeNotification.getCurrentPluginVersion();

        assertNotNull(actual, "the packaged version resource must contain a version");
        if (expected != null) {
            // Set by Gradle, so the test follows version bumps instead of hardcoding one.
            assertEquals(expected, actual);
        } else {
            // IDE test runs do not pass the Gradle property; only require an expanded version.
            assertTrue(actual.matches("\\d+\\.\\d+\\.\\d+.*"), "unexpected version: " + actual);
        }
    }
}
