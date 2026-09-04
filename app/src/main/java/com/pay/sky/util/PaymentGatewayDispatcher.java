package com.pay.sky.util;

import android.content.Context;
import android.os.Build;
import com.pay.sky.api.ApiClient;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentGatewayDispatcher {

    private static final ExecutorService DISPATCH_EXECUTOR = Executors.newSingleThreadExecutor();

    public static void dispatch(Context context, SmsModel sms) {
        PreferencesManager prefs = PreferencesManager.getInstance();
        if (!prefs.isWebhookEnabled()) {
            return;
        }

        String webhookUrl = prefs.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.trim().isEmpty() || !webhookUrl.startsWith("https://")) {
            return;
        }

        final String token = prefs.getWebhookSecret();
        final long smsId = sms.getId();

        DISPATCH_EXECUTOR.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("schema_version", "1");
                payload.put("event", "sms.received");

                JSONObject msgObj = new JSONObject();
                msgObj.put("id", String.valueOf(sms.getId()));
                msgObj.put("sender", sms.getSender());
                msgObj.put("body", sms.getBody());
                msgObj.put("received_at", sms.getTimestamp());
                msgObj.put("sim_slot", sms.getSimSlot());
                msgObj.put("processing_status", "processed");
                payload.put("message", msgObj);

                JSONObject devObj = new JSONObject();
                devObj.put("app_version", "1.0.0");
                devObj.put("android_version", Build.VERSION.SDK_INT);
                devObj.put("device_model", Build.MODEL);
                payload.put("device", devObj);

                ApiClient.executePost(webhookUrl, token, payload.toString());
                SmsDatabaseHelper.getInstance().updateWebhookStatus(smsId, "delivered");
            } catch (Exception e) {
                SmsDatabaseHelper.getInstance().updateWebhookStatus(smsId, "failed");
            }
        });
    }
}
