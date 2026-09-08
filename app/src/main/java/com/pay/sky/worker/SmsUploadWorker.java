package com.pay.sky.worker;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.pay.sky.api.ApiClient;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import org.json.JSONObject;

public class SmsUploadWorker extends Worker {

    public static final String KEY_QUEUE_ID   = "queue_id";
    public static final String KEY_SMS_ROW_ID = "sms_row_id";
    public static final String KEY_SENDER     = "sender";
    public static final String KEY_BODY       = "body";
    public static final String KEY_TIMESTAMP  = "timestamp";
    public static final String KEY_SIM_SLOT   = "sim_slot";
    public static final String KEY_SUB_ID     = "sub_id";
    public static final String KEY_SMS_ID     = "sms_id";

    public SmsUploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // ── ১. Internet check ─────────────────────────────────────────────
        if (!isInternetAvailable(context)) {
            return Result.retry();
        }

        // ── ২. Login check ────────────────────────────────────────────────
        SessionManager.init(context);
        if (!SessionManager.getInstance().isLoggedIn()) {
            return Result.failure();
        }

        // ── ৩. Input data ─────────────────────────────────────────────────
        long   queueId   = getInputData().getLong(KEY_QUEUE_ID,   -1);
        long   smsRowId  = getInputData().getLong(KEY_SMS_ROW_ID, -1);
        String sender    = getInputData().getString(KEY_SENDER);
        String body      = getInputData().getString(KEY_BODY);
        long   timestamp = getInputData().getLong(KEY_TIMESTAMP,  0);
        int    simSlot   = getInputData().getInt(KEY_SIM_SLOT,    0);
        int    subId     = getInputData().getInt(KEY_SUB_ID,      0);
        String smsId     = getInputData().getString(KEY_SMS_ID);

        if (queueId < 0 || sender == null || body == null) {
            return Result.failure();
        }

        // ── ৪. Duplicate guard: queue-এ এখনো আছে? ───────────────────────
        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        if (db == null) {
            SmsDatabaseHelper.init(context);
            db = SmsDatabaseHelper.getInstance();
        }
        if (!db.queuedMessageExists(queueId)) {
            // ইতিমধ্যে সফলভাবে পাঠানো হয়েছে (QueueDispatcher বা আগের Worker run)
            return Result.success();
        }

        // ── ৫. Synchronous API call ───────────────────────────────────────
        try {
            SmsModel sms = new SmsModel();
            sms.setId(queueId);
            sms.setSmsId(smsId != null ? smsId : String.valueOf(queueId));
            sms.setSender(sender);
            sms.setBody(body);
            sms.setTimestamp(timestamp);
            sms.setSimSlot(simSlot);
            sms.setSubId(subId);

            String responseStr = ApiClient.forwardSmsToMainServer(context, sms);
            JSONObject resJson = new JSONObject(responseStr);
            boolean ok = resJson.optBoolean("ok", false);

            if (ok) {
                db.deleteQueuedMessage(queueId);
                if (smsRowId >= 0) {
                    db.updateWebhookStatus(smsRowId, "delivered");
                }
                return Result.success();
            } else {
                String errMsg = resJson.optString("description", "Server returned ok:false");
                db.updateQueuedMessageAttempt(queueId, getRunAttemptCount() + 1, errMsg);
                return Result.retry();
            }

        } catch (Exception e) {
            String errMsg = friendlyError(e);
            db.updateQueuedMessageAttempt(queueId, getRunAttemptCount() + 1, errMsg);
            return Result.retry();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean isInternetAvailable(Context context) {
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

    /** Exception → user-friendly message (API URL expose হয় না) */
    public static String friendlyError(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return "Connection error";
        String lower = msg.toLowerCase();
        if (lower.contains("unable to resolve host") || lower.contains("unknownhostexception")) {
            return "Server connection error";
        }
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return "Connection timed out";
        }
        if (lower.contains("connection refused")) {
            return "Server connection error";
        }
        return "Network error";
    }

    /** WorkManager-এর জন্য Data object তৈরি */
    public static Data buildInputData(long queueId, long smsRowId, String smsId,
                                      String sender, String body, long timestamp,
                                      int simSlot, int subId) {
        return new Data.Builder()
                .putLong(KEY_QUEUE_ID,   queueId)
                .putLong(KEY_SMS_ROW_ID, smsRowId)
                .putString(KEY_SMS_ID,   smsId != null ? smsId : String.valueOf(queueId))
                .putString(KEY_SENDER,   sender != null ? sender : "")
                .putString(KEY_BODY,     body   != null ? body   : "")
                .putLong(KEY_TIMESTAMP,  timestamp)
                .putInt(KEY_SIM_SLOT,    simSlot)
                .putInt(KEY_SUB_ID,      subId)
                .build();
    }
}
