package com.hmydk.aigit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.service.AIService;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * OllamaService
 *
 * @author hmydk
 */
public class OllamaService implements AIService {
//    private static final Logger log = LoggerFactory.getLogger(OllamaService.class);
    @Override
    public String generateCommitMessage(String content) {
        String aiResponse;
        try {
            aiResponse = getAIResponse(content);
        } catch (Exception e) {
            return e.getMessage();
        }
//        log.info("aiResponse is  :\n{}", aiResponse);
        return aiResponse.replaceAll("```", "");
    }

    @Override
    public boolean checkApiKeyIsExists() {
        return true;
    }

    @Override
    public boolean validateConfig(String model, String apiKey, String language) {
        return true;
    }

    private static String getAIResponse(String textContent) throws Exception {
        HttpURLConnection connection = getHttpURLConnection(textContent);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        return jsonResponse.path("response").asText();
    }

    private static @NotNull HttpURLConnection getHttpURLConnection(String textContent) throws IOException {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        String selectedModule = settings.getSelectedModule();
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(Constants.Ollama);

        GenerateRequest request = new GenerateRequest(selectedModule, textContent, false);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonInputString = objectMapper.writeValueAsString(request);

        URI uri = URI.create(moduleConfig.getUrl());
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
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

    public static void main(String[] args) {
        OllamaService ollamaService = new OllamaService();
        String s = ollamaService.generateCommitMessage("你如何看待节假日调休这件事情？");
        System.out.println(s);
    }
}
