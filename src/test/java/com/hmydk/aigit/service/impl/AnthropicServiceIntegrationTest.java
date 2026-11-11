package com.hmydk.aigit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Headless IntelliJ Platform integration test for the official Anthropic provider.
 *
 * <p>Unlike the plain JUnit tests, this test boots the real IntelliJ Platform test application
 * (application services, HTTP stack) and talks to a local mock Anthropic Messages API endpoint, so
 * it verifies the behavior the plugin would have inside a real IDE without needing an API key or a
 * GUI.</p>
 */
public class AnthropicServiceIntegrationTest extends BasePlatformTestCase {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private HttpServer server;
    private String endpoint;
    private final List<CapturedRequest> requests = new ArrayList<>();
    private ApiKeySettings.ModuleConfig previousConfig;
    private String previousModule;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        requests.clear();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/messages", this::handleMessages);
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/messages";

        ApiKeySettings settings = ApiKeySettings.getInstance();
        assertNotNull("ApiKeySettings application service is not registered", settings);
        previousConfig = settings.getModuleConfigs().get(Constants.Anthropic);
        previousModule = settings.getSelectedModule();
        settings.getModuleConfigs().put(Constants.Anthropic,
                new ApiKeySettings.ModuleConfig(endpoint, "test-key"));
        settings.setSelectedModule("claude-sonnet-5-5");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            ApiKeySettings settings = ApiKeySettings.getInstance();
            if (settings != null) {
                if (previousConfig == null) {
                    settings.getModuleConfigs().remove(Constants.Anthropic);
                } else {
                    settings.getModuleConfigs().put(Constants.Anthropic, previousConfig);
                }
                settings.setSelectedModule(previousModule);
            }
        } finally {
            if (server != null) {
                server.stop(0);
                server = null;
            }
            super.tearDown();
        }
    }

    public void testGenerateCommitMessageSendsOfficialMessagesApiRequest() throws Exception {
        String message = new AnthropicService().generateCommitMessage("write a commit message");

        assertEquals("feat(provider): add Claude", message);
        assertEquals(1, requests.size());
        CapturedRequest request = requests.get(0);
        assertEquals("POST", request.method);
        assertEquals("test-key", request.firstHeader("x-api-key"));
        assertEquals(AnthropicService.ANTHROPIC_VERSION, request.firstHeader("anthropic-version"));

        JsonNode body = OBJECT_MAPPER.readTree(request.body);
        assertEquals("claude-sonnet-5-5", body.path("model").asText());
        assertEquals(AnthropicService.DEFAULT_MAX_TOKENS, body.path("max_tokens").asInt());
        assertFalse(body.path("stream").asBoolean());
        assertEquals(1, body.path("messages").size());
        assertEquals("user", body.path("messages").get(0).path("role").asText());
        assertEquals("write a commit message", body.path("messages").get(0).path("content").asText());
    }

    public void testGenerateCommitMessageStreamConsumesAnthropicSseEvents() throws Exception {
        StringBuilder received = new StringBuilder();
        boolean[] completed = {false};
        List<Throwable> errors = new ArrayList<>();

        new AnthropicService().generateCommitMessageStream(
                "write a commit message",
                received::append,
                errors::add,
                () -> completed[0] = true);

        assertEquals("feat: add Claude", received.toString());
        assertTrue("onComplete callback was not invoked", completed[0]);
        assertTrue("unexpected streaming errors: " + errors, errors.isEmpty());

        assertEquals(1, requests.size());
        CapturedRequest request = requests.get(0);
        assertEquals("text/event-stream", request.firstHeader("accept"));
        JsonNode body = OBJECT_MAPPER.readTree(request.body);
        assertTrue(body.path("stream").asBoolean());
    }

    public void testValidateConfigAcceptsWorkingCredentials() {
        Pair<Boolean, String> result = new AnthropicService().validateConfig(Map.of(
                "url", endpoint,
                "module", "claude-haiku-5-5",
                "apiKey", "test-key"));

        assertTrue("validation should pass, but got: " + result.getRight(), result.getLeft());
        assertEquals(1, requests.size());
        JsonNode body;
        try {
            body = OBJECT_MAPPER.readTree(requests.get(0).body);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        assertEquals("claude-haiku-5-5", body.path("model").asText());
        assertEquals(AnthropicService.VALIDATION_MAX_TOKENS, body.path("max_tokens").asInt());
    }

    public void testValidateConfigReportsApiError() {
        Pair<Boolean, String> result = new AnthropicService().validateConfig(Map.of(
                "url", endpoint,
                "module", "claude-haiku-5-5",
                "apiKey", "bad-key"));

        assertFalse("validation should fail for an invalid key", result.getLeft());
        assertTrue("error message should contain the API error: " + result.getRight(),
                result.getRight().contains("invalid x-api-key"));
    }

    private void handleMessages(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new CapturedRequest(exchange.getRequestMethod(), copyHeaders(exchange), body));

        if ("bad-key".equals(firstHeader(exchange, "x-api-key"))) {
            respondJson(exchange, 401,
                    "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\","
                            + "\"message\":\"invalid x-api-key\"}}");
            return;
        }

        JsonNode json = OBJECT_MAPPER.readTree(body);
        if (json.path("max_tokens").asInt() == AnthropicService.VALIDATION_MAX_TOKENS) {
            respondJson(exchange, 200, "{\"type\":\"message\",\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}");
            return;
        }
        if (json.path("stream").asBoolean()) {
            respondSse(exchange);
            return;
        }
        respondJson(exchange, 200,
                "{\"type\":\"message\",\"role\":\"assistant\","
                        + "\"content\":[{\"type\":\"text\",\"text\":\"feat(provider): add Claude\"}]}");
    }

    private void respondJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    private void respondSse(HttpExchange exchange) throws IOException {
        String body = """
                event: message_start
                data: {"type":"message_start","message":{"id":"msg_test","role":"assistant"}}

                event: content_block_start
                data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"feat: add "}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Claude"}}

                event: message_stop
                data: {"type":"message_stop"}

                """;
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    private static String firstHeader(HttpExchange exchange, String name) {
        List<String> values = exchange.getRequestHeaders().get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static Map<String, List<String>> copyHeaders(HttpExchange exchange) {
        Map<String, List<String>> copy = new HashMap<>();
        exchange.getRequestHeaders().forEach((key, values) -> copy.put(key.toLowerCase(java.util.Locale.ROOT),
                List.copyOf(values)));
        return copy;
    }

    private static final class CapturedRequest {
        private final String method;
        private final Map<String, List<String>> headers;
        private final String body;

        private CapturedRequest(String method, Map<String, List<String>> headers, String body) {
            this.method = method;
            this.headers = headers;
            this.body = body;
        }

        private String firstHeader(String name) {
            List<String> values = headers.get(name.toLowerCase(java.util.Locale.ROOT));
            return values == null || values.isEmpty() ? null : values.get(0);
        }
    }
}
