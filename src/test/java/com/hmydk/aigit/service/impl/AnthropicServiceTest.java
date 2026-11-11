package com.hmydk.aigit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnthropicServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void buildsMessagesApiRequestWithRequiredFields() throws Exception {
        JsonNode json = OBJECT_MAPPER.readTree(
                AnthropicService.buildRequestBody("claude-sonnet-5-5", "generate commit message", false));

        assertEquals("claude-sonnet-5-5", json.path("model").asText());
        assertEquals(AnthropicService.DEFAULT_MAX_TOKENS, json.path("max_tokens").asInt());
        assertFalse(json.path("stream").asBoolean());
        assertTrue(json.path("messages").isArray());
        assertEquals(1, json.path("messages").size());
        assertEquals("user", json.path("messages").get(0).path("role").asText());
        assertEquals("generate commit message", json.path("messages").get(0).path("content").asText());
    }

    @Test
    void buildsStreamingRequestWithStreamFlag() throws Exception {
        JsonNode json = OBJECT_MAPPER.readTree(
                AnthropicService.buildRequestBody("claude-haiku-5-5", "prompt", true));

        assertEquals("claude-haiku-5-5", json.path("model").asText());
        assertTrue(json.path("stream").asBoolean());
    }

    @Test
    void extractsTextFromNonStreamingResponse() throws Exception {
        String responseBody = """
                {"id":"msg_1","type":"message","role":"assistant","model":"claude-sonnet-5-5",
                 "content":[{"type":"thinking","thinking":"internal"},{"type":"text","text":"feat(provider): add Claude"}]}
                """;

        assertEquals("feat(provider): add Claude", AnthropicService.extractResponseText(responseBody));
    }

    @Test
    void extractsStreamingTextDeltaOnly() throws Exception {
        JsonNode textDelta = OBJECT_MAPPER.readTree("""
                {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"feat: add "}}
                """);
        assertEquals("feat: add ", AnthropicService.extractTextDelta(textDelta));

        assertNull(AnthropicService.extractTextDelta(OBJECT_MAPPER.readTree("""
                {"type":"message_start","message":{"id":"msg_1"}}
                """)));
        assertNull(AnthropicService.extractTextDelta(OBJECT_MAPPER.readTree("""
                {"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"hmm"}}
                """)));
        assertNull(AnthropicService.extractTextDelta(null));
    }
}
