package com.pay.sky.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.pay.sky.R;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.util.HapticHelper;
import com.pay.sky.util.OrientationHelper;
import com.pay.sky.util.PreferencesManager;
import com.pay.sky.util.ThemeHelper;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvCurrentTheme;
    private TextView tvCurrentOrientation;
    private TextView tvQueueSubtext;
    private SwitchMaterial switchHaptic;
    private SwitchMaterial switchCapture;
    private SwitchMaterial switchGlobalNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            finish();
        });

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSettingsValues();
    }

    private void initViews() {
        tvCurrentTheme = findViewById(R.id.tvCurrentTheme);
        tvCurrentOrientation = findViewById(R.id.tvCurrentOrientation);
        tvQueueSubtext = findViewById(R.id.tvQueueSubtext);
        switchHaptic = findViewById(R.id.switchHapticFeedback);
        switchCapture = findViewById(R.id.switchSettingsCapture);
        switchGlobalNotifications = findViewById(R.id.switchGlobalNotifications);
    }

    private void updateSettingsValues() {
        PreferencesManager prefs = PreferencesManager.getInstance();
        if (prefs == null) return;

        int themeMode = ThemeHelper.getSavedThemeMode(this);
        if (themeMode == ThemeHelper.MODE_LIGHT) {
            tvCurrentTheme.setText("Light");
        } else if (themeMode == ThemeHelper.MODE_DARK) {
            tvCurrentTheme.setText("Dark");
        } else {
            tvCurrentTheme.setText("System Default");
        }

        int orientMode = prefs.getOrientationMode();
        if (orientMode == OrientationHelper.MODE_PORTRAIT) {
            tvCurrentOrientation.setText("Portrait");
        } else if (orientMode == OrientationHelper.MODE_LANDSCAPE) {
            tvCurrentOrientation.setText("Landscape");
        } else {
            tvCurrentOrientation.setText("System Default");
        }

        switchHaptic.setChecked(prefs.isHapticEnabled());
        switchCapture.setChecked(prefs.isReaderEnabled());
        switchGlobalNotifications.setChecked(prefs.isGlobalNotificationsDisabled());

        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        if (db != null) {
            int qCount = db.getQueuedCount();
            if (qCount > 0) {
                tvQueueSubtext.setText(qCount + " message(s) awaiting retry");
                tvQueueSubtext.setTextColor(0xFFEF4444);
            } else {
                tvQueueSubtext.setText("All webhook deliveries up to date");
                tvQueueSubtext.setTextColor(0xFF64748B);
            }
        }
    }

    private void setupListeners() {
        PreferencesManager prefs = PreferencesManager.getInstance();

        findViewById(R.id.rowThemeSelection).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            showThemeDialog();
        });

        findViewById(R.id.rowOrientationSelection).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            showOrientationDialog();
        });

        switchHaptic.setOnCheckedChangeListener((btn, isChecked) -> {
            HapticHelper.performHaptic(btn);
            prefs.setHapticEnabled(isChecked);
        });

        switchCapture.setOnCheckedChangeListener((btn, isChecked) -> {
            HapticHelper.performHaptic(btn);
            prefs.setReaderEnabled(isChecked);
        });

        switchGlobalNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
            HapticHelper.performHaptic(btn);
            prefs.setGlobalNotificationsDisabled(isChecked);
        });

        findViewById(R.id.rowMutedSenders).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            startActivity(new Intent(this, MutedSendersActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        findViewById(R.id.rowWebhookForwarding).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            startActivity(new Intent(this, WebhookSettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        findViewById(R.id.rowDataPayload).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            startActivity(new Intent(this, DataPayloadActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        findViewById(R.id.rowQueueMessages).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            startActivity(new Intent(this, QueueMessagesActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        findViewById(R.id.rowHelp).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            Intent helpIntent = new Intent(this, HelpActivity.class);
            helpIntent.putExtra(HelpActivity.EXTRA_HELP_MODE, HelpActivity.MODE_USER_DOCS);
            startActivity(helpIntent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        findViewById(R.id.rowAbout).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            startActivity(new Intent(this, AboutActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        findViewById(R.id.rowLogout).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            showLogoutConfirmDialog();
        });
    }

    private void showThemeDialog() {
        String[] options = {"System Default", "Light Theme", "Dark Theme"};
        int current = ThemeHelper.getSavedThemeMode(this);

        new AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    HapticHelper.performHaptic(findViewById(android.R.id.content));
                    ThemeHelper.saveThemeMode(SettingsActivity.this, which);
                    ThemeHelper.applyTheme(SettingsActivity.this);
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showOrientationDialog() {
        String[] options = {"System Default", "Force Portrait", "Force Landscape"};
        PreferencesManager prefs = PreferencesManager.getInstance();
        int current = prefs.getOrientationMode();

        new AlertDialog.Builder(this)
                .setTitle("Screen Orientation")
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    HapticHelper.performHaptic(findViewById(android.R.id.content));
                    prefs.setOrientationMode(which);
                    OrientationHelper.applyOrientation(SettingsActivity.this);
                    dialog.dismiss();
                    updateSettingsValues();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutConfirmDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_logout, null);
        dialog.setView(view);

        MaterialButton btnCancel = view.findViewById(R.id.btnCancelLogout);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmLogout);

        btnCancel.setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            dialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            HapticHelper.performActionHaptic(this);
            dialog.dismiss();

            SessionManager.getInstance().logout();
            SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
            if (db != null) {
                db.clearAll();
                db.clearQueue();
            }

            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        dialog.show();
    }
}
