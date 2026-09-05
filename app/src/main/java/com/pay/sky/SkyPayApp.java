package com.pay.sky;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import androidx.annotation.NonNull;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.util.PreferencesManager;
import com.pay.sky.util.QueueDispatcher;

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
        registerNetworkCallback();
    }

    public static SkyPayApp getInstance() {
        return instance;
    }

    // ── Network Callback: online হলে সাথেসাথে queue retry ────────────────
    private void registerNetworkCallback() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                // Internet ফিরে এলে pending queue পাঠাও
                QueueDispatcher.retryQueueAsync(SkyPayApp.this, null);
            }
        });
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
