package com.hmydk.aigit.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.pojo.OpenAIRequestBO;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

public class OpenAIUtil {

    public static boolean checkNecessaryModuleConfigIsRight(String client) {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(client);
        if (moduleConfig == null) {
            return false;
        }
        String selectedModule = settings.getSelectedModule();
        String url = moduleConfig.getUrl();
        String apiKey = moduleConfig.getApiKey();
        return StringUtils.isNotEmpty(selectedModule) && StringUtils.isNotEmpty(url) && StringUtils.isNotEmpty(apiKey);
    }

    public static @NotNull HttpURLConnection getHttpURLConnection(String url, String module, String apiKey, String textContent) throws IOException {
        OpenAIRequestBO openAIRequestBO = new OpenAIRequestBO();
        openAIRequestBO.setModel(module);
        openAIRequestBO.setStream(true);
        openAIRequestBO.setMessages(List.of(new OpenAIRequestBO.OpenAIRequestMessage("user", textContent)));

        ObjectMapper objectMapper1 = new ObjectMapper();
        String jsonInputString = objectMapper1.writeValueAsString(openAIRequestBO);

        URI uri = URI.create(url);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);
        connection.setConnectTimeout(30000); // 连接超时：30秒
        connection.setReadTimeout(30000); // 读取超时：30秒

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }

    public static void getAIResponseStream(String client, String content, Consumer<String> onNext, Consumer<Throwable> onError, Runnable onComplete) throws Exception {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        String selectedModule = settings.getSelectedModule();
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(client);
        String url = moduleConfig.getUrl();
        String apiKey = moduleConfig.getApiKey();

        HttpURLConnection connection = getHttpURLConnection(url, selectedModule, apiKey, content);
        if (connection.getResponseCode() != 200) {
            // 读取错误响应
            StringBuilder response = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            throw new Exception(response.toString());
        }

        // 获取响应的字符集
        String charset = CommonUtil.getCharsetFromContentType(connection.getContentType());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6);
                    if ("[DONE]".equals(jsonData)) {
                        if (onComplete != null) {
                            onComplete.run();
                        }
                        break;
                    }
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode rootNode = objectMapper.readTree(jsonData);
                        JsonNode choices = rootNode.get("choices");
                        if (choices != null && choices.isArray() && choices.size() > 0) {
                            JsonNode delta = choices.get(0).get("delta");
                            if (delta != null && delta.has("content")) {
                                JsonNode contentNode = delta.get("content");
                                if (contentNode != null && !contentNode.isNull()) {
                                    String tokenContent = contentNode.asText();
                                    onNext.accept(tokenContent);
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (onError != null) {
                            onError.accept(e);
                        }
                    }
                }
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
}
