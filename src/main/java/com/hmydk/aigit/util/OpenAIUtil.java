package com.hmydk.aigit.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.pojo.OpenAIRequestBO;

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
        connection.setConnectTimeout(10000); // 连接超时：10秒
        connection.setReadTimeout(10000); // 读取超时：10秒

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }

    public static void getAIResponseStream(String client, String textContent, Consumer<String> onNext) throws Exception {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        String selectedModule = settings.getSelectedModule();
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(client);

        HttpURLConnection connection = OpenAIUtil.getHttpURLConnection(moduleConfig.getUrl(), selectedModule,
                moduleConfig.getApiKey(), textContent);

        // 获取响应的字符集
        String charset = getCharsetFromContentType(connection.getContentType());
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6);
                    if (!"[DONE]".equals(jsonData)) {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root = mapper.readTree(jsonData);
                        JsonNode choices = root.path("choices");
                        if (choices.isArray() && !choices.isEmpty()) {
                            String text = choices.get(0).path("delta").path("content").asText();
                            onNext.accept(text);
                        }
                    }
                }
            }
        }
    }

    private static String getCharsetFromContentType(String contentType) {
        if (contentType != null) {
            String[] values = contentType.split(";");
            for (String value : values) {
                value = value.trim();
                if (value.toLowerCase().startsWith("charset=")) {
                    return value.substring("charset=".length());
                }
            }
        }
        return StandardCharsets.UTF_8.name(); // 默认使用UTF-8
    }
}
