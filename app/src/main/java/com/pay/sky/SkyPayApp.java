package com.pay.sky;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.util.PreferencesManager;

public class SkyPayApp extends Application {

    public static final String CHANNEL_ID = "skypay_alerts_channel";
    private static SkyPayApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        PreferencesManager.init(this);
        SmsDatabaseHelper.init(this);
        createNotificationChannel();
    }

    public static SkyPayApp getInstance() {
        return instance;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_desc);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
