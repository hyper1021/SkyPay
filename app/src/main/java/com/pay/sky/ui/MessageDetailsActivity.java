package com.pay.sky.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.pay.sky.R;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;

public class MessageDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE_ID = "extra_message_id";
    private SmsModel currentSms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

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

        findViewById(R.id.btnCopyBody).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                ClipData clip = ClipData.newPlainText("SkyPay SMS", currentSms.getBody());
                cm.setPrimaryClip(clip);
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnMuteSender).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Mute " + sender + "?")
                    .setMessage("Future SMS from this sender will be stored in SQLite, but SkyPay notifications will be suppressed.")
                    .setPositiveButton("Mute", (d, w) -> {
                        SmsDatabaseHelper.getInstance().muteSender(sender);
                        Toast.makeText(this, "Muted " + sender, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}
