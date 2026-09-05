package com.pay.sky.ui;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.pay.sky.R;
import com.pay.sky.api.ApiClient;
import com.pay.sky.util.HapticUtil;
import com.pay.sky.util.PreferencesManager;
import org.json.JSONObject;

public class WebhookSettingsActivity extends BaseActivity {

    private SwitchMaterial switchActive;
    private EditText etUrl;
    private EditText etSecret;
    private TextView tvTestResult;
    private MaterialButton btnTest;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webhook_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            finish();
        });

        switchActive = findViewById(R.id.switchWebhookActive);
        etUrl = findViewById(R.id.etWhUrl);
        etSecret = findViewById(R.id.etWhSecret);
        tvTestResult = findViewById(R.id.tvTestResult);
        btnTest = findViewById(R.id.btnTestWebhook);
        btnSave = findViewById(R.id.btnSaveWebhook);

        PreferencesManager prefs = PreferencesManager.getInstance();
        switchActive.setChecked(prefs.isWebhookEnabled());
        etUrl.setText(prefs.getWebhookUrl());
        etSecret.setText(prefs.getWebhookSecret());

        btnTest.setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            testConnection();
        });
        btnSave.setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            saveSettings();
        });
    }

    private void testConnection() {
        String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
        String secret = etSecret.getText() != null ? etSecret.getText().toString().trim() : "";

        if (url.isEmpty()) {
            tvTestResult.setText("Please enter a webhook URL");
            tvTestResult.setTextColor(0xFFEF4444);
            Toast.makeText(this, "Please enter a webhook URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.startsWith("https://")) {
            tvTestResult.setText("URL must start with https://");
            tvTestResult.setTextColor(0xFFEF4444);
            Toast.makeText(this, "URL must start with https://", Toast.LENGTH_SHORT).show();
            return;
        }

        tvTestResult.setText("Testing connection...");
        tvTestResult.setTextColor(0xFF0284C7);
        btnTest.setEnabled(false);

        ApiClient.testWebhook(url, secret, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                btnTest.setEnabled(true);
                tvTestResult.setText("Connection successful\nStatus 200");
                tvTestResult.setTextColor(0xFF10B981);
                Toast.makeText(WebhookSettingsActivity.this, "Connection successful (Status 200)", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                btnTest.setEnabled(true);
                int statusCode = 0;
                if (message != null && message.contains("HTTP ")) {
                    try {
                        int idx = message.indexOf("HTTP ");
                        String codeStr = message.substring(idx + 5).trim();
                        int endIdx = 0;
                        while (endIdx < codeStr.length() && Character.isDigit(codeStr.charAt(endIdx))) {
                            endIdx++;
                        }
                        if (endIdx > 0) {
                            statusCode = Integer.parseInt(codeStr.substring(0, endIdx));
                        }
                    } catch (Exception ignored) {
                    }
                }
                String display = (statusCode > 0) ? ("Connection failed\nStatus " + statusCode) : "Connection failed";
                tvTestResult.setText(display);
                tvTestResult.setTextColor(0xFFEF4444);
                Toast.makeText(WebhookSettingsActivity.this, display.replace("\n", " - "), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveSettings() {
        String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
        String secret = etSecret.getText() != null ? etSecret.getText().toString().trim() : "";

        if (switchActive.isChecked() && !url.isEmpty() && !url.startsWith("https://")) {
            Toast.makeText(this, "HTTPS is required for webhook forwarding", Toast.LENGTH_SHORT).show();
            return;
        }

        PreferencesManager prefs = PreferencesManager.getInstance();
        prefs.setWebhookEnabled(switchActive.isChecked());
        prefs.setWebhookUrl(url);
        prefs.setWebhookSecret(secret);

        Toast.makeText(this, "Webhook settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
