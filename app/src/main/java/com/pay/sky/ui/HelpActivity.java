package com.pay.sky.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.pay.sky.R;
import com.pay.sky.util.HapticHelper;

public class HelpActivity extends AppCompatActivity {

    public static final String EXTRA_HELP_MODE = "extra_help_mode";
    public static final int MODE_LOGIN = 1;
    public static final int MODE_USER_DOCS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        Toolbar toolbar = findViewById(R.id.toolbarHelp);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            finish();
        });

        int mode = getIntent().getIntExtra(EXTRA_HELP_MODE, MODE_LOGIN);
        LinearLayout container = findViewById(R.id.layoutHelpContainer);

        if (mode == MODE_LOGIN) {
            toolbar.setTitle("Login Help & Setup");
            populateLoginHelp(container);
        } else {
            toolbar.setTitle("User Documentation");
            populateUserDocs(container);
        }
    }

    private void populateLoginHelp(LinearLayout container) {
        addCard(container, "1. Required Credentials",
                "To connect this phone to your SkyPay account, you need:\n" +
                "• Your registered SkyPay Merchant Email address.\n" +
                "• An active Device Authorization Token generated from the SkyPay web panel.");

        addCard(container, "2. How to Generate a Device Key",
                "1. Log in to your SkyPay Merchant Portal at https://payv2.skypaybd.top\n" +
                "2. Navigate to 'Gateways & Devices' > 'Android Devices'.\n" +
                "3. Click '+ Add Device' and choose a device name and validity duration (e.g. 30 Days or Unlimited).\n" +
                "4. Copy the generated Device Token and enter it in this app along with your account email.");

        addCard(container, "3. Common Login Issues",
                "• 'Invalid email or password / device key': Verify that your email matches your portal account and that the device token is active and not revoked.\n" +
                "• 'Device token expired': Device tokens not connected within 24 hours of generation expire automatically. Generate a fresh device token from your web dashboard.\n" +
                "• 'Plan limit reached': Check your subscription plan quotas on the web dashboard to ensure available device slots.");

        addCard(container, "4. Network Connectivity",
                "Ensure your device has active mobile data or Wi-Fi connectivity before tapping Sign In. The initial authentication verifies your device token with the server.");
    }

    private void populateUserDocs(LinearLayout container) {
        addCard(container, "1. Overview & Dashboard",
                "The dashboard displays real-time statistics of captured incoming SMS transactions for Today, Total, 7 Days, and 30 Days. An interactive bar chart visualizes your daily transaction volume.");

        addCard(container, "2. System Pause / Resume",
                "You can pause SkyPay at any time using the Power icon in the top header. When paused, both server forwarding and external webhook dispatches are suspended. Tapping the Play icon immediately resumes all forwarding without re-entering credentials.");

        addCard(container, "3. Global Notification Control",
                "In Settings, enabling 'Disable Notifications' suppresses incoming SMS notification alerts from appearing on your screen while allowing background transaction processing and forwarding to continue uninterrupted.");

        addCard(container, "4. Sender Muting",
                "Tap 'Mute' on any notification or message detail screen to mute future alerts from that specific sender. Transaction messages are still recorded in SQLite and forwarded to your servers. Manage muted senders in Settings > Muted Senders.");

        addCard(container, "5. Webhook Forwarding & Data Payload",
                "Configure your remote HTTPS webhook in Settings > Webhook Forwarding. In Settings > Data Payload, you can customize the HTTP method (POST, GET, PUT, etc.), content-type, field key mappings, custom headers, and static fields sent to your external server.");

        addCard(container, "6. Offline Queue & Automatic Retry",
                "If an external webhook delivery fails due to temporary network or server issues, the message is safely stored in the local SQLite Queue. The app automatically retries delivery when connectivity returns. Inspect and manage queued items in Settings > Queue Messages.");

        addCard(container, "7. Appearance & Orientation Modes",
                "Choose between System Default, Force Portrait, or Force Landscape under Settings > Appearance & Display. You can also toggle subtle native Haptic Feedback for interactive buttons and switches.");

        addCard(container, "8. Secure Session Management",
                "Your authentication credentials are encrypted and stored in private app storage. Logging out clears the local session, device tokens, and stored SMS records to protect merchant privacy.");
    }

    private void addCard(LinearLayout container, String title, String body) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_sender_analytic, container, false);
        card.setBackgroundResource(R.drawable.bg_card);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(18), dpToPx(18), dpToPx(18), dpToPx(18));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(15);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(getResources().getColor(R.color.colorTextPrimary));

        TextView tvBody = new TextView(this);
        tvBody.setText(body);
        tvBody.setTextSize(13);
        tvBody.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        tvBody.setLineSpacing(dpToPx(3), 1.0f);
        tvBody.setPadding(0, dpToPx(8), 0, 0);

        layout.addView(tvTitle);
        layout.addView(tvBody);

        com.google.android.material.card.MaterialCardView cardView = new com.google.android.material.card.MaterialCardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(14));
        cardView.setLayoutParams(lp);
        cardView.setRadius(dpToPx(16));
        cardView.setCardElevation(0);
        cardView.setStrokeWidth(dpToPx(1));
        cardView.setStrokeColor(getResources().getColor(R.color.colorCardBorder));
        cardView.setCardBackgroundColor(getResources().getColor(R.color.colorCardBackground));

        cardView.addView(layout);
        container.addView(cardView);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
