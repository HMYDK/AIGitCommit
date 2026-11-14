package com.hmydk.aigit.util;

import com.hmydk.aigit.config.ApiKeySettings;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class CommonUtil {

    public static String getCharsetFromContentType(String contentType) {
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

    public static Proxy getProxy(URI uri) {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        if (settings.isUseSystemProxy()) {
            return ProxySelector.getDefault().select(uri).stream()
                    .findFirst()
                    .orElse(Proxy.NO_PROXY);
        }
        return Proxy.NO_PROXY;
    }

    /**
     * 应用用户配置的请求超时（连接超时与读取超时相同），issue #96。
     *
     * <p>所有自行建立 {@link HttpURLConnection} 的 provider 都通过这里读取设置，
     * 避免各 provider 继续使用各自硬编码的超时值。</p>
     */
    public static void applyRequestTimeout(HttpURLConnection connection) {
        int timeoutMillis = ApiKeySettings.getInstance().getRequestTimeoutMillis();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis);
    }
}
