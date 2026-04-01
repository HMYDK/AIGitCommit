package com.hmydk.aigit.util;

import com.hmydk.aigit.config.ApiKeySettings;

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
}
