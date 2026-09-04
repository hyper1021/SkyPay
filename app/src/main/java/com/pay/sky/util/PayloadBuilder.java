package com.pay.sky.util;

import com.pay.sky.data.KeyValuePair;
import com.pay.sky.data.SmsModel;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PayloadBuilder {

    public static String buildPayload(List<KeyValuePair> rows, String contentType, SmsModel sms, String authToken, String deviceId) {
        if (rows == null || rows.isEmpty()) {
            return isFormUrlEncoded(contentType) ? "" : "{}";
        }

        if (isFormUrlEncoded(contentType)) {
            return buildFormUrlEncoded(rows, sms, contentType, authToken, deviceId);
        } else {
            return buildJson(rows, sms, contentType, authToken, deviceId);
        }
    }

    public static String buildQueryString(List<KeyValuePair> rows, SmsModel sms, String contentType, String authToken, String deviceId) {
        return buildFormUrlEncoded(rows, sms, contentType, authToken, deviceId);
    }

    public static Map<String, String> buildHeaders(List<KeyValuePair> rows, SmsModel sms, String contentType, String authToken, String deviceId) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rows == null) {
            return map;
        }

        for (KeyValuePair pair : rows) {
            if (pair == null) continue;
            String key = pair.getKey().trim();
            if (key.isEmpty()) continue;
            String value = UnifiedPlaceholderResolver.resolve(pair.getValue(), sms, contentType, authToken, deviceId);
            map.put(key, value);
        }
        return map;
    }

    private static String buildJson(List<KeyValuePair> rows, SmsModel sms, String contentType, String authToken, String deviceId) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;

        for (KeyValuePair pair : rows) {
            if (pair == null) continue;
            String key = pair.getKey().trim();
            if (key.isEmpty()) continue;

            String rawVal = pair.getValue();
            String resolvedVal = UnifiedPlaceholderResolver.resolve(rawVal, sms, contentType, authToken, deviceId);

            if (!first) {
                sb.append(",");
            }
            first = false;

            sb.append(JSONObject.quote(key));
            sb.append(":");
            sb.append(JSONObject.quote(resolvedVal));
        }

        sb.append("}");
        return sb.toString();
    }

    private static String buildFormUrlEncoded(List<KeyValuePair> rows, SmsModel sms, String contentType, String authToken, String deviceId) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (KeyValuePair pair : rows) {
            if (pair == null) continue;
            String key = pair.getKey().trim();
            if (key.isEmpty()) continue;

            String resolvedVal = UnifiedPlaceholderResolver.resolve(pair.getValue(), sms, contentType, authToken, deviceId);

            if (!first) {
                sb.append("&");
            }
            first = false;

            try {
                sb.append(URLEncoder.encode(key, "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(resolvedVal, "UTF-8"));
            } catch (Exception e) {
                sb.append(key).append("=").append(resolvedVal);
            }
        }

        return sb.toString();
    }

    public static boolean isFormUrlEncoded(String contentType) {
        if (contentType == null) return false;
        String lower = contentType.toLowerCase();
        return lower.contains("form") || lower.contains("urlencoded");
    }
}
