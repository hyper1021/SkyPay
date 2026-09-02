package com.pay.sky.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.pay.sky.R;
import com.pay.sky.SkyPayApp;
import com.pay.sky.ui.MainActivity;
import com.pay.sky.util.PreferencesManager;

public class SmsReaderService extends Service {

    public static final String ACTION_START = "com.pay.sky.action.START_SERVICE";
    public static final String ACTION_STOP = "com.pay.sky.action.STOP_SERVICE";
    public static final String ACTION_UPDATE_COUNT = "com.pay.sky.action.UPDATE_COUNT";
    public static final String ACTION_REQUEST_STOP = "com.pay.sky.action.REQUEST_STOP";
    public static final String ACTION_CONFIRM_STOP = "com.pay.sky.action.CONFIRM_STOP";
    public static final String ACTION_CANCEL_STOP = "com.pay.sky.action.CANCEL_STOP";
    public static final String BROADCAST_STATUS_CHANGED = "com.pay.sky.broadcast.SERVICE_STATUS_CHANGED";

    private static final int NOTIFICATION_ID = 1001;
    private static volatile boolean isRunning = false;
    private boolean isConfirmingStop = false;

    public static boolean isRunning() {
        return isRunning;
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, SmsReaderService.class);
        intent.setAction(ACTION_START);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, SmsReaderService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    public static void notifySmsReceived(Context context) {
        if (isRunning) {
            Intent intent = new Intent(context, SmsReaderService.class);
            intent.setAction(ACTION_UPDATE_COUNT);
            context.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (ACTION_STOP.equals(action) || ACTION_CONFIRM_STOP.equals(action)) {
            handleStopService();
            return START_NOT_STICKY;
        }

        if (ACTION_REQUEST_STOP.equals(action)) {
            isConfirmingStop = true;
            updateNotification();
            return START_STICKY;
        }

        if (ACTION_CANCEL_STOP.equals(action)) {
            isConfirmingStop = false;
            updateNotification();
            return START_STICKY;
        }

        if (ACTION_UPDATE_COUNT.equals(action)) {
            if (isRunning && !isConfirmingStop) {
                updateNotification();
            }
            return START_STICKY;
        }

        isRunning = true;
        isConfirmingStop = false;
        PreferencesManager.getInstance().setReaderEnabled(true);
        startInForeground();
        broadcastStatus();
        return START_STICKY;
    }

    private void startInForeground() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private Notification buildNotification() {
        if (isConfirmingStop) {
            return buildConfirmationNotification();
        }
        return buildRunningNotification();
    }

    private Notification buildRunningNotification() {
        int sessionCount = PreferencesManager.getInstance().getSessionSmsCount();

        Intent viewIntent = new Intent(this, MainActivity.class);
        viewIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(
                this, 101, viewIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, SmsReaderService.class);
        stopIntent.setAction(ACTION_REQUEST_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 102, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String contentText = getString(R.string.notification_text_running, sessionCount);

        return new NotificationCompat.Builder(this, SkyPayApp.CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title_running))
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_stat_skypay)
                .setContentIntent(viewPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.drawable.ic_message, getString(R.string.notification_action_view), viewPendingIntent)
                .addAction(R.drawable.ic_power, getString(R.string.notification_action_stop), stopPendingIntent)
                .build();
    }

    private Notification buildConfirmationNotification() {
        Intent confirmIntent = new Intent(this, SmsReaderService.class);
        confirmIntent.setAction(ACTION_CONFIRM_STOP);
        PendingIntent confirmPendingIntent = PendingIntent.getService(
                this, 103, confirmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent cancelIntent = new Intent(this, SmsReaderService.class);
        cancelIntent.setAction(ACTION_CANCEL_STOP);
        PendingIntent cancelPendingIntent = PendingIntent.getService(
                this, 104, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent viewIntent = new Intent(this, MainActivity.class);
        viewIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(
                this, 105, viewIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, SkyPayApp.CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title_confirm_stop))
                .setContentText(getString(R.string.notification_text_confirm_stop))
                .setSmallIcon(R.drawable.ic_warning)
                .setContentIntent(viewPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.drawable.ic_power, getString(R.string.notification_action_confirm_stop), confirmPendingIntent)
                .addAction(R.drawable.ic_check, getString(R.string.notification_action_keep_running), cancelPendingIntent)
                .build();
    }

    private void handleStopService() {
        isRunning = false;
        isConfirmingStop = false;
        PreferencesManager.getInstance().setReaderEnabled(false);
        stopForeground(true);
        stopSelf();
        broadcastStatus();
    }

    private void broadcastStatus() {
        Intent intent = new Intent(BROADCAST_STATUS_CHANGED);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        isConfirmingStop = false;
        broadcastStatus();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
