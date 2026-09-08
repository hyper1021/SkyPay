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
import com.pay.sky.data.KeyValuePair;
import com.pay.sky.data.QueuedMessage;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import com.pay.sky.worker.SmsUploadWorker;
import org.json.JSONObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

            // ── ২. Internet আছে কিনা চেক করো ────────────────────────────
            boolean hasInternet = isInternetAvailable(appContext);

            if (hasInternet) {
                // ── ৩a. Online: সরাসরি পাঠানোর চেষ্টা ───────────────────
                boolean sent = false;
                try {
                    String responseStr = ApiClient.forwardSmsToMainServer(appContext, sms);
                    JSONObject resJson = new JSONObject(responseStr);
                    sent = resJson.optBoolean("ok", false);
                } catch (Exception e) {
                    String errMsg = SmsUploadWorker.friendlyError(e);
                    db.updateQueuedMessageAttempt(queueId, 1, errMsg);
                }

                if (sent) {
                    // ✅ সফল → queue থেকে সরাও
                    db.deleteQueuedMessage(queueId);
                    db.updateWebhookStatus(sms.getId(), "delivered");
                } else {
                    // Direct send ব্যর্থ হলেও WorkManager দিয়ে retry
                    db.updateWebhookStatus(sms.getId(), "queued");
                    enqueueWorker(appContext, queueId, sms);
                }
            } else {
                // ── ৩b. Offline: WorkManager-এ দাও, সে অনলাইন হলে পাঠাবে ─
                db.updateWebhookStatus(sms.getId(), "queued");
                enqueueWorker(appContext, queueId, sms);
            }

            // ── ৪. External webhook (optional, unchanged) ─────────────────
            if (prefs.isWebhookEnabled()) {
                dispatchToExternalWebhook(appContext, sms);
            }
        });
    }

    /**
     * WorkManager-এ একটা OneTimeWorkRequest enqueue করে।
     * Unique work name = "sms_upload_<queueId>" → duplicate prevent হয়।
     * Constraint: CONNECTED → শুধু internet থাকলে চলবে।
     */
    private static void enqueueWorker(Context context, long queueId, SmsModel sms) {
        Data inputData = SmsUploadWorker.buildInputData(
                queueId,
                sms.getId(),
                sms.getSmsId() != null ? sms.getSmsId() : String.valueOf(sms.getId()),
                sms.getSender(),
                sms.getBody(),
                sms.getTimestamp(),
                sms.getSimSlot(),
                sms.getSubId()
        );

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SmsUploadWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build();

        // Unique name দিয়ে enqueue → একই message দুইবার যাবে না
        WorkManager.getInstance(context)
                .enqueueUniqueWork(
                        "sms_upload_" + queueId,
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
        } catch (Exception ignored) {
        }
    }
}
