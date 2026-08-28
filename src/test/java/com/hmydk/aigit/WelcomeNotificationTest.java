package com.hmydk.aigit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WelcomeNotificationTest {

    @Test
    void readsVersionGeneratedFromGradleProjectVersion() {
        assertEquals("2.2.1", WelcomeNotification.getCurrentPluginVersion());
    }
}
