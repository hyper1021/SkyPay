package com.pay.sky.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import com.pay.sky.api.ApiClient;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentGatewayDispatcher {

    private static final ExecutorService DISPATCH_EXECUTOR = Executors.newCachedThreadPool();

    public static void dispatch(Context context, SmsModel sms) {
        if (sms == null) {
            return;
        }

        SessionManager session = SessionManager.getInstance();
        if (session == null || !session.isLoggedIn()) {
            return;
        }

        PreferencesManager prefs = PreferencesManager.getInstance();
        if (prefs == null || prefs.isSystemPaused()) {
            return;
        }

        final Context appContext = context.getApplicationContext();

        dispatchToMainServer(appContext, sms);

        if (prefs.isWebhookEnabled()) {
            dispatchToExternalWebhook(appContext, sms);
        }
    }

    private static void dispatchToMainServer(Context context, SmsModel sms) {
        DISPATCH_EXECUTOR.execute(() -> {
            try {
                String token = SessionManager.getInstance().getToken();
                if (token == null || token.isEmpty()) {
                    return;
                }

                JSONObject body = new JSONObject();
                body.put("device_token", token);
                body.put("sender", sms.getSender());
                body.put("body", sms.getBody());
                body.put("sim_slot", sms.getSimSlot());
                body.put("timestamp", sms.getTimestamp());
                body.put("app_version", getAppVersion(context));
                body.put("android_version", Build.VERSION.RELEASE);
                body.put("sdk_level", Build.VERSION.SDK_INT);
                body.put("device_model", Build.MANUFACTURER + " " + Build.MODEL);

                ApiClient.sendToMainServer(token, body.toString());
            } catch (Exception ignored) {
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

        DISPATCH_EXECUTOR.execute(() -> {
            SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
            if (db == null) return;

            SmsDatabaseHelper.PayloadConfig config = db.getPayloadConfig();
            List<SmsDatabaseHelper.KeyValueItem> customHeadersList = db.getCustomHeaders();
            List<SmsDatabaseHelper.KeyValueItem> customFieldsList = db.getCustomFields();

            Map<String, String> headers = new HashMap<>();
            for (SmsDatabaseHelper.KeyValueItem item : customHeadersList) {
                headers.put(item.key, item.value);
            }

            String payloadString = "";
            try {
                if ("application/x-www-form-urlencoded".equalsIgnoreCase(config.contentType)) {
                    StringBuilder sb = new StringBuilder();
                    if (config.includeSender) appendParam(sb, config.keySender, sms.getSender());
                    if (config.includeBody) appendParam(sb, config.keyBody, sms.getBody());
                    if (config.includeTimestamp) appendParam(sb, config.keyTimestamp, String.valueOf(sms.getTimestamp()));
                    if (config.includeSimSlot) appendParam(sb, config.keySimSlot, String.valueOf(sms.getSimSlot()));
                    if (config.includeDeviceModel) appendParam(sb, config.keyDeviceModel, Build.MODEL);
                    if (config.includeAndroidVersion) appendParam(sb, config.keyAndroidVersion, Build.VERSION.RELEASE);
                    for (SmsDatabaseHelper.KeyValueItem f : customFieldsList) {
                        appendParam(sb, f.key, f.value);
                    }
                    payloadString = sb.toString();
                } else {
                    JSONObject payload = new JSONObject();
                    if (config.includeSender) payload.put(config.keySender, sms.getSender());
                    if (config.includeBody) payload.put(config.keyBody, sms.getBody());
                    if (config.includeTimestamp) payload.put(config.keyTimestamp, sms.getTimestamp());
                    if (config.includeSimSlot) payload.put(config.keySimSlot, sms.getSimSlot());
                    if (config.includeDeviceModel) payload.put(config.keyDeviceModel, Build.MODEL);
                    if (config.includeAndroidVersion) payload.put(config.keyAndroidVersion, Build.VERSION.RELEASE);
                    for (SmsDatabaseHelper.KeyValueItem f : customFieldsList) {
                        payload.put(f.key, f.value);
                    }
                    payloadString = payload.toString();
                }

                String targetUrl = webhookUrl;
                if ("GET".equalsIgnoreCase(config.httpMethod) && !payloadString.isEmpty()) {
                    targetUrl += (targetUrl.contains("?") ? "&" : "?") + payloadString;
                }

                ApiClient.executeHttp(targetUrl, config.httpMethod, token, headers, config.contentType,
                        "GET".equalsIgnoreCase(config.httpMethod) ? null : payloadString);

                db.updateWebhookStatus(smsId, "delivered");
            } catch (Exception e) {
                db.updateWebhookStatus(smsId, "queued");
                db.queueMessage(smsId, sms.getSender(), sms.getBody(), sms.getTimestamp(),
                        payloadString, e.getMessage() != null ? e.getMessage() : "Delivery failed");
            }
        });
    }

    public static void retryQueue(Context context) {
        PreferencesManager prefs = PreferencesManager.getInstance();
        if (prefs == null || prefs.isSystemPaused() || !prefs.isWebhookEnabled()) {
            return;
        }

        String webhookUrl = prefs.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.trim().isEmpty() || !webhookUrl.startsWith("https://")) {
            return;
        }

        final String token = prefs.getWebhookSecret();

        DISPATCH_EXECUTOR.execute(() -> {
            SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
            if (db == null) return;

            List<SmsDatabaseHelper.QueuedMessage> queued = db.getQueuedMessages();
            if (queued == null || queued.isEmpty()) {
                return;
            }

            SmsDatabaseHelper.PayloadConfig config = db.getPayloadConfig();
            List<SmsDatabaseHelper.KeyValueItem> customHeadersList = db.getCustomHeaders();
            Map<String, String> headers = new HashMap<>();
            for (SmsDatabaseHelper.KeyValueItem item : customHeadersList) {
                headers.put(item.key, item.value);
            }

            for (SmsDatabaseHelper.QueuedMessage msg : queued) {
                try {
                    String targetUrl = webhookUrl;
                    String body = msg.payload;
                    if ("GET".equalsIgnoreCase(config.httpMethod) && body != null && !body.isEmpty()) {
                        targetUrl += (targetUrl.contains("?") ? "&" : "?") + body;
                        body = null;
                    }

                    ApiClient.executeHttp(targetUrl, config.httpMethod, token, headers, config.contentType, body);

                    db.deleteQueuedMessage(msg.id);
                    if (msg.smsId > 0) {
                        db.updateWebhookStatus(msg.smsId, "delivered");
                    }
                } catch (Exception e) {
                    db.updateQueueRetry(msg.id, e.getMessage() != null ? e.getMessage() : "Retry connection failed");
                }
            }
        });
    }

    private static void appendParam(StringBuilder sb, String key, String value) {
        if (key == null || value == null) return;
        try {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()))
              .append("=")
              .append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
        } catch (Exception ignored) {
        }
    }

    private static String getAppVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName : "1.0.0";
        } catch (Exception e) {
            return "1.0.0";
        }
    }
}
