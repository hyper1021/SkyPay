package com.pay.sky.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.material.button.MaterialButton;
import com.pay.sky.R;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import com.pay.sky.util.HapticUtil;

public class MessageDetailsActivity extends BaseActivity {

    public static final String EXTRA_MESSAGE_ID = "extra_message_id";
    public static final String EXTRA_DISMISS_NOTIF_ID = "extra_dismiss_notif_id";
    private SmsModel currentSms;
    private MaterialButton btnMuteSender;
    private MaterialButton btnSendWebhook;
    private MaterialButton btnCopyBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_details);

        int dismissNotifId = getIntent().getIntExtra(EXTRA_DISMISS_NOTIF_ID, -1);
        if (dismissNotifId >= 0) {
            NotificationManagerCompat.from(this).cancel(dismissNotifId);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            finish();
        });

        long messageId = getIntent().getLongExtra(EXTRA_MESSAGE_ID, -1);
        SmsDatabaseHelper.init(this);
        currentSms = SmsDatabaseHelper.getInstance().getSmsById(messageId);

        if (currentSms == null) {
            Toast.makeText(this, "Message not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SmsDatabaseHelper.getInstance().markAsRead(currentSms.getId());

        TextView tvAvatar = findViewById(R.id.tvDetailSenderAvatar);
        TextView tvSender = findViewById(R.id.tvDetailSender);
        TextView tvClassification = findViewById(R.id.tvDetailClassification);
        TextView tvSimBadge = findViewById(R.id.tvDetailSimBadge);
        TextView tvTimestamp = findViewById(R.id.tvDetailTimestamp);
        TextView tvBody = findViewById(R.id.tvDetailBody);
        TextView tvSmsId = findViewById(R.id.tvDetailSmsId);
        TextView tvWebhookStatus = findViewById(R.id.tvDetailWebhookStatus);

        btnMuteSender = findViewById(R.id.btnMuteSender);
        btnSendWebhook = findViewById(R.id.btnSendWebhook);
        btnCopyBody = findViewById(R.id.btnCopyBody);

        String sender = currentSms.getSender();
        toolbar.setTitle(sender);
        tvSender.setText(sender);
        tvClassification.setText(currentSms.getRecognizedSenderLabel());

        String initial = "S";
        if (sender != null && !sender.isEmpty()) {
            initial = String.valueOf(sender.charAt(0)).toUpperCase();
        }
        tvAvatar.setText(initial);

        tvSimBadge.setText(currentSms.getSimSlotDisplay());
        tvTimestamp.setText(currentSms.getFormattedDate());
        tvBody.setText(currentSms.getBody());
        tvSmsId.setText("#" + currentSms.getId());
        tvWebhookStatus.setText(currentSms.getWebhookStatus());

        updateMuteButtonState(sender);

        btnMuteSender.setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            toggleMuteState(sender);
        });

        // Send button হাইড — duplicate send prevent করতে
        if (btnSendWebhook != null) {
            btnSendWebhook.setVisibility(android.view.View.GONE);
        }

        if (btnCopyBody != null) {
            btnCopyBody.setOnClickListener(v -> {
                HapticUtil.vibrateClick(this);
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    ClipData clip = ClipData.newPlainText("SkyPay SMS", currentSms.getBody());
                    cm.setPrimaryClip(clip);
                    Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateMuteButtonState(String sender) {
        boolean isMuted = SmsDatabaseHelper.getInstance().isSenderMuted(sender);
        if (isMuted) {
            btnMuteSender.setText("Unmute");
            btnMuteSender.setIconResource(R.drawable.ic_check);
            btnMuteSender.setTextColor(0xFF10B981);
            btnMuteSender.setIconTint(ColorStateList.valueOf(0xFF10B981));
            btnMuteSender.setStrokeColor(ColorStateList.valueOf(0xFF6EE7B7));
        } else {
            btnMuteSender.setText("Mute");
            btnMuteSender.setIconResource(R.drawable.ic_mute);
            btnMuteSender.setTextColor(0xFFEF4444);
            btnMuteSender.setIconTint(ColorStateList.valueOf(0xFFEF4444));
            btnMuteSender.setStrokeColor(ColorStateList.valueOf(0xFFFCA5A5));
        }
    }

    private void toggleMuteState(String sender) {
        if (sender == null || sender.trim().isEmpty()) {
            return;
        }
        boolean isMuted = SmsDatabaseHelper.getInstance().isSenderMuted(sender);
        if (isMuted) {
            SmsDatabaseHelper.getInstance().unmuteSender(sender);
            Toast.makeText(this, "Unmuted " + sender, Toast.LENGTH_SHORT).show();
        } else {
            SmsDatabaseHelper.getInstance().muteSender(sender);
            Toast.makeText(this, "Muted " + sender, Toast.LENGTH_SHORT).show();
        }
        updateMuteButtonState(sender);
    }
}
