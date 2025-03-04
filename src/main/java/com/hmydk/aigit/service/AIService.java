package com.hmydk.aigit.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.function.Consumer;

import com.hmydk.aigit.util.OpenAIUtil;

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


    default boolean validateConfig(Map<String, String> config) {
        int statusCode;
        try {
            HttpURLConnection connection = OpenAIUtil.getHttpURLConnection(config.get("url"), config.get("module"), config.get("apiKey"), "hi");
            statusCode = connection.getResponseCode();
        } catch (IOException e) {
            return false;
        }
        // 打印状态码
//        System.out.println("HTTP Status Code: " + statusCode);
        return statusCode == 200;
    }
}
