package com.pay.sky.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.pay.sky.R;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import com.pay.sky.util.HapticHelper;

public class MessageDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE_ID = "extra_message_id";
    private SmsModel currentSms;
    private MaterialButton btnMuteSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            finish();
        });

        long messageId = getIntent().getLongExtra(EXTRA_MESSAGE_ID, -1);
        SmsDatabaseHelper.init(this);
        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        currentSms = db.getSmsById(messageId);

        if (currentSms == null) {
            Toast.makeText(this, "Message not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.markAsRead(currentSms.getId());

        TextView tvAvatar = findViewById(R.id.tvDetailSenderAvatar);
        TextView tvSender = findViewById(R.id.tvDetailSender);
        TextView tvClassification = findViewById(R.id.tvDetailClassification);
        TextView tvSimBadge = findViewById(R.id.tvDetailSimBadge);
        TextView tvTimestamp = findViewById(R.id.tvDetailTimestamp);
        TextView tvBody = findViewById(R.id.tvDetailBody);
        TextView tvSmsId = findViewById(R.id.tvDetailSmsId);
        TextView tvWebhookStatus = findViewById(R.id.tvDetailWebhookStatus);
        btnMuteSender = findViewById(R.id.btnMuteSender);

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

        findViewById(R.id.btnCopyBody).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                ClipData clip = ClipData.newPlainText("SkyPay SMS", currentSms.getBody());
                cm.setPrimaryClip(clip);
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        btnMuteSender.setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            boolean isMuted = db.isSenderMuted(sender);
            if (isMuted) {
                db.unmuteSender(sender);
                Toast.makeText(this, "Unmuted", Toast.LENGTH_SHORT).show();
            } else {
                db.muteSender(sender);
                Toast.makeText(this, "Muted", Toast.LENGTH_SHORT).show();
            }
            updateMuteButtonState(sender);
        });
    }

    private void updateMuteButtonState(String sender) {
        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        if (db == null) return;
        boolean isMuted = db.isSenderMuted(sender);
        if (isMuted) {
            btnMuteSender.setText("Unmute");
            btnMuteSender.setIconResource(R.drawable.ic_check);
        } else {
            btnMuteSender.setText("Mute Sender");
            btnMuteSender.setIconResource(R.drawable.ic_mute);
        }
    }
}
