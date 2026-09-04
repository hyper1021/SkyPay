package com.pay.sky.api;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.pay.sky.data.KeyValuePair;
import com.pay.sky.data.SmsModel;
import com.pay.sky.util.PayloadBuilder;
import com.pay.sky.util.PreferencesManager;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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

    public interface PingCallback {
        void onResult(boolean success, String description, boolean requireLogout);
    }

    public static void login(String email, String deviceKey, ApiCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                JSONObject requestJson = new JSONObject();
                requestJson.put("email", email);
                requestJson.put("device_key", deviceKey);
                requestJson.put("device_token", deviceKey);
                requestJson.put("app_version", "1.0.0");
                requestJson.put("platform", "android");
                requestJson.put("android_version", Build.VERSION.RELEASE);
                requestJson.put("sdk_int", Build.VERSION.SDK_INT);
                requestJson.put("model", Build.MODEL);
                requestJson.put("manufacturer", Build.MANUFACTURER);

                String endpoint = ApiConfig.DEFAULT_BASE_URL + ApiConfig.LOGIN_ENDPOINT;
                String responseStr = executePost(endpoint, null, requestJson.toString());

                JSONObject responseJson = new JSONObject(responseStr);
                boolean success = responseJson.optBoolean("success", responseJson.optBoolean("ok", false));
                if (success || responseJson.has("token") || responseJson.has("device_token")) {
                    MAIN_HANDLER.post(() -> callback.onSuccess(responseJson));
                } else {
                    String message = responseJson.optString("message", responseJson.optString("error", "Invalid email or password."));
                    MAIN_HANDLER.post(() -> callback.onError(message));
                }
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Network communication error";
                MAIN_HANDLER.post(() -> callback.onError(message));
            }
        });
    }

    public static void login(String email, String deviceKey, String deviceId, ApiCallback callback) {
        login(email, deviceKey, callback);
    }

    public static void pingDevice(String token, String deviceId, String email, PingCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                if (token == null || token.trim().isEmpty()) {
                    MAIN_HANDLER.post(() -> callback.onResult(true, "", false));
                    return;
                }

                JSONObject requestJson = new JSONObject();
                requestJson.put("device_token", token);
                requestJson.put("device_id", deviceId);
                requestJson.put("email", email);
                requestJson.put("app_version", "1.0.0");
                requestJson.put("android_version", Build.VERSION.RELEASE);
                requestJson.put("sdk_int", Build.VERSION.SDK_INT);
                requestJson.put("model", Build.MODEL);
                requestJson.put("manufacturer", Build.MANUFACTURER);

                String endpoint = ApiConfig.DEFAULT_BASE_URL + ApiConfig.PING_ENDPOINT;
                String responseStr = executePost(endpoint, token, requestJson.toString());

                JSONObject resJson = new JSONObject(responseStr);
                boolean ok = resJson.optBoolean("ok", resJson.optBoolean("success", true));
                String desc = resJson.optString("description", resJson.optString("message", ""));
                boolean logout = resJson.optBoolean("logout", false);

                MAIN_HANDLER.post(() -> callback.onResult(ok, desc, logout));
            } catch (Exception e) {
                MAIN_HANDLER.post(() -> callback.onResult(true, "", false));
            }
        });
    }

    public static String forwardSmsToMainServer(Context context, SmsModel sms) throws Exception {
        SessionManager session = SessionManager.getInstance();
        if (!session.isLoggedIn()) {
            throw new Exception("User not logged in");
        }

        JSONObject requestJson = new JSONObject();
        requestJson.put("device_token", session.getToken());
        requestJson.put("device_id", session.getDeviceId());
        requestJson.put("email", session.getUserEmail());
        requestJson.put("sms_id", sms.getSmsId() != null ? sms.getSmsId() : String.valueOf(sms.getId()));
        requestJson.put("sender", sms.getSender());
        requestJson.put("body", sms.getBody());
        requestJson.put("timestamp", sms.getTimestamp());
        requestJson.put("sim_slot", sms.getSimSlot());
        requestJson.put("sub_id", sms.getSubId());
        requestJson.put("service_center", sms.getServiceCenter());
        requestJson.put("sms_type", sms.getSmsType());
        requestJson.put("app_version", "1.0.0");
        requestJson.put("android_version", Build.VERSION.RELEASE);
        requestJson.put("sdk_int", Build.VERSION.SDK_INT);
        requestJson.put("model", Build.MODEL);
        requestJson.put("manufacturer", Build.MANUFACTURER);

        String endpoint = ApiConfig.DEFAULT_BASE_URL + ApiConfig.SMS_DATA_ENDPOINT;
        return executePost(endpoint, session.getToken(), requestJson.toString());
    }

    public static void testWebhook(String webhookUrl, String secret, ApiCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                if (webhookUrl == null || !webhookUrl.startsWith("https://")) {
                    MAIN_HANDLER.post(() -> callback.onError("Webhook URL must use secure HTTPS."));
                    return;
                }

                PreferencesManager prefs = PreferencesManager.getInstance();
                String method = prefs.getHttpMethod();
                String contentType = prefs.getPayloadContentType();
                String deviceId = SessionManager.getInstance().getDeviceId();
                List<KeyValuePair> headerRows = prefs.getHeaderRows();
                List<KeyValuePair> bodyRows = prefs.getPostBodyRows();

                SmsModel dummySms = new SmsModel();
                dummySms.setId(1);
                dummySms.setSender("SkyPay Test");
                dummySms.setBody("Webhook connection test");
                dummySms.setTimestamp(System.currentTimeMillis());
                dummySms.setSimSlot(1);

                Map<String, String> headers = PayloadBuilder.buildHeaders(headerRows, dummySms, contentType, secret, deviceId);
                String payload = PayloadBuilder.buildPayload(bodyRows, contentType, dummySms, secret, deviceId);

                String requestUrl = webhookUrl;
                String requestBody = payload;
                if ("GET".equalsIgnoreCase(method)) {
                    String queryString = PayloadBuilder.buildQueryString(bodyRows, dummySms, contentType, secret, deviceId);
                    if (queryString != null && !queryString.isEmpty()) {
                        requestUrl += (requestUrl.contains("?") ? "&" : "?") + queryString;
                    }
                    requestBody = null;
                }

                String contentTypeHeader = PayloadBuilder.isFormUrlEncoded(contentType)
                        ? "application/x-www-form-urlencoded" : "application/json; charset=UTF-8";

                executeHttpRequest(requestUrl, method, contentTypeHeader, secret, headers, requestBody);
                JSONObject responseJson = new JSONObject();
                responseJson.put("success", true);
                responseJson.put("status", 200);
                MAIN_HANDLER.post(() -> callback.onSuccess(responseJson));
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Connection failed";
                MAIN_HANDLER.post(() -> callback.onError(msg));
            }
        });
    }

    public static String executePost(String urlString, String bearerToken, String jsonBody) throws Exception {
        return executeHttpRequest(urlString, "POST", "application/json; charset=UTF-8", bearerToken, null, jsonBody);
    }

    public static String executeHttpRequest(String urlString, String method, String contentType, String bearerToken, Map<String, String> customHeaders, String body) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn;
        if (urlString.startsWith("https://")) {
            conn = (HttpsURLConnection) url.openConnection();
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }

        conn.setRequestMethod(method != null ? method.toUpperCase() : "POST");
        conn.setConnectTimeout(ApiConfig.CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(ApiConfig.READ_TIMEOUT_MS);

        if (contentType != null && !contentType.isEmpty()) {
            conn.setRequestProperty("Content-Type", contentType);
        }
        conn.setRequestProperty("Accept", "application/json, text/plain, */*");

        boolean hasCustomAuth = false;
        if (customHeaders != null) {
            for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().trim().isEmpty()) {
                    String k = entry.getKey().trim();
                    String v = entry.getValue() != null ? entry.getValue() : "";
                    if ("authorization".equalsIgnoreCase(k)) {
                        hasCustomAuth = true;
                    }
                    conn.setRequestProperty(k, v);
                }
            }
        }

        if (!hasCustomAuth && bearerToken != null && !bearerToken.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken.trim());
        }

        boolean hasBody = !"GET".equalsIgnoreCase(method) && body != null && !body.isEmpty();
        if (hasBody) {
            conn.setDoOutput(true);
            byte[] outputBytes = body.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(outputBytes);
                os.flush();
            }
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
