package com.hmydk.aigit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.AIService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import com.hmydk.aigit.util.CommonUtil;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OllamaService
 *
 * @author hmydk
 */
public class OllamaService implements AIService {
    @Override
    public boolean generateByStream() {
        return true;
    }

    // private static final Logger log =
    // LoggerFactory.getLogger(OllamaService.class);
    @Override
    public String generateCommitMessage(String content) throws Exception {

        ApiKeySettings settings = ApiKeySettings.getInstance();
        String selectedModule = settings.getSelectedModule();
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(Constants.Ollama);
        String aiResponse = getAIResponse(selectedModule, moduleConfig.getUrl(), content);

        return aiResponse.replaceAll("```", "");
    }

    @Override
    public void generateCommitMessageStream(String content, Consumer<String> onNext, Consumer<Throwable> onError,
            Runnable onComplete) throws Exception {
        getAIResponseStream(content, onNext);
    }

    @Override
    public boolean checkNecessaryModuleConfigIsRight() {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(Constants.Ollama);
        if (moduleConfig == null) {
            return false;
        }
        String selectedModule = settings.getSelectedModule();
        String url = moduleConfig.getUrl();
        return StringUtils.isNotEmpty(selectedModule) && StringUtils.isNotEmpty(url);
    }

    @Override
    public Pair<Boolean, String> validateConfig(Map<String, String> config) {
        HttpURLConnection connection = null;
        try {
            connection = getHttpURLConnection(config.get("module"), config.get("url"), "hi");
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

    private static String getAIResponse(String module, String url, String textContent) throws Exception {
        HttpURLConnection connection = getHttpURLConnection(module, url, textContent);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        return jsonResponse.path("response").asText();
    }

    private static @NotNull HttpURLConnection getHttpURLConnection(String module, String url, String textContent)
            throws IOException {

        GenerateRequest request = new GenerateRequest(module, textContent, false);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonInputString = objectMapper.writeValueAsString(request);

        URI uri = URI.create(url);
        Proxy proxy = CommonUtil.getProxy(uri);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(proxy);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        return connection;
    }

    private static class GenerateRequest {
        private String model;
        private String prompt;
        private boolean stream;

        public GenerateRequest(String model, String prompt, boolean stream) {
            this.model = model;
            this.prompt = prompt;
            this.stream = stream;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public boolean isStream() {
            return stream;
        }

        public void setStream(boolean stream) {
            this.stream = stream;
        }
    }

    private void getAIResponseStream(String textContent, Consumer<String> onNext) throws Exception {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        String selectedModule = settings.getSelectedModule();
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(Constants.Ollama);

        GenerateRequest request = new GenerateRequest(selectedModule, textContent, true);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonInputString = objectMapper.writeValueAsString(request);

        URI uri = URI.create(moduleConfig.getUrl());
        Proxy proxy = CommonUtil.getProxy(uri);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(proxy);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                JsonNode jsonResponse = objectMapper.readTree(line);
                String response = jsonResponse.path("response").asText();
                if (!response.isEmpty()) {
                    onNext.accept(response);
                }
            }
        }
    }
}
