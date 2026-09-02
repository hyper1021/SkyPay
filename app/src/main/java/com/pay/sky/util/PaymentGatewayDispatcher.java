package com.pay.sky.util;

import android.content.Context;
import com.pay.sky.data.SmsModel;
import org.json.JSONObject;

public class PaymentGatewayDispatcher {

    public static void dispatch(Context context, SmsModel sms) {
        PreferencesManager prefs = PreferencesManager.getInstance();
        if (!prefs.isWebhookEnabled()) {
            return;
        }

        String webhookUrl = prefs.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return;
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("smsId", sms.getSmsId());
            payload.put("threadId", sms.getThreadId());
            payload.put("sender", sms.getSender());
            payload.put("body", sms.getBody());
            payload.put("timestamp", sms.getTimestamp());
            payload.put("simSlot", sms.getSimSlot());
            payload.put("subId", sms.getSubId());
            payload.put("serviceCenter", sms.getServiceCenter());
            payload.put("smsType", sms.getSmsType());

            enqueuePayload(context, webhookUrl, prefs.getWebhookSecret(), payload);
        } catch (Exception ignored) {
        }
    }

    private static void enqueuePayload(Context context, String url, String secret, JSONObject payload) {
    }
}
