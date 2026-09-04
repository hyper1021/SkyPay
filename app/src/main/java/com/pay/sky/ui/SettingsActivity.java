package com.pay.sky.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.pay.sky.R;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.util.HapticUtil;
import com.pay.sky.util.PreferencesManager;
import com.pay.sky.util.ThemeHelper;

public class SettingsActivity extends BaseActivity {

    private TextView tvCurrentTheme;
    private TextView tvCurrentOrientation;
    private SwitchMaterial switchHaptic;
    private SwitchMaterial switchCapture;
    private SwitchMaterial switchHideNotifications;
    private TextView tvQueueSummary;

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
            HapticUtil.vibrateClick(this);
            finish();
        });

        tvCurrentTheme = findViewById(R.id.tvCurrentTheme);
        tvCurrentOrientation = findViewById(R.id.tvCurrentOrientation);
        switchHaptic = findViewById(R.id.switchHapticFeedback);
        switchCapture = findViewById(R.id.switchSettingsCapture);
        switchHideNotifications = findViewById(R.id.switchHideNotifications);
        tvQueueSummary = findViewById(R.id.tvQueueSummary);

        updateThemeLabel();
        updateOrientationLabel();

        PreferencesManager prefs = PreferencesManager.getInstance();

        switchHaptic.setChecked(prefs.isHapticEnabled());
        switchHaptic.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.setHapticEnabled(isChecked);
            if (isChecked) {
                HapticUtil.vibrateClick(this);
            }
        });

        switchCapture.setChecked(prefs.isReaderEnabled());
        switchCapture.setOnCheckedChangeListener((btn, isChecked) -> {
            HapticUtil.vibrateClick(this);
            prefs.setReaderEnabled(isChecked);
            Toast.makeText(this, isChecked ? "SMS capture enabled" : "SMS capture paused", Toast.LENGTH_SHORT).show();
        });

        switchHideNotifications.setChecked(prefs.isNotificationsHidden());
        switchHideNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
            HapticUtil.vibrateClick(this);
            prefs.setNotificationsHidden(isChecked);
            Toast.makeText(this, isChecked ? "Notifications hidden" : "Notifications enabled", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.rowThemeSelection).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            showThemeDialog();
        });

        findViewById(R.id.rowOrientationSelection).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            showOrientationDialog();
        });

        findViewById(R.id.rowMutedSenders).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            startActivity(new Intent(this, MutedSendersActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        findViewById(R.id.rowWebhookForwarding).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            startActivity(new Intent(this, WebhookSettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        findViewById(R.id.rowDataPayload).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            startActivity(new Intent(this, DataPayloadActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        findViewById(R.id.rowQueueMessages).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            startActivity(new Intent(this, QueueMessagesActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        findViewById(R.id.rowHelp).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            Intent intent = new Intent(this, HelpActivity.class);
            intent.putExtra("from_login", false);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        findViewById(R.id.rowAbout).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            startActivity(new Intent(this, AboutActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        findViewById(R.id.rowLogout).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            showLogoutDialog();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateQueueSummary();
    }

    private void updateQueueSummary() {
        SmsDatabaseHelper.init(this);
        int count = SmsDatabaseHelper.getInstance().getQueuedMessageCount();
        if (count > 0) {
            tvQueueSummary.setText(count + " pending delivery retry");
        } else {
            tvQueueSummary.setText("Review and retry failed webhook deliveries");
        }
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

    private void updateOrientationLabel() {
        int mode = PreferencesManager.getInstance().getOrientationMode();
        if (mode == PreferencesManager.ORIENTATION_PORTRAIT) {
            tvCurrentOrientation.setText("Portrait");
        } else if (mode == PreferencesManager.ORIENTATION_LANDSCAPE) {
            tvCurrentOrientation.setText("Landscape");
        } else {
            tvCurrentOrientation.setText("System Default");
        }
    }

    private void showThemeDialog() {
        String[] options = {"System Default", "Light", "Dark"};
        int current = ThemeHelper.getSavedThemeMode(this);
        new AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    HapticUtil.vibrateClick(this);
                    ThemeHelper.saveThemeMode(this, which);
                    updateThemeLabel();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showOrientationDialog() {
        String[] options = {"System Default", "Portrait", "Landscape"};
        int current = PreferencesManager.getInstance().getOrientationMode();
        new AlertDialog.Builder(this)
                .setTitle("Screen Orientation")
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    HapticUtil.vibrateClick(this);
                    PreferencesManager.getInstance().setOrientationMode(which);
                    updateOrientationLabel();
                    applyOrientationMode();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_logout_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.findViewById(R.id.btnCancelLogout).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            dialog.dismiss();
        });

        dialog.findViewById(R.id.btnConfirmLogout).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            dialog.dismiss();

            SessionManager.init(this);
            SessionManager.getInstance().logout();

            SmsDatabaseHelper.init(this);
            SmsDatabaseHelper.getInstance().clearAll();

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
