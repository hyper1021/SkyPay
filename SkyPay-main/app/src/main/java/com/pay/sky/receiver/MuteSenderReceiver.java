package com.pay.sky.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import androidx.core.app.NotificationManagerCompat;
import com.pay.sky.data.SmsDatabaseHelper;

public class MuteSenderReceiver extends BroadcastReceiver {

    public static final String ACTION_MUTE_SENDER = "com.pay.sky.action.MUTE_SENDER";
    public static final String EXTRA_SENDER = "extra_sender";
    public static final String EXTRA_NOTIF_ID = "extra_notif_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_MUTE_SENDER.equals(intent.getAction())) {
            return;
        }

        String sender = intent.getStringExtra(EXTRA_SENDER);
        int notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1);

        if (sender != null && !sender.trim().isEmpty()) {
            SmsDatabaseHelper.init(context);
            SmsDatabaseHelper.getInstance().muteSender(sender);
            Toast.makeText(context, "Muted", Toast.LENGTH_SHORT).show();
        }

        if (notifId >= 0) {
            NotificationManagerCompat.from(context).cancel(notifId);
        }
    }
}
