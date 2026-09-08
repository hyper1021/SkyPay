package com.pay.sky.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.pay.sky.api.ApiClient;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.QueuedMessage;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import com.pay.sky.worker.SmsUploadWorker;
import org.json.JSONObject;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class QueueDispatcher {

    private static final ExecutorService QUEUE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean isRetrying = new AtomicBoolean(false);

    public interface QueueRetryListener {
        void onCompleted(int successCount, int failedCount);
        /** প্রতিটা item সফলভাবে পাঠানোর সাথে সাথে UI update-এর জন্য */
        default void onItemSent(long queueId) {}
    }

    /**
     * Queue-এ থাকা সব message পাঠানোর চেষ্টা করে।
     * Internet না থাকলে immediately WorkManager-এ দেয়।
     * সফল হলে queue থেকে real-time সরায়।
     */
    public static void retryQueueAsync(Context context, QueueRetryListener listener) {
        if (!isRetrying.compareAndSet(false, true)) {
            return; // Already running
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

                boolean hasInternet = isInternetAvailable(appContext);

                for (QueuedMessage qm : list) {
                    if (!hasInternet) {
                        // Internet নেই → WorkManager-এ দাও (CONNECTED constraint সহ)
                        enqueueWorker(appContext, qm);
                        failed++;
                        continue;
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

                        String responseStr = ApiClient.forwardSmsToMainServer(appContext, sms);
                        JSONObject resJson = new JSONObject(responseStr);
                        boolean ok = resJson.optBoolean("ok", false);

                        if (ok) {
                            // ✅ সফল → queue থেকে real-time সরাও
                            db.deleteQueuedMessage(qm.getId());
                            try {
                                long smsRowId = Long.parseLong(qm.getSmsId());
                                db.updateWebhookStatus(smsRowId, "delivered");
                            } catch (NumberFormatException ignored) {}
                            success++;
                            if (listener != null) listener.onItemSent(qm.getId());
                        } else {
                            String errMsg = resJson.optString("description", "Server returned ok:false");
                            db.updateQueuedMessageAttempt(qm.getId(), qm.getAttempts() + 1, errMsg);
                            enqueueWorker(appContext, qm);
                            failed++;
                        }

                    } catch (Exception e) {
                        String errMsg = SmsUploadWorker.friendlyError(e);
                        db.updateQueuedMessageAttempt(qm.getId(), qm.getAttempts() + 1, errMsg);
                        enqueueWorker(appContext, qm);
                        failed++;
                    }
                }
            } finally {
                isRetrying.set(false);
                if (listener != null) {
                    final int s = success, f = failed;
                    listener.onCompleted(s, f);
                }
            }
        });
    }

    private static void enqueueWorker(Context context, QueuedMessage qm) {
        Data inputData = SmsUploadWorker.buildInputData(
                qm.getId(),
                -1,
                qm.getSmsId(),
                qm.getSender(),
                qm.getBody(),
                qm.getTimestamp(),
                qm.getSimSlot(),
                qm.getSubId()
        );

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SmsUploadWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(
                        "sms_upload_" + qm.getId(),
                        androidx.work.ExistingWorkPolicy.KEEP,
                        workRequest
                );
    }

    private static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
