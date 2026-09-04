package com.pay.sky.util;

import android.content.Context;
import com.pay.sky.api.ApiClient;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.KeyValuePair;
import com.pay.sky.data.QueuedMessage;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
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
                String token = prefs.getWebhookSecret();
                String method = prefs.getHttpMethod();
                String contentType = prefs.getPayloadContentType();
                String deviceId = SessionManager.getInstance().getDeviceId();
                List<KeyValuePair> headerRows = prefs.getHeaderRows();
                List<KeyValuePair> bodyRows = prefs.getPostBodyRows();

                for (QueuedMessage qm : list) {
                    if (prefs.isPaused()) {
                        break;
                    }
                    try {
                        SmsModel sms = new SmsModel();
                        sms.setId(qm.getId());
                        sms.setSmsId(qm.getSmsId());
                        sms.setSender(qm.getSender());
                        sms.setBody(qm.getBody());
                        sms.setTimestamp(qm.getTimestamp());
                        sms.setSimSlot(qm.getSimSlot());
                        sms.setSubId(qm.getSubId());

                        Map<String, String> headers = PayloadBuilder.buildHeaders(headerRows, sms, contentType, token, deviceId);
                        String payload = PayloadBuilder.buildPayload(bodyRows, contentType, sms, token, deviceId);

                        String requestUrl = webhookUrl;
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
}
