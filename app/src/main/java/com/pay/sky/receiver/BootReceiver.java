package com.pay.sky.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.pay.sky.service.SmsReaderService;
import com.pay.sky.util.PreferencesManager;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            PreferencesManager.init(context);
            PreferencesManager prefs = PreferencesManager.getInstance();
            if (prefs.isReaderEnabled() && prefs.isAutoStartOnBoot()) {
                SmsReaderService.start(context);
            }
        }
    }
}
