package com.pay.sky;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.util.PreferencesManager;

public class SkyPayApp extends Application {

    public static final String CHANNEL_ID = "skypay_sms_channel";
    private static SkyPayApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        PreferencesManager.init(this);
        SessionManager.init(this);
        SmsDatabaseHelper.init(this);
        createNotificationChannel();
    }

    public static SkyPayApp getInstance() {
        return instance;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(getString(R.string.notification_channel_desc));
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
