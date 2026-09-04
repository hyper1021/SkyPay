package com.pay.sky.api;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
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
        void onResult(boolean ok, String description, boolean logout);
    }

    public static void login(String email, String deviceKey, ApiCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                JSONObject requestJson = new JSONObject();
                requestJson.put("email", email);
                requestJson.put("device_key", deviceKey);
                requestJson.put("app_version", "1.0.0");
                requestJson.put("platform", "android");
                requestJson.put("device_model", Build.MANUFACTURER + " " + Build.MODEL);
                requestJson.put("android_version", Build.VERSION.RELEASE);

                String endpoint = ApiConfig.DEFAULT_BASE_URL + ApiConfig.LOGIN_ENDPOINT;
                String responseStr = executeHttp(endpoint, "POST", null, null, "application/json", requestJson.toString());

                JSONObject responseJson = new JSONObject(responseStr);
                boolean success = responseJson.optBoolean("success", true);
                if (success) {
                    MAIN_HANDLER.post(() -> callback.onSuccess(responseJson));
                } else {
                    String msg = responseJson.optString("message", "Invalid email or device key.");
                    MAIN_HANDLER.post(() -> callback.onError(msg));
                }
            } catch (SocketTimeoutException e) {
                MAIN_HANDLER.post(() -> callback.onError("Connection timed out. Please try again."));
            } catch (UnknownHostException | ConnectException e) {
                MAIN_HANDLER.post(() -> callback.onError("No internet connection."));
            } catch (HttpException e) {
                String errorMsg = "Invalid email or device key.";
                if (e.getStatusCode() == 401 || e.getStatusCode() == 422 || e.getStatusCode() == 403) {
                    if (e.getResponseBody() != null && !e.getResponseBody().isEmpty()) {
                        try {
                            JSONObject errJson = new JSONObject(e.getResponseBody());
                            if (errJson.has("message")) {
                                errorMsg = errJson.getString("message");
                            } else if (errJson.has("description")) {
                                errorMsg = errJson.getString("description");
                            }
                        } catch (Exception ignored) {
                        }
                    }
                } else if (e.getStatusCode() >= 500) {
                    errorMsg = "Server error (" + e.getStatusCode() + "). Please try again later.";
                }
                final String finalMsg = errorMsg;
                MAIN_HANDLER.post(() -> callback.onError(finalMsg));
            } catch (Exception e) {
                MAIN_HANDLER.post(() -> callback.onError("Invalid email or device key."));
            }
        });
    }

    public static void pingDevice(String token, PingCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("device_token", token);
                body.put("device_model", Build.MANUFACTURER + " " + Build.MODEL);
                body.put("android_version", Build.VERSION.RELEASE);
                body.put("sdk_level", Build.VERSION.SDK_INT);
                body.put("app_version", "1.0.0");

                String endpoint = ApiConfig.DEFAULT_BASE_URL + ApiConfig.PING_ENDPOINT;
                String responseStr = executeHttp(endpoint, "POST", token, null, "application/json", body.toString());

                JSONObject res = new JSONObject(responseStr);
                boolean ok = res.optBoolean("ok", true);
                String desc = res.optString("description", "");
                boolean logout = res.optBoolean("logout", false);

                MAIN_HANDLER.post(() -> callback.onResult(ok, desc, logout));
            } catch (HttpException e) {
                boolean ok = false;
                String desc = "Device authorization failed.";
                boolean logout = false;
                if (e.getResponseBody() != null && !e.getResponseBody().isEmpty()) {
                    try {
                        JSONObject errJson = new JSONObject(e.getResponseBody());
                        ok = errJson.optBoolean("ok", false);
                        desc = errJson.optString("description", "Device authorization notice");
                        logout = errJson.optBoolean("logout", e.getStatusCode() == 401 || e.getStatusCode() == 403);
                    } catch (Exception ignored) {
                    }
                } else if (e.getStatusCode() == 401 || e.getStatusCode() == 403) {
                    desc = "Device token expired or unauthorized.";
                    logout = true;
                }
                final boolean fOk = ok;
                final String fDesc = desc;
                final boolean fLogout = logout;
                MAIN_HANDLER.post(() -> callback.onResult(fOk, fDesc, fLogout));
            } catch (Exception e) {
                MAIN_HANDLER.post(() -> callback.onResult(true, "", false));
            }
        });
    }

    public static void sendToMainServer(String token, String jsonBody) throws Exception {
        String endpoint = ApiConfig.DEFAULT_BASE_URL + ApiConfig.SMS_DATA_ENDPOINT;
        executeHttp(endpoint, "POST", token, null, "application/json", jsonBody);
    }

    public static String executePost(String urlString, String bearerToken, String jsonBody) throws Exception {
        return executeHttp(urlString, "POST", bearerToken, null, "application/json", jsonBody);
    }

    public static String executeHttp(String urlString, String method, String bearerToken,
                                     Map<String, String> customHeaders, String contentType,
                                     String body) throws Exception {
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
            conn.setRequestProperty("Content-Type", contentType + "; charset=UTF-8");
        } else {
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        }
        conn.setRequestProperty("Accept", "application/json");

        if (bearerToken != null && !bearerToken.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken.trim());
            conn.setRequestProperty("DEVICE-TOKEN", bearerToken.trim());
        }

        if (customHeaders != null) {
            for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
        }

        if (!"GET".equalsIgnoreCase(method) && !"DELETE".equalsIgnoreCase(method) && body != null) {
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

        String responseBody = sb.toString().trim();
        if (statusCode >= 200 && statusCode < 300) {
            return responseBody.isEmpty() ? "{\"success\":true}" : responseBody;
        } else {
            throw new HttpException(statusCode, responseBody);
        }
    }

    public static class HttpException extends Exception {
        private final int statusCode;
        private final String responseBody;

        public HttpException(int statusCode, String responseBody) {
            super("HTTP " + statusCode + (responseBody != null ? ": " + responseBody : ""));
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }
}
