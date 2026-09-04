package com.pay.sky.util;

import android.content.Context;
import com.pay.sky.api.ApiClient;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.DataPayloadConfig;
import com.pay.sky.data.QueuedMessage;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentGatewayDispatcher {

    private static final ExecutorService DISPATCH_EXECUTOR = Executors.newSingleThreadExecutor();

    public static void dispatch(Context context, SmsModel sms) {
        if (sms == null) {
            return;
        }

        SessionManager.init(context);
        if (!SessionManager.getInstance().isLoggedIn()) {
            return;
        }

        PreferencesManager prefs = PreferencesManager.getInstance();
        if (prefs.isPaused()) {
            return;
        }

        final Context appContext = context.getApplicationContext();
        DISPATCH_EXECUTOR.execute(() -> {
            try {
                ApiClient.forwardSmsToMainServer(appContext, sms);
            } catch (Exception ignored) {
            }

            if (prefs.isWebhookEnabled()) {
                dispatchToExternalWebhook(appContext, sms);
            }
        });
    }

    private static void dispatchToExternalWebhook(Context context, SmsModel sms) {
        PreferencesManager prefs = PreferencesManager.getInstance();
        String webhookUrl = prefs.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.trim().isEmpty() || !webhookUrl.startsWith("https://")) {
            return;
        }

        final String token = prefs.getWebhookSecret();
        final long smsId = sms.getId();
        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        DataPayloadConfig config = db.getDataPayloadConfig();

        try {
            String payload = buildExternalPayload(sms, config);
            Map<String, String> headers = config.parseCustomHeaders();
            String contentType = "application/x-www-form-urlencoded".equalsIgnoreCase(config.getContentType())
                    ? "application/x-www-form-urlencoded" : "application/json; charset=UTF-8";

            ApiClient.executeHttpRequest(webhookUrl, config.getHttpMethod(), contentType, token, headers, payload);
            db.updateWebhookStatus(smsId, "delivered");
        } catch (Exception e) {
            db.updateWebhookStatus(smsId, "failed");

            QueuedMessage qm = new QueuedMessage();
            qm.setSmsId(sms.getSmsId() != null ? sms.getSmsId() : String.valueOf(sms.getId()));
            qm.setSender(sms.getSender());
            qm.setBody(sms.getBody());
            qm.setTimestamp(sms.getTimestamp());
            qm.setSimSlot(sms.getSimSlot());
            qm.setSubId(sms.getSubId());
            qm.setAttempts(1);
            qm.setLastAttempt(System.currentTimeMillis());
            qm.setErrorReason(e.getMessage() != null ? e.getMessage() : "Dispatch failed");
            db.insertQueuedMessage(qm);
        }

        QueueDispatcher.retryQueueAsync(context, null);
    }

    private static String buildExternalPayload(SmsModel sms, DataPayloadConfig config) throws Exception {
        if ("application/x-www-form-urlencoded".equalsIgnoreCase(config.getContentType())) {
            StringBuilder sb = new StringBuilder();
            if (config.isIncludeSender()) {
                appendParam(sb, config.getKeySender(), sms.getSender());
            }
            if (config.isIncludeBody()) {
                appendParam(sb, config.getKeyBody(), sms.getBody());
            }
            if (config.isIncludeTimestamp()) {
                appendParam(sb, config.getKeyTimestamp(), String.valueOf(sms.getTimestamp()));
            }
            if (config.isIncludeSimSlot()) {
                appendParam(sb, config.getKeySimSlot(), String.valueOf(sms.getSimSlot()));
            }
            if (config.isIncludeDeviceId()) {
                appendParam(sb, config.getKeyDeviceId(), SessionManager.getInstance().getDeviceId());
            }
            Map<String, String> customFields = config.parseCustomFields();
            for (Map.Entry<String, String> entry : customFields.entrySet()) {
                appendParam(sb, entry.getKey(), entry.getValue());
            }
            return sb.toString();
        } else {
            JSONObject obj = new JSONObject();
            if (config.isIncludeSender()) {
                obj.put(config.getKeySender(), sms.getSender());
            }
            if (config.isIncludeBody()) {
                obj.put(config.getKeyBody(), sms.getBody());
            }
            if (config.isIncludeTimestamp()) {
                obj.put(config.getKeyTimestamp(), sms.getTimestamp());
            }
            if (config.isIncludeSimSlot()) {
                obj.put(config.getKeySimSlot(), sms.getSimSlot());
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

    private static void appendParam(StringBuilder sb, String key, String val) throws Exception {
        if (sb.length() > 0) {
            sb.append("&");
        }
        sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
        sb.append("=");
        sb.append(URLEncoder.encode(val != null ? val : "", StandardCharsets.UTF_8.name()));
    }
}
