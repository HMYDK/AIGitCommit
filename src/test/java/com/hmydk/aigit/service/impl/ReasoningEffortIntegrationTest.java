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
 * Headless IntelliJ Platform integration test for issue #92: the optional OpenAI
 * {@code reasoning_effort} request field.
 *
 * <p>The real platform application services are used and the plugin talks to a local mock
 * chat-completions endpoint, so the outgoing HTTP request can be asserted without an API key or a
 * GUI.</p>
 */
public class ReasoningEffortIntegrationTest extends BasePlatformTestCase {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MODEL_ID = "reasoning-model-test";

    private HttpServer server;
    private String endpoint;
    private final List<CapturedRequest> requests = new ArrayList<>();
    private ApiKeySettings.ModuleConfig previousConfig;
    private String previousReasoningEffort;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        requests.clear();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", this::handleChatCompletions);
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";

        ApiKeySettings settings = ApiKeySettings.getInstance();
        assertNotNull("ApiKeySettings application service is not registered", settings);
        previousConfig = settings.getModuleConfigs().get(Constants.OpenAI_Compatible);
        previousReasoningEffort = settings.getReasoningEffort();
        settings.getModuleConfigs().put(Constants.OpenAI_Compatible,
                new ApiKeySettings.ModuleConfig(endpoint, "test-key", MODEL_ID));
        settings.setReasoningEffort(null);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            ApiKeySettings settings = ApiKeySettings.getInstance();
            if (settings != null) {
                if (previousConfig == null) {
                    settings.getModuleConfigs().remove(Constants.OpenAI_Compatible);
                } else {
                    settings.getModuleConfigs().put(Constants.OpenAI_Compatible, previousConfig);
                }
                settings.setReasoningEffort(previousReasoningEffort);
            }
        } finally {
            if (server != null) {
                server.stop(0);
                server = null;
            }
            super.tearDown();
        }
    }

    public void testStreamingRequestCarriesConfiguredReasoningEffort() throws Exception {
        ApiKeySettings.getInstance().setReasoningEffort("high");
        StringBuilder received = new StringBuilder();
        List<Throwable> errors = new ArrayList<>();
        boolean[] completed = {false};

        new OpenAICompatibleService().generateCommitMessageStream(
                "write a commit message", received::append, errors::add, () -> completed[0] = true);

        assertTrue("unexpected streaming errors: " + errors, errors.isEmpty());
        assertTrue("onComplete callback was not invoked", completed[0]);
        assertEquals("feat: add reasoning effort", received.toString());

        assertEquals(1, requests.size());
        CapturedRequest request = requests.get(0);
        assertEquals("Bearer test-key", request.firstHeader("authorization"));
        JsonNode body = OBJECT_MAPPER.readTree(request.body);
        assertEquals(MODEL_ID, body.path("model").asText());
        assertTrue(body.path("stream").asBoolean());
        assertEquals("high", body.path("reasoning_effort").asText());
    }

    public void testStreamingRequestOmitsReasoningEffortByDefault() throws Exception {
        StringBuilder received = new StringBuilder();
        List<Throwable> errors = new ArrayList<>();

        new OpenAICompatibleService().generateCommitMessageStream(
                "write a commit message", received::append, errors::add, () -> {
                });

        assertTrue("unexpected streaming errors: " + errors, errors.isEmpty());
        assertEquals("feat: add reasoning effort", received.toString());

        assertEquals(1, requests.size());
        JsonNode body = OBJECT_MAPPER.readTree(requests.get(0).body);
        assertFalse("a default configuration must keep the previous request shape: " + body,
                body.has("reasoning_effort"));
    }

    public void testValidateConfigUsesConfiguredReasoningEffort() {
        ApiKeySettings.getInstance().setReasoningEffort("low");

        Pair<Boolean, String> result = new OpenAICompatibleService().validateConfig(Map.of(
                "url", endpoint,
                "module", MODEL_ID,
                "apiKey", "test-key"));

        assertTrue("validation should pass, but got: " + result.getRight(), result.getLeft());
        assertEquals(1, requests.size());
        try {
            JsonNode body = OBJECT_MAPPER.readTree(requests.get(0).body);
            assertEquals("low", body.path("reasoning_effort").asText());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private void handleChatCompletions(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new CapturedRequest(exchange.getRequestMethod(), copyHeaders(exchange), body));

        if (OBJECT_MAPPER.readTree(body).path("stream").asBoolean()) {
            respondSse(exchange);
        } else {
            respondJson(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");
        }
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
                data: {"id":"chatcmpl-test","choices":[{"delta":{"content":"feat: add reasoning effort"}}]}

                data: [DONE]

                """;
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
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
