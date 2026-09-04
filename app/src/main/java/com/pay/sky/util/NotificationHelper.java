package com.pay.sky.util;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.pay.sky.R;
import com.pay.sky.SkyPayApp;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import com.pay.sky.receiver.MuteSenderReceiver;
import com.pay.sky.ui.MessageDetailsActivity;

public class NotificationHelper {

    public static void showSmsNotification(Context context, SmsModel sms) {
        if (sms == null) {
            return;
        }

        SessionManager.init(context);
        if (!SessionManager.getInstance().isLoggedIn()) {
            return;
        }

        PreferencesManager.init(context);
        if (PreferencesManager.getInstance().isNotificationsHidden()) {
            return;
        }

        SmsDatabaseHelper.init(context);
        if (SmsDatabaseHelper.getInstance().isSenderMuted(sms.getSender())) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        try {
            int notifId = (int) (sms.getId() > 0 ? sms.getId() : (System.currentTimeMillis() % 100000));

            Intent viewIntent = new Intent(context, MessageDetailsActivity.class);
            viewIntent.putExtra(MessageDetailsActivity.EXTRA_MESSAGE_ID, sms.getId());
            viewIntent.putExtra(MessageDetailsActivity.EXTRA_DISMISS_NOTIF_ID, notifId);
            viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent viewPendingIntent = PendingIntent.getActivity(
                    context, notifId * 2, viewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Intent muteIntent = new Intent(context, MuteSenderReceiver.class);
            muteIntent.setAction(MuteSenderReceiver.ACTION_MUTE_SENDER);
            muteIntent.putExtra(MuteSenderReceiver.EXTRA_SENDER, sms.getSender());
            muteIntent.putExtra(MuteSenderReceiver.EXTRA_NOTIF_ID, notifId);
            PendingIntent mutePendingIntent = PendingIntent.getBroadcast(
                    context, notifId * 2 + 1, muteIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            String title = sms.getSender() != null && !sms.getSender().trim().isEmpty()
                    ? sms.getSender().trim() : sms.getRecognizedSenderLabel();
            String preview = sms.getBody();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SkyPayApp.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_skypay)
                    .setContentTitle(title)
                    .setContentText(preview)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(preview))
                    .setWhen(sms.getTimestamp())
                    .setShowWhen(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(viewPendingIntent)
                    .addAction(R.drawable.ic_message, "View", viewPendingIntent)
                    .addAction(R.drawable.ic_mute, "Mute", mutePendingIntent);

            NotificationManagerCompat.from(context).notify(notifId, builder.build());
        } catch (Exception ignored) {
        }
    }
}
