package com.hmydk.aigit.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmydk.aigit.pojo.GeminiRequestBO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * TestUtil
 *
 * @author hmydk
 */
public class TestUtil {

    public static void main(String[] args) throws Exception {
        String apiKey  = "";
        String textContent = "hi";
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
        GeminiRequestBO geminiRequestBO = new GeminiRequestBO();
        geminiRequestBO.setContents(List.of(new GeminiRequestBO.Content(List.of(new GeminiRequestBO.Part(textContent)))));
        ObjectMapper objectMapper1 = new ObjectMapper();
        String jsonInputString = objectMapper1.writeValueAsString(geminiRequestBO);

        URI uri = URI.create(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000); // 连接超时：10秒
        connection.setReadTimeout(10000); // 读取超时：10秒

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        System.out.println(response);
    }
}
