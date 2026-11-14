package com.hmydk.aigit.service.impl;

import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.util.CommonUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Headless IntelliJ Platform integration test for the configurable request timeout of issue #96.
 *
 * <p>It boots the real IntelliJ Platform test application (application services, HTTP stack) and
 * talks to a local mock endpoint that answers much later than the configured timeout. No API key or
 * GUI is required. The same setting must apply to every provider that opens its own connection,
 * which is why both the OpenAI-compatible path and the Anthropic path are checked.</p>
 */
public class RequestTimeoutIntegrationTest extends BasePlatformTestCase {

    private HttpServer server;
    private ExecutorService serverExecutor;
    private String endpoint;
    private ApiKeySettings.ModuleConfig previousOpenAiConfig;
    private ApiKeySettings.ModuleConfig previousAnthropicConfig;
    private String previousModule;
    private int previousTimeoutSeconds;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> respondSlowly(exchange, """
                data: {"id":"chatcmpl-test","choices":[{"delta":{"content":"too late"}}]}

                data: [DONE]

                """));
        server.createContext("/v1/messages", exchange -> respondSlowly(exchange, """
                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"too late"}}

                event: message_stop
                data: {"type":"message_stop"}

                """));
        serverExecutor = Executors.newCachedThreadPool();
        server.setExecutor(serverExecutor);
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort();

        ApiKeySettings settings = ApiKeySettings.getInstance();
        assertNotNull("ApiKeySettings application service is not registered", settings);
        previousOpenAiConfig = settings.getModuleConfigs().get(Constants.OpenAI_Compatible);
        previousAnthropicConfig = settings.getModuleConfigs().get(Constants.Anthropic);
        previousModule = settings.getSelectedModule();
        previousTimeoutSeconds = settings.getRequestTimeoutSeconds();

        settings.getModuleConfigs().put(Constants.OpenAI_Compatible,
                new ApiKeySettings.ModuleConfig(endpoint + "/v1/chat/completions", "test-key", "timeout-model"));
        settings.getModuleConfigs().put(Constants.Anthropic,
                new ApiKeySettings.ModuleConfig(endpoint + "/v1/messages", "test-key"));
        settings.setSelectedModule("claude-sonnet-5-5");
        settings.setRequestTimeoutSeconds(1);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            ApiKeySettings settings = ApiKeySettings.getInstance();
            if (settings != null) {
                restore(settings, Constants.OpenAI_Compatible, previousOpenAiConfig);
                restore(settings, Constants.Anthropic, previousAnthropicConfig);
                settings.setSelectedModule(previousModule);
                settings.setRequestTimeoutSeconds(previousTimeoutSeconds);
            }
        } finally {
            if (server != null) {
                server.stop(0);
                server = null;
            }
            if (serverExecutor != null) {
                serverExecutor.shutdownNow();
                serverExecutor = null;
            }
            super.tearDown();
        }
    }

    public void testConfiguredTimeoutIsAppliedToProviderConnections() throws Exception {
        HttpURLConnection connection = openUnconnectedConnection();

        CommonUtil.applyRequestTimeout(connection);
        assertEquals(1_000, connection.getConnectTimeout());
        assertEquals(1_000, connection.getReadTimeout());

        ApiKeySettings.getInstance().setRequestTimeoutSeconds(120);
        connection = openUnconnectedConnection();
        CommonUtil.applyRequestTimeout(connection);
        assertEquals(120_000, connection.getConnectTimeout());
        assertEquals(120_000, connection.getReadTimeout());
    }

    public void testOpenAiCompatibleStreamingFailsAfterConfiguredTimeout() {
        List<Throwable> errors = new ArrayList<>();
        boolean[] completed = {false};

        long startedAt = System.nanoTime();
        try {
            new OpenAICompatibleService().generateCommitMessageStream(
                    "write a commit message", chunk -> {
                    }, errors::add, () -> completed[0] = true);
            fail("a request stalled longer than the configured timeout must fail");
        } catch (Exception e) {
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            assertTrue("expected a SocketTimeoutException, got: " + e, isTimeout(e));
            assertTrue("the request should fail after ~1s, but took " + elapsedMillis + "ms",
                    elapsedMillis < 5_000);
        }

        assertFalse("a timed out request must not complete", completed[0]);
    }

    public void testAnthropicStreamingFailsAfterConfiguredTimeout() {
        List<Throwable> errors = new ArrayList<>();
        boolean[] completed = {false};

        long startedAt = System.nanoTime();
        try {
            new AnthropicService().generateCommitMessageStream(
                    "write a commit message", chunk -> {
                    }, errors::add, () -> completed[0] = true);
            fail("a request stalled longer than the configured timeout must fail");
        } catch (Exception e) {
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            assertTrue("expected a SocketTimeoutException, got: " + e, isTimeout(e));
            assertTrue("the request should fail after ~1s, but took " + elapsedMillis + "ms",
                    elapsedMillis < 5_000);
        }

        assertFalse("a timed out request must not complete", completed[0]);
        assertFalse("the error callback should have been notified", errors.isEmpty());
    }

    private HttpURLConnection openUnconnectedConnection() throws IOException {
        // Nothing is sent here: the connection just needs to expose the timeouts that were applied.
        return (HttpURLConnection) URI.create(endpoint + "/v1/chat/completions").toURL().openConnection();
    }

    private static boolean isTimeout(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof SocketTimeoutException
                    || (current.getMessage() != null && current.getMessage().toLowerCase().contains("timed out"))) {
                return true;
            }
        }
        return false;
    }

    private static void respondSlowly(HttpExchange exchange, String responseBody) throws IOException {
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    private static void restore(ApiKeySettings settings, String client, ApiKeySettings.ModuleConfig config) {
        if (config == null) {
            settings.getModuleConfigs().remove(client);
        } else {
            settings.getModuleConfigs().put(client, config);
        }
    }
}
