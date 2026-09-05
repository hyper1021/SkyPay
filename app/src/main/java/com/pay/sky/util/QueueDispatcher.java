package com.pay.sky.util;

import android.content.Context;
import com.pay.sky.api.ApiClient;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.QueuedMessage;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import org.json.JSONObject;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class QueueDispatcher {

    private static final ExecutorService QUEUE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean isRetrying = new AtomicBoolean(false);

    public interface QueueRetryListener {
        void onCompleted(int successCount, int failedCount);
    }

    /**
     * Queue-এ থাকা সমস্ত মেসেজ একটা একটা করে main server-এ পাঠায়।
     * সার্ভার ok:true দিলে queue থেকে সরে যায়, না হলে থেকে যায়।
     */
    public static void retryQueueAsync(Context context, QueueRetryListener listener) {
        if (!isRetrying.compareAndSet(false, true)) {
            // ইতিমধ্যে চলছে
            return;
        }

        SessionManager.init(context);
        if (!SessionManager.getInstance().isLoggedIn()) {
            isRetrying.set(false);
            if (listener != null) listener.onCompleted(0, 0);
            return;
        }

        final Context appContext = context.getApplicationContext();

        QUEUE_EXECUTOR.execute(() -> {
            int success = 0;
            int failed  = 0;
            try {
                SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
                List<QueuedMessage> list = db.getQueuedMessages();

                for (QueuedMessage qm : list) {
                    try {
                        SmsModel sms = new SmsModel();
                        sms.setId(qm.getId());
                        sms.setSmsId(qm.getSmsId());
                        sms.setSender(qm.getSender());
                        sms.setBody(qm.getBody());
                        sms.setTimestamp(qm.getTimestamp());
                        sms.setSimSlot(qm.getSimSlot());
                        sms.setSubId(qm.getSubId());

                        String responseStr = ApiClient.forwardSmsToMainServer(appContext, sms);
                        JSONObject resJson = new JSONObject(responseStr);
                        boolean ok = resJson.optBoolean("ok", false);

                        if (ok) {
                            db.deleteQueuedMessage(qm.getId());
                            // received_sms table-এও delivered হিসেবে mark করো
                            try {
                                long smsRowId = Long.parseLong(qm.getSmsId());
                                db.updateWebhookStatus(smsRowId, "delivered");
                            } catch (NumberFormatException ignored) { }
                            success++;
                        } else {
                            db.updateQueuedMessageAttempt(qm.getId(),
                                    qm.getAttempts() + 1, "Server returned ok:false");
                            failed++;
                        }
                    } catch (Exception e) {
                        db.updateQueuedMessageAttempt(qm.getId(),
                                qm.getAttempts() + 1,
                                e.getMessage() != null ? e.getMessage() : "Unknown error");
                        failed++;
                    }
                }
            } finally {
                isRetrying.set(false);
                if (listener != null) {
                    final int s = success;
                    final int f = failed;
                    listener.onCompleted(s, f);
                }
            }
        });
    }
}
