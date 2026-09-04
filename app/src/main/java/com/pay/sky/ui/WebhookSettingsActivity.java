package com.pay.sky.ui;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.pay.sky.R;
import com.pay.sky.api.ApiClient;
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
        toolbar.setNavigationOnClickListener(v -> finish());

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

        btnTest.setOnClickListener(v -> testConnection());
        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void testConnection() {
        String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
        String secret = etSecret.getText() != null ? etSecret.getText().toString().trim() : "";

        if (url.isEmpty()) {
            tvTestResult.setText("Please enter a webhook URL");
            tvTestResult.setTextColor(0xFFEF4444);
            return;
        }

        if (!url.startsWith("https://")) {
            tvTestResult.setText("URL must start with https://");
            tvTestResult.setTextColor(0xFFEF4444);
            return;
        }

        tvTestResult.setText("Connecting...");
        tvTestResult.setTextColor(0xFF0284C7);
        btnTest.setEnabled(false);

        ApiClient.testWebhook(url, secret, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                btnTest.setEnabled(true);
                tvTestResult.setText("Success: Server responded OK");
                tvTestResult.setTextColor(0xFF10B981);
            }

            @Override
            public void onError(String message) {
                btnTest.setEnabled(true);
                tvTestResult.setText("Error: " + message);
                tvTestResult.setTextColor(0xFFEF4444);
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
