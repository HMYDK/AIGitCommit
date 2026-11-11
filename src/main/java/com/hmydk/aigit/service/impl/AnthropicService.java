package com.hmydk.aigit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.pojo.AnthropicRequestBO;
import com.hmydk.aigit.service.AIService;
import com.hmydk.aigit.util.CommonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Official Anthropic (Claude) API support, using the Messages API.
 *
 * <p>Unlike the OpenAI-compatible providers, Anthropic uses a dedicated endpoint
 * ({@code /v1/messages}), the {@code x-api-key} header instead of a Bearer token, a required
 * {@code max_tokens} field, and a different streaming event format.</p>
 *
 * @see <a href="https://platform.claude.com/docs/en/api/messages">Anthropic Messages API</a>
 */
public class AnthropicService implements AIService {

    static final String ANTHROPIC_VERSION = "2023-06-01";
    static final int DEFAULT_MAX_TOKENS = 1024;
    static final int VALIDATION_MAX_TOKENS = 1;
    static final int CONNECT_TIMEOUT_MS = 30_000;
    static final int READ_TIMEOUT_MS = 60_000;

    private static final String SSE_DATA_PREFIX = "data: ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(AnthropicService.class);

    @Override
    public boolean generateByStream() {
        return true;
    }

    @Override
    public String generateCommitMessage(String content) throws Exception {
        AnthropicRequest request = resolveRequest(ApiKeySettings.getInstance());
        HttpURLConnection connection = openConnection(request, content, false, DEFAULT_MAX_TOKENS);
        try {
            ensureSuccess(connection);
            String responseBody = readResponseBody(connection);
            log.info("Anthropic response: {}", responseBody);
            return extractResponseText(responseBody).replace("```", "").trim();
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public void generateCommitMessageStream(String content, Consumer<String> onNext, Consumer<Throwable> onError,
            Runnable onComplete) throws Exception {
        AnthropicRequest request = resolveRequest(ApiKeySettings.getInstance());
        HttpURLConnection connection = openConnection(request, content, true, DEFAULT_MAX_TOKENS);
        try {
            ensureSuccess(connection);
            String charset = CommonUtil.getCharsetFromContentType(connection.getContentType());
            boolean completed = false;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith(SSE_DATA_PREFIX)) {
                        continue;
                    }
                    String data = line.substring(SSE_DATA_PREFIX.length()).trim();
                    if (data.isEmpty()) {
                        continue;
                    }
                    JsonNode event = OBJECT_MAPPER.readTree(data);
                    String eventType = event.path("type").asText();
                    if ("content_block_delta".equals(eventType)) {
                        String text = extractTextDelta(event);
                        if (text != null && !text.isEmpty()) {
                            onNext.accept(text);
                        }
                    } else if ("message_stop".equals(eventType)) {
                        completed = true;
                        break;
                    } else if ("error".equals(eventType)) {
                        throw new Exception(describeError(event));
                    }
                }
            }
            if (!completed) {
                log.warn("Anthropic stream ended without a message_stop event");
            }
            if (onComplete != null) {
                onComplete.run();
            }
        } catch (Exception e) {
            if (onError != null) {
                onError.accept(e);
            }
            throw e;
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public boolean checkNecessaryModuleConfigIsRight() {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(Constants.Anthropic);
        if (moduleConfig == null) {
            return false;
        }
        return StringUtils.isNotEmpty(settings.getSelectedModule())
                && StringUtils.isNotEmpty(moduleConfig.getUrl())
                && StringUtils.isNotEmpty(moduleConfig.getApiKey());
    }

    @Override
    public Pair<Boolean, String> validateConfig(Map<String, String> config) {
        HttpURLConnection connection = null;
        try {
            AnthropicRequest request = new AnthropicRequest(
                    config.get("url"),
                    config.get("apiKey"),
                    StringUtils.defaultIfBlank(config.get("module"), defaultModel()));
            connection = openConnection(request, "hi", false, VALIDATION_MAX_TOKENS);
            if (connection.getResponseCode() != 200) {
                return Pair.of(false, readErrorBody(connection));
            }
        } catch (Exception e) {
            return Pair.of(false, e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return Pair.of(true, "");
    }

    @NotNull
    private static String defaultModel() {
        String[] defaults = Constants.CLIENT_MODULES.get(Constants.Anthropic);
        if (defaults != null && defaults.length > 0 && StringUtils.isNotBlank(defaults[0])) {
            return defaults[0];
        }
        throw new IllegalArgumentException("Anthropic default model is missing");
    }

    @NotNull
    static String buildRequestBody(String model, String prompt, boolean stream) throws IOException {
        AnthropicRequestBO requestBO = new AnthropicRequestBO(model, DEFAULT_MAX_TOKENS, prompt, stream);
        return OBJECT_MAPPER.writeValueAsString(requestBO);
    }

    /**
     * Extracts the assistant text from a non-streaming Messages API response.
     */
    @NotNull
    static String extractResponseText(@NotNull String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode content = root.path("content");
        if (!content.isArray()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        return text.toString();
    }

    /**
     * Returns the text of a {@code content_block_delta} streaming event, or {@code null} for any
     * other event type (message start/ping/stop, thinking deltas, ...).
     */
    @Nullable
    static String extractTextDelta(@Nullable JsonNode event) {
        if (event == null || !"content_block_delta".equals(event.path("type").asText())) {
            return null;
        }
        JsonNode delta = event.path("delta");
        if (!"text_delta".equals(delta.path("type").asText())) {
            return null;
        }
        return delta.path("text").asText("");
    }

    @NotNull
    private static AnthropicRequest resolveRequest(ApiKeySettings settings) {
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(Constants.Anthropic);
        ApiKeySettings.ModuleConfig defaultConfig = Constants.moduleConfigs.get(Constants.Anthropic);
        if (moduleConfig == null && defaultConfig == null) {
            throw new IllegalArgumentException("Anthropic module configuration is missing");
        }

        String url = moduleConfig != null && StringUtils.isNotEmpty(moduleConfig.getUrl())
                ? moduleConfig.getUrl()
                : defaultConfig.getUrl();
        String apiKey = moduleConfig != null ? moduleConfig.getApiKey() : defaultConfig.getApiKey();
        String model = resolveModel(settings, moduleConfig);
        return new AnthropicRequest(url, apiKey, model);
    }

    @NotNull
    private static String resolveModel(ApiKeySettings settings, ApiKeySettings.ModuleConfig moduleConfig) {
        String selectedModule = settings.getSelectedModule();
        if (StringUtils.isNotBlank(selectedModule)) {
            return selectedModule;
        }
        if (moduleConfig != null && StringUtils.isNotBlank(moduleConfig.getModelId())) {
            return moduleConfig.getModelId();
        }
        return defaultModel();
    }

    @NotNull
    private static HttpURLConnection openConnection(@NotNull AnthropicRequest request,
            @NotNull String prompt,
            boolean stream,
            int maxTokens) throws IOException {
        String url = StringUtils.isNotBlank(request.url())
                ? request.url()
                : Constants.moduleConfigs.get(Constants.Anthropic).getUrl();
        AnthropicRequestBO requestBO = new AnthropicRequestBO(request.model(), maxTokens, prompt, stream);
        String jsonInputString = OBJECT_MAPPER.writeValueAsString(requestBO);

        URI uri = URI.create(url);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(CommonUtil.getProxy(uri));
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", stream ? "text/event-stream" : "application/json");
        connection.setRequestProperty("x-api-key", StringUtils.defaultString(request.apiKey()));
        connection.setRequestProperty("anthropic-version", ANTHROPIC_VERSION);
        connection.setDoOutput(true);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }

    private static void ensureSuccess(@NotNull HttpURLConnection connection) throws Exception {
        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new Exception(readErrorBody(connection));
        }
    }

    @NotNull
    private static String readResponseBody(@NotNull HttpURLConnection connection) throws IOException {
        try (InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, CommonUtil.getCharsetFromContentType(
                                connection.getContentType())))) {
            return readAll(reader);
        }
    }

    @NotNull
    private static String readErrorBody(@NotNull HttpURLConnection connection) {
        try (InputStream errorStream = connection.getErrorStream()) {
            if (errorStream == null) {
                return "Anthropic request failed with HTTP " + connection.getResponseCode();
            }
            return readAll(new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            return e.getMessage() == null ? "Anthropic request failed" : e.getMessage();
        }
    }

    @NotNull
    private static String readAll(@NotNull BufferedReader reader) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        return response.toString();
    }

    @NotNull
    private static String describeError(@NotNull JsonNode event) {
        JsonNode error = event.path("error");
        String message = error.path("message").asText("");
        return message.isEmpty() ? event.toString() : message;
    }

    private record AnthropicRequest(String url, String apiKey, String model) {
    }
}
