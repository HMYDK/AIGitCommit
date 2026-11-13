package com.hmydk.aigit.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Request-shaping tests for the OpenAI chat-completions payload.
 */
class OpenAIUtilTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void includesReasoningEffortWhenConfigured() throws Exception {
        JsonNode body = OBJECT_MAPPER.readTree(OpenAIUtil.buildRequestBody(
                "gpt-5", "generate a commit message", true, "medium"));

        assertEquals("medium", body.path("reasoning_effort").asText());
        assertEquals("gpt-5", body.path("model").asText());
        assertTrue(body.path("stream").asBoolean());
        assertEquals(1, body.path("messages").size());
        assertEquals("user", body.path("messages").get(0).path("role").asText());
        assertEquals("generate a commit message", body.path("messages").get(0).path("content").asText());
    }

    @Test
    void omitsReasoningEffortWhenNotConfigured() throws Exception {
        JsonNode body = OBJECT_MAPPER.readTree(OpenAIUtil.buildRequestBody(
                "gpt-5", "generate a commit message", true, null));

        assertFalse(body.has("reasoning_effort"),
                "reasoning_effort must not be sent unless the user configured it: " + body);
    }

    @Test
    void omitsReasoningEffortWhenBlank() throws Exception {
        JsonNode body = OBJECT_MAPPER.readTree(OpenAIUtil.buildRequestBody(
                "gpt-5", "generate a commit message", false, "   "));

        assertFalse(body.has("reasoning_effort"),
                "blank reasoning_effort must be normalized to 'not sent': " + body);
        assertFalse(body.path("stream").asBoolean());
    }
}
