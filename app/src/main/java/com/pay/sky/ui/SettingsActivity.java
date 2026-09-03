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

    private SwitchMaterial switchReader;
    private SwitchMaterial switchWebhook;
    private EditText etWebhookUrl;
    private EditText etWebhookSecret;

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

        switchReader = findViewById(R.id.switchReaderEnabled);
        switchWebhook = findViewById(R.id.switchWebhook);
        etWebhookUrl = findViewById(R.id.etWebhookUrl);
        etWebhookSecret = findViewById(R.id.etWebhookSecret);
        MaterialButton btnSave = findViewById(R.id.btnSaveSettings);

        PreferencesManager prefs = PreferencesManager.getInstance();
        switchReader.setChecked(prefs.isReaderEnabled());
        switchWebhook.setChecked(prefs.isWebhookEnabled());
        etWebhookUrl.setText(prefs.getWebhookUrl());
        etWebhookSecret.setText(prefs.getWebhookSecret());

        btnSave.setOnClickListener(v -> {
            prefs.setReaderEnabled(switchReader.isChecked());
            prefs.setWebhookEnabled(switchWebhook.isChecked());
            prefs.setWebhookUrl(etWebhookUrl.getText().toString().trim());
            prefs.setWebhookSecret(etWebhookSecret.getText().toString().trim());

            Toast.makeText(this, R.string.settings_saved_toast, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
