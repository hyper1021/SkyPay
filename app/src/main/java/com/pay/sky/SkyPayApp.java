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
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.util.PreferencesManager;
import com.pay.sky.util.QueueDispatcher;

public class SkyPayApp extends Application {

    public static final String CHANNEL_ID = "skypay_sms_channel";
    private static SkyPayApp instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingRetry;

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

    /**
     * Network ফিরে আসলে QueueDispatcher-কে জানাও।
     * WorkManager নিজেই CONNECTED constraint দেখে চলে,
     * কিন্তু QueueDispatcher-এর manual retry-ও trigger করি
     * যাতে UI real-time update পায়।
     * Debounce: 1 সেকেন্ড — rapid network toggle এড়াতে।
     */
    private void registerNetworkCallback() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build();

        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                // Debounce
                if (pendingRetry != null) mainHandler.removeCallbacks(pendingRetry);
                pendingRetry = () -> QueueDispatcher.retryQueueAsync(SkyPayApp.this, null);
                mainHandler.postDelayed(pendingRetry, 1000);
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

            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
