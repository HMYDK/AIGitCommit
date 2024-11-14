package com.hmydk.aigit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.pojo.OpenAIRequestBO;
import com.hmydk.aigit.service.AIService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * OpenAIService
 *
 * @author hmydk
 */
public class OpenAIService implements AIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIService.class);

    @Override
    public String generateCommitMessage(String content) {
        String aiResponse;
        try {
            ApiKeySettings settings = ApiKeySettings.getInstance();
            String selectedModule = settings.getSelectedModule();
            ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(Constants.OpenAI);
            aiResponse = getAIResponse(moduleConfig.getUrl(), selectedModule, moduleConfig.getApiKey(), content);
        } catch (Exception e) {
            return e.getMessage();
        }
        log.info("aiResponse is  :\n{}", aiResponse);
        return aiResponse.replaceAll("```", "");
    }

    @Override
    public boolean checkNecessaryModuleConfigIsRight() {
        String apiKey = ApiKeySettings.getInstance().getModuleConfigs().get(Constants.OpenAI).getApiKey();
        return !apiKey.isEmpty();
    }

    @Override
    public boolean validateConfig(Map<String, String> config) {
        int statusCode;
        try {
            HttpURLConnection connection = getHttpURLConnection(config.get("url"), config.get("module"), config.get("apiKey"), "hi");
//            getChatCompletion(config.get("url"), config.get("module"), config.get("apiKey"));
            statusCode = connection.getResponseCode();
        } catch (IOException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 打印状态码
        System.out.println("HTTP Status Code: " + statusCode);
        return statusCode == 200;
    }

    public static String getAIResponse(String url, String module, String apiKey, String textContent) throws Exception {
        HttpURLConnection connection = getHttpURLConnection(url, module, apiKey, textContent);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        System.out.println("open ai Response: " + response);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        JsonNode candidates = jsonResponse.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode firstCandidate = candidates.get(0);
            JsonNode content = firstCandidate.path("content");
            JsonNode parts = content.path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                JsonNode firstPart = parts.get(0);
                return firstPart.path("text").asText();
            }
        }
        return "sth error when request ai api";
    }

    private static @NotNull HttpURLConnection getHttpURLConnection(String url, String module, String apiKey, String textContent) throws IOException {

        OpenAIRequestBO openAIRequestBO = new OpenAIRequestBO();
        openAIRequestBO.setModel(module);
        openAIRequestBO.setMessages(List.of(new OpenAIRequestBO.OpenAIRequestMessage("user", textContent)));

        ObjectMapper objectMapper1 = new ObjectMapper();
        String jsonInputString = objectMapper1.writeValueAsString(openAIRequestBO);

        System.out.println("jsonInputString: " + jsonInputString);

        URI uri = URI.create(url);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer "+apiKey);
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000); // 连接超时：10秒
        connection.setReadTimeout(10000); // 读取超时：10秒

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }

    public static String getChatCompletion(String urlStr, String module, String apiKey) throws Exception {
        // Set up the URL connection
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);

        // Create JSON request body
        String jsonInputString = """
                {
                    "model": "gpt-4o-mini",
                    "messages": [
                        {
                            "role": "system",
                            "content": "You are a helpful assistant."
                        },
                        {
                            "role": "user",
                            "content": "Write a haiku that explains the concept of recursion."
                        }
                    ]
                }
                """;

        // Send request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Get the response
        int status = connection.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = reader.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } else {
            throw new RuntimeException("Request failed with status code: " + status);
        }
    }
}
