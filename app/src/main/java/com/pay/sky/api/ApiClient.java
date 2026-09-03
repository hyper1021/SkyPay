package com.pay.sky.api;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HttpsURLConnection;

public class ApiClient {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

    public static void login(String email, String deviceKey, ApiCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                JSONObject requestJson = new JSONObject();
                requestJson.put("email", email);
                requestJson.put("device_key", deviceKey);
                requestJson.put("app_version", "1.0.0");
                requestJson.put("platform", "android");

                String endpoint = ApiConfig.DEFAULT_BASE_URL + ApiConfig.LOGIN_ENDPOINT;
                String responseStr = executePost(endpoint, null, requestJson.toString());

                JSONObject responseJson = new JSONObject(responseStr);
                boolean success = responseJson.optBoolean("success", true);
                if (success) {
                    MAIN_HANDLER.post(() -> callback.onSuccess(responseJson));
                } else {
                    String msg = responseJson.optString("message", "Authentication failed");
                    MAIN_HANDLER.post(() -> callback.onError(msg));
                }
            } catch (Exception e) {
                String errorMsg = "Unable to connect to server. Please verify your connection.";
                MAIN_HANDLER.post(() -> callback.onError(errorMsg));
            }
        });
    }

    public static void testWebhook(String webhookUrl, String secret, ApiCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                if (!webhookUrl.startsWith("https://")) {
                    MAIN_HANDLER.post(() -> callback.onError("Webhook URL must use secure HTTPS."));
                    return;
                }

                JSONObject testPayload = new JSONObject();
                testPayload.put("schema_version", "1");
                testPayload.put("event", "webhook.test");
                testPayload.put("timestamp", System.currentTimeMillis());

                String responseStr = executePost(webhookUrl, secret, testPayload.toString());
                JSONObject responseJson;
                try {
                    responseJson = new JSONObject(responseStr);
                } catch (Exception ignored) {
                    responseJson = new JSONObject();
                    responseJson.put("success", true);
                    responseJson.put("message", "HTTP Connection OK");
                }
                final JSONObject result = responseJson;
                MAIN_HANDLER.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                final String msg = e.getMessage() != null ? e.getMessage() : "Connection failed";
                MAIN_HANDLER.post(() -> callback.onError(msg));
            }
        });
    }

    public static String executePost(String urlString, String bearerToken, String jsonBody) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn;
        if (urlString.startsWith("https://")) {
            conn = (HttpsURLConnection) url.openConnection();
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(ApiConfig.CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(ApiConfig.READ_TIMEOUT_MS);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        if (bearerToken != null && !bearerToken.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken.trim());
        }

        byte[] outputBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(outputBytes);
            os.flush();
        }

        int statusCode = conn.getResponseCode();
        InputStream is = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream();

        StringBuilder sb = new StringBuilder();
        if (is != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
        }

        conn.disconnect();

        if (statusCode >= 200 && statusCode < 300) {
            String res = sb.toString().trim();
            return res.isEmpty() ? "{\"success\":true}" : res;
        } else {
            throw new Exception("Server returned HTTP " + statusCode);
        }
    }
}
