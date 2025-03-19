package com.hmydk.aigit.service;

import com.hmydk.aigit.util.OpenAIUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.net.HttpURLConnection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AIService
 *
 * @author hmydk
 */
public interface AIService {

    boolean generateByStream();

    String generateCommitMessage(String content) throws Exception;

    void generateCommitMessageStream(String content, Consumer<String> onNext, Consumer<Throwable> onError, Runnable onComplete) throws Exception;

    boolean checkNecessaryModuleConfigIsRight();


    default Pair<Boolean, String> validateConfig(Map<String, String> config) {
        HttpURLConnection connection = null;
        try {
            connection = OpenAIUtil.getHttpURLConnection(config.get("url"), config.get("module"), config.get("apiKey"), "hi");
            if (connection.getResponseCode() != 200) {
                // 读取错误响应
                StringBuilder response = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getErrorStream()))) {
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
}
