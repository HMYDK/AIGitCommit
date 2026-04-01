package com.hmydk.aigit.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.pojo.OpenAIRequestBO;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

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
import java.util.function.Consumer;

public class OpenAIUtil {

    private static final String THINK_START_TAG = "<think>";
    private static final String THINK_END_TAG = "</think>";

    public static boolean checkNecessaryModuleConfigIsRight(String client) {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(client);
        if (moduleConfig == null) {
            return false;
        }
        String selectedModule = settings.getSelectedModule();
        String url = moduleConfig.getUrl();
        String apiKey = moduleConfig.getApiKey();
        if (Constants.OpenAI_Compatible.equals(client)) {
            return StringUtils.isNotEmpty(url) && StringUtils.isNotEmpty(apiKey)
                    && StringUtils.isNotEmpty(moduleConfig.getModelId());
        }
        return StringUtils.isNotEmpty(selectedModule) && StringUtils.isNotEmpty(url) && StringUtils.isNotEmpty(apiKey);
    }

    public static @NotNull HttpURLConnection getHttpURLConnection(String url, String module, String apiKey,
            String textContent) throws IOException {
        OpenAIRequestBO openAIRequestBO = new OpenAIRequestBO();
        openAIRequestBO.setModel(module);
        openAIRequestBO.setStream(true);
        openAIRequestBO.setMessages(List.of(new OpenAIRequestBO.OpenAIRequestMessage("user", textContent)));

        ObjectMapper objectMapper1 = new ObjectMapper();
        String jsonInputString = objectMapper1.writeValueAsString(openAIRequestBO);

        URI uri = URI.create(url);
        Proxy proxy = CommonUtil.getProxy(uri);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(proxy);
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

    public static void getAIResponseStream(String client, String content, Consumer<String> onNext,
            Consumer<Throwable> onError, Runnable onComplete) throws Exception {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        String selectedModule = settings.getSelectedModule();
        ApiKeySettings.ModuleConfig moduleConfig = settings.getModuleConfigs().get(client);
        String url = moduleConfig.getUrl();
        String apiKey = moduleConfig.getApiKey();

        String modelToUse = selectedModule;
        if (Constants.OpenAI_Compatible.equals(client) && moduleConfig.getModelId() != null
                && !moduleConfig.getModelId().trim().isEmpty()) {
            modelToUse = moduleConfig.getModelId();
        }

        HttpURLConnection connection = getHttpURLConnection(url, modelToUse, apiKey, content);
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
            throw new Exception(response.toString());
        }

        // 获取响应的字符集
        String charset = CommonUtil.getCharsetFromContentType(connection.getContentType());

        // 用于过滤 <think>...</think> 标签
        StringBuilder thinkBuffer = new StringBuilder();
        boolean[] insideThinkTag = { false };

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6);
                    if ("[DONE]".equals(jsonData)) {
                        // 处理剩余的缓冲区内容
                        if (!insideThinkTag[0] && thinkBuffer.length() > 0) {
                            onNext.accept(thinkBuffer.toString());
                        }
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
                                    // 过滤 <think>...</think> 标签内容
                                    processAndFilterThinkTags(tokenContent, thinkBuffer, insideThinkTag, onNext);
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

    /**
     * 过滤流式响应中的 <think>...</think> 标签内容
     */
    private static void processAndFilterThinkTags(String tokenContent, StringBuilder thinkBuffer,
            boolean[] insideThinkTag, Consumer<String> onNext) {
        thinkBuffer.append(tokenContent);
        String buffered = thinkBuffer.toString();

        while (true) {
            if (insideThinkTag[0]) {
                // 在 <think> 标签内部，查找结束标签
                int endIdx = buffered.indexOf(THINK_END_TAG);
                if (endIdx != -1) {
                    // 找到结束标签，跳过思考内容
                    buffered = buffered.substring(endIdx + THINK_END_TAG.length());
                    insideThinkTag[0] = false;
                } else {
                    // 未找到结束标签，继续等待更多内容
                    break;
                }
            } else {
                // 不在 <think> 标签内，查找开始标签
                int startIdx = buffered.indexOf(THINK_START_TAG);
                if (startIdx != -1) {
                    // 找到开始标签，输出标签前的内容
                    String beforeThink = buffered.substring(0, startIdx);
                    if (!beforeThink.isEmpty()) {
                        onNext.accept(beforeThink);
                    }
                    buffered = buffered.substring(startIdx + THINK_START_TAG.length());
                    insideThinkTag[0] = true;
                } else {
                    // 没有找到开始标签，但需要保留可能的不完整标签
                    int safeLength = buffered.length() - THINK_START_TAG.length() + 1;
                    if (safeLength > 0) {
                        onNext.accept(buffered.substring(0, safeLength));
                        buffered = buffered.substring(safeLength);
                    }
                    break;
                }
            }
        }

        // 更新缓冲区
        thinkBuffer.setLength(0);
        thinkBuffer.append(buffered);
    }
}
