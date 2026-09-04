package com.pay.sky.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.pay.sky.R;
import com.pay.sky.api.SessionManager;
import com.pay.sky.util.PreferencesManager;
import com.pay.sky.util.ThemeHelper;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvCurrentTheme;
    private SwitchMaterial switchCapture;

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

        tvCurrentTheme = findViewById(R.id.tvCurrentTheme);
        switchCapture = findViewById(R.id.switchSettingsCapture);

        updateThemeLabel();

        PreferencesManager prefs = PreferencesManager.getInstance();
        switchCapture.setChecked(prefs.isReaderEnabled());
        switchCapture.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.setReaderEnabled(isChecked);
            Toast.makeText(this, isChecked ? "SMS capture enabled" : "SMS capture paused", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.rowThemeSelection).setOnClickListener(v -> showThemeDialog());
        findViewById(R.id.rowMutedSenders).setOnClickListener(v -> {
            startActivity(new Intent(this, MutedSendersActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });
        findViewById(R.id.rowWebhookForwarding).setOnClickListener(v -> {
            startActivity(new Intent(this, WebhookSettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });
        findViewById(R.id.rowHelp).setOnClickListener(v -> {
            startActivity(new Intent(this, HelpActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });
        findViewById(R.id.rowAbout).setOnClickListener(v -> {
            startActivity(new Intent(this, AboutActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });
        findViewById(R.id.rowLogout).setOnClickListener(v -> showLogoutDialog());
    }

    private void updateThemeLabel() {
        int themeMode = ThemeHelper.getSavedThemeMode(this);
        if (themeMode == ThemeHelper.MODE_LIGHT || themeMode == ThemeHelper.THEME_LIGHT) {
            tvCurrentTheme.setText("Light");
        } else if (themeMode == ThemeHelper.MODE_DARK || themeMode == ThemeHelper.THEME_DARK) {
            tvCurrentTheme.setText("Dark");
        } else {
            tvCurrentTheme.setText("System Default");
        }
    }

    private void showThemeDialog() {
        String[] options = {"System Default", "Light", "Dark"};
        int current = ThemeHelper.getSavedThemeMode(this);
        new AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    ThemeHelper.saveThemeMode(this, which);
                    updateThemeLabel();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out of this device?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    SessionManager.init(this);
                    SessionManager.getInstance().logout();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
