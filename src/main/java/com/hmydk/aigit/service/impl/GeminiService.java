package com.hmydk.aigit.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.pojo.GeminiRequestBO;
import com.hmydk.aigit.service.AIService;
import com.hmydk.aigit.util.CommonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OpenAIService
 *
 * @author hmydk
 */
public class GeminiService implements AIService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final Gson GSON = new Gson();

    @Override
    public boolean generateByStream() {
        return true;
    }

    @Override
    public String generateCommitMessage(String content) throws Exception {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        String selectedModule = settings.getSelectedModule();
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(Constants.Gemini);
        String aiResponse = getAIResponse(moduleConfig.getUrl(), selectedModule, moduleConfig.getApiKey(), content);
        log.info("aiResponse is  :\n{}", aiResponse);
        return aiResponse.replaceAll("```", "");
    }

    @Override
    public void generateCommitMessageStream(String content, Consumer<String> onNext, Consumer<Throwable> onError,
            Runnable onComplete)
            throws Exception {
        getAIResponseStream(content, onNext, onError, onComplete);
    }

    @Override
    public boolean checkNecessaryModuleConfigIsRight() {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(Constants.Gemini);
        if (moduleConfig == null) {
            return false;
        }
        String selectedModule = settings.getSelectedModule();
        String url = moduleConfig.getUrl();
        String apiKey = moduleConfig.getApiKey();
        return StringUtils.isNotEmpty(selectedModule) && StringUtils.isNotEmpty(url) && StringUtils.isNotEmpty(apiKey);
    }

    @Override
    public Pair<Boolean, String> validateConfig(Map<String, String> config) {
        HttpURLConnection connection = null;
        try {
            connection = getHttpURLConnection(config.get("url"), config.get("module"), config.get("apiKey"), "hi");
            if (connection.getResponseCode() != 200) {
                // 读取错误响应
                StringBuilder response = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                return Pair.of(false, response.toString());
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

    public static String getAIResponse(String url, String module, String apiKey, String textContent) throws Exception {
        HttpURLConnection connection = getStreamHttpURLConnection(url, module, apiKey, textContent);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
        JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
            JsonArray parts = content == null ? null : content.getAsJsonArray("parts");
            if (parts != null && !parts.isEmpty()) {
                JsonElement text = parts.get(0).getAsJsonObject().get("text");
                if (text != null && !text.isJsonNull()) {
                    return text.getAsString();
                }
            }
        }
        return "sth error when request ai api";
    }

    private static @NotNull HttpURLConnection getHttpURLConnection(String url, String module, String apiKey,
            String textContent) throws IOException {
        // String apiUrl =
        // "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key="
        // + apiKey;
        String apiUrl = url + "/" + module + ":generateContent?key=" + apiKey;
        GeminiRequestBO geminiRequestBO = new GeminiRequestBO();
        geminiRequestBO
                .setContents(List.of(new GeminiRequestBO.Content(List.of(new GeminiRequestBO.Part(textContent)))));
        String jsonInputString = GSON.toJson(geminiRequestBO);

        URI uri = URI.create(apiUrl);
        Proxy proxy = CommonUtil.getProxy(uri);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(proxy);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        CommonUtil.applyRequestTimeout(connection);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }

    private static @NotNull HttpURLConnection getStreamHttpURLConnection(String url, String module, String apiKey,
            String textContent) throws IOException {
        // String apiUrl =
        // "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key="
        // + apiKey;
        String apiUrl = url + "/" + module + ":streamGenerateContent?alt=sse&key=" + apiKey;
        GeminiRequestBO geminiRequestBO = new GeminiRequestBO();
        geminiRequestBO
                .setContents(List.of(new GeminiRequestBO.Content(List.of(new GeminiRequestBO.Part(textContent)))));
        geminiRequestBO
                .setGenerationConfig(new GeminiRequestBO.GenerationConfig(new GeminiRequestBO.ThinkingConfig(0)));
        String jsonInputString = GSON.toJson(geminiRequestBO);

        URI uri = URI.create(apiUrl);
        Proxy proxy = CommonUtil.getProxy(uri);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(proxy);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        connection.setDoOutput(true);
        CommonUtil.applyRequestTimeout(connection);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }

    private void getAIResponseStream(String textContent, Consumer<String> onNext, Consumer<Throwable> onError,
            Runnable onComplete) throws Exception {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        String selectedModule = settings.getSelectedModule();
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(Constants.Gemini);

        HttpURLConnection connection = getStreamHttpURLConnection(moduleConfig.getUrl(), selectedModule,
                moduleConfig.getApiKey(), textContent);

        // 获取响应的字符集
        String charset = CommonUtil.getCharsetFromContentType(connection.getContentType());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6);
                    if (!"[DONE]".equals(jsonData)) {
                        JsonObject root = JsonParser.parseString(jsonData).getAsJsonObject();
                        JsonArray candidates = root.getAsJsonArray("candidates");
                        if (candidates != null && !candidates.isEmpty()) {
                            JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                            JsonArray parts = content == null ? null : content.getAsJsonArray("parts");
                            if (parts != null && !parts.isEmpty()) {
                                JsonElement text = parts.get(0).getAsJsonObject().get("text");
                                if (text != null && !text.isJsonNull()) {
                                    onNext.accept(text.getAsString());
                                }
                            }
                        }
                    } else {
                        if (onComplete != null) {
                            onComplete.run();
                        }
                        break;
                    }
                }
            }
        }
    }
}
