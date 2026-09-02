package com.pay.sky.ui;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.pay.sky.R;
import com.pay.sky.util.PreferencesManager;

public class SettingsActivity extends AppCompatActivity {

    private SwitchMaterial switchWebhook;
    private EditText etWebhookUrl;
    private EditText etWebhookSecret;
    private SwitchMaterial switchAutoStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        switchWebhook = findViewById(R.id.switchWebhook);
        etWebhookUrl = findViewById(R.id.etWebhookUrl);
        etWebhookSecret = findViewById(R.id.etWebhookSecret);
        switchAutoStart = findViewById(R.id.switchAutoStart);
        MaterialButton btnSave = findViewById(R.id.btnSaveSettings);

        PreferencesManager prefs = PreferencesManager.getInstance();
        switchWebhook.setChecked(prefs.isWebhookEnabled());
        etWebhookUrl.setText(prefs.getWebhookUrl());
        etWebhookSecret.setText(prefs.getWebhookSecret());
        switchAutoStart.setChecked(prefs.isAutoStartOnBoot());

        btnSave.setOnClickListener(v -> {
            prefs.setWebhookEnabled(switchWebhook.isChecked());
            prefs.setWebhookUrl(etWebhookUrl.getText().toString().trim());
            prefs.setWebhookSecret(etWebhookSecret.getText().toString().trim());
            prefs.setAutoStartOnBoot(switchAutoStart.isChecked());

            Toast.makeText(this, R.string.settings_saved_toast, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
