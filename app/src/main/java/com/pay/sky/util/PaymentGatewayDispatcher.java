package com.pay.sky.util;

import android.content.Context;
import com.pay.sky.api.ApiClient;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.KeyValuePair;
import com.pay.sky.data.QueuedMessage;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import org.json.JSONObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentGatewayDispatcher {

    private static final ExecutorService DISPATCH_EXECUTOR = Executors.newSingleThreadExecutor();

    public static void dispatch(Context context, SmsModel sms) {
        if (sms == null) return;

        SessionManager.init(context);
        if (!SessionManager.getInstance().isLoggedIn()) return;

        PreferencesManager prefs = PreferencesManager.getInstance();
        if (prefs.isPaused()) return;

        final Context appContext = context.getApplicationContext();

        DISPATCH_EXECUTOR.execute(() -> {
            SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();

            // ── ১. সবার আগে Queue-এ ঢোকাও ──────────────────────────────
            QueuedMessage qm = new QueuedMessage();
            qm.setSmsId(sms.getSmsId() != null ? sms.getSmsId() : String.valueOf(sms.getId()));
            qm.setSender(sms.getSender());
            qm.setBody(sms.getBody());
            qm.setTimestamp(sms.getTimestamp());
            qm.setSimSlot(sms.getSimSlot());
            qm.setSubId(sms.getSubId());
            qm.setAttempts(0);
            qm.setLastAttempt(0);
            qm.setErrorReason(null);
            long queueId = db.insertQueuedMessage(qm);
            qm.setId(queueId);

            // ── ২. Main server-এ পাঠানোর চেষ্টা করো ────────────────────
            boolean mainServerOk = false;
            try {
                String responseStr = ApiClient.forwardSmsToMainServer(appContext, sms);
                JSONObject resJson = new JSONObject(responseStr);
                mainServerOk = resJson.optBoolean("ok", false);
            } catch (Exception e) {
                db.updateQueuedMessageAttempt(queueId, 1, e.getMessage());
            }

            if (mainServerOk) {
                // সার্ভার 200+ok → queue থেকে সরাও
                db.deleteQueuedMessage(queueId);
                db.updateWebhookStatus(sms.getId(), "delivered");
            } else {
                db.updateWebhookStatus(sms.getId(), "queued");
            }

            // ── ৩. External webhook (optional, আগের মতো) ────────────────
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
        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();

        try {
            String method      = prefs.getHttpMethod();
            String contentType = prefs.getPayloadContentType();
            String deviceId    = SessionManager.getInstance().getDeviceId();
            List<KeyValuePair> headerRows = prefs.getHeaderRows();
            List<KeyValuePair> bodyRows   = prefs.getPostBodyRows();

            Map<String, String> headers = PayloadBuilder.buildHeaders(headerRows, sms, contentType, token, deviceId);
            String payload = PayloadBuilder.buildPayload(bodyRows, contentType, sms, token, deviceId);

            String requestUrl  = webhookUrl;
            String requestBody = payload;
            if ("GET".equalsIgnoreCase(method)) {
                String queryString = PayloadBuilder.buildQueryString(bodyRows, sms, contentType, token, deviceId);
                if (queryString != null && !queryString.isEmpty()) {
                    requestUrl += (requestUrl.contains("?") ? "&" : "?") + queryString;
                }
                requestBody = null;
            }

            String contentTypeHeader = PayloadBuilder.isFormUrlEncoded(contentType)
                    ? "application/x-www-form-urlencoded" : "application/json; charset=UTF-8";

            ApiClient.executeHttpRequest(requestUrl, method, contentTypeHeader, token, headers, requestBody);
            db.updateWebhookStatus(sms.getId(), "delivered");
        } catch (Exception e) {
            db.updateWebhookStatus(sms.getId(), "failed");
        }
    }
}
