package com.pay.sky.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.util.PreferencesManager;
import com.pay.sky.util.QueueDispatcher;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) {

            PreferencesManager.init(context);
            SessionManager.init(context);
            SmsDatabaseHelper.init(context);

            // ফোন রিস্টার্টের পর pending queue পাঠানোর চেষ্টা
            QueueDispatcher.retryQueueAsync(context, null);
        }
    }
}
