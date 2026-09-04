package com.pay.sky.util;

import android.content.Context;
import com.pay.sky.api.ApiClient;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.DataPayloadConfig;
import com.pay.sky.data.QueuedMessage;
import com.pay.sky.data.SmsDatabaseHelper;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QueueDispatcher {

    private static final ExecutorService QUEUE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static volatile boolean isRetrying = false;

    public interface QueueRetryListener {
        void onCompleted(int successCount, int failedCount);
    }

    public static void retryQueueAsync(Context context, QueueRetryListener listener) {
        if (isRetrying) {
            return;
        }

        PreferencesManager prefs = PreferencesManager.getInstance();
        if (prefs.isPaused() || !prefs.isWebhookEnabled()) {
            if (listener != null) {
                listener.onCompleted(0, 0);
            }
            return;
        }

        SessionManager.init(context);
        if (!SessionManager.getInstance().isLoggedIn()) {
            if (listener != null) {
                listener.onCompleted(0, 0);
            }
            return;
        }

        String webhookUrl = prefs.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.trim().isEmpty() || !webhookUrl.startsWith("https://")) {
            if (listener != null) {
                listener.onCompleted(0, 0);
            }
            return;
        }

        isRetrying = true;
        QUEUE_EXECUTOR.execute(() -> {
            int success = 0;
            int failed = 0;
            try {
                SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
                List<QueuedMessage> list = db.getQueuedMessages();
                DataPayloadConfig config = db.getDataPayloadConfig();
                String token = prefs.getWebhookSecret();

                for (QueuedMessage qm : list) {
                    if (prefs.isPaused()) {
                        break;
                    }
                    try {
                        String payload = buildPayload(qm, config);
                        Map<String, String> headers = config.parseCustomHeaders();
                        String contentType = "application/x-www-form-urlencoded".equalsIgnoreCase(config.getContentType())
                                ? "application/x-www-form-urlencoded" : "application/json; charset=UTF-8";

                        ApiClient.executeHttpRequest(webhookUrl, config.getHttpMethod(), contentType, token, headers, payload);
                        db.deleteQueuedMessage(qm.getId());
                        success++;
                    } catch (Exception e) {
                        failed++;
                        db.updateQueuedMessageAttempt(qm.getId(), qm.getAttempts() + 1, e.getMessage());
                    }
                }
            } finally {
                isRetrying = false;
                if (listener != null) {
                    final int s = success;
                    final int f = failed;
                    listener.onCompleted(s, f);
                }
            }
        });
    }

    private static String buildPayload(QueuedMessage qm, DataPayloadConfig config) throws Exception {
        if ("application/x-www-form-urlencoded".equalsIgnoreCase(config.getContentType())) {
            StringBuilder sb = new StringBuilder();
            if (config.isIncludeSender()) {
                appendFormParam(sb, config.getKeySender(), qm.getSender());
            }
            if (config.isIncludeBody()) {
                appendFormParam(sb, config.getKeyBody(), qm.getBody());
            }
            if (config.isIncludeTimestamp()) {
                appendFormParam(sb, config.getKeyTimestamp(), String.valueOf(qm.getTimestamp()));
            }
            if (config.isIncludeSimSlot()) {
                appendFormParam(sb, config.getKeySimSlot(), String.valueOf(qm.getSimSlot()));
            }
            if (config.isIncludeDeviceId()) {
                appendFormParam(sb, config.getKeyDeviceId(), SessionManager.getInstance().getDeviceId());
            }
            Map<String, String> customFields = config.parseCustomFields();
            for (Map.Entry<String, String> entry : customFields.entrySet()) {
                appendFormParam(sb, entry.getKey(), entry.getValue());
            }
            return sb.toString();
        } else {
            JSONObject obj = new JSONObject();
            if (config.isIncludeSender()) {
                obj.put(config.getKeySender(), qm.getSender());
            }
            if (config.isIncludeBody()) {
                obj.put(config.getKeyBody(), qm.getBody());
            }
            if (config.isIncludeTimestamp()) {
                obj.put(config.getKeyTimestamp(), qm.getTimestamp());
            }
            if (config.isIncludeSimSlot()) {
                obj.put(config.getKeySimSlot(), qm.getSimSlot());
            }
            if (config.isIncludeDeviceId()) {
                obj.put(config.getKeyDeviceId(), SessionManager.getInstance().getDeviceId());
            }
            Map<String, String> customFields = config.parseCustomFields();
            for (Map.Entry<String, String> entry : customFields.entrySet()) {
                obj.put(entry.getKey(), entry.getValue());
            }
            return obj.toString();
        }
    }

    private static void appendFormParam(StringBuilder sb, String key, String val) throws Exception {
        if (sb.length() > 0) {
            sb.append("&");
        }
        sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
        sb.append("=");
        sb.append(URLEncoder.encode(val != null ? val : "", StandardCharsets.UTF_8.name()));
    }
}
