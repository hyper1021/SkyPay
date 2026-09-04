package com.pay.sky.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
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
    private ImageView ivThemeChevron;
    private ImageView ivOrientationChevron;
    private LinearLayout layoutThemeOptions;
    private LinearLayout layoutOrientationOptions;

    private ImageView ivCheckThemeSystem;
    private ImageView ivCheckThemeLight;
    private ImageView ivCheckThemeDark;

    private ImageView ivCheckOrientationSystem;
    private ImageView ivCheckOrientationPortrait;
    private ImageView ivCheckOrientationLandscape;

    private SwitchMaterial switchHaptic;
    private SwitchMaterial switchCapture;
    private SwitchMaterial switchHideNotifications;
    private TextView tvQueueSummary;

    private boolean isThemeExpanded = false;
    private boolean isOrientationExpanded = false;

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
        ivThemeChevron = findViewById(R.id.ivThemeChevron);
        ivOrientationChevron = findViewById(R.id.ivOrientationChevron);
        layoutThemeOptions = findViewById(R.id.layoutThemeOptions);
        layoutOrientationOptions = findViewById(R.id.layoutOrientationOptions);

        ivCheckThemeSystem = findViewById(R.id.ivCheckThemeSystem);
        ivCheckThemeLight = findViewById(R.id.ivCheckThemeLight);
        ivCheckThemeDark = findViewById(R.id.ivCheckThemeDark);

        ivCheckOrientationSystem = findViewById(R.id.ivCheckOrientationSystem);
        ivCheckOrientationPortrait = findViewById(R.id.ivCheckOrientationPortrait);
        ivCheckOrientationLandscape = findViewById(R.id.ivCheckOrientationLandscape);

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

        // Inline Expandable Theme Selector
        findViewById(R.id.rowThemeSelection).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            toggleThemeExpansion();
        });

        findViewById(R.id.optionThemeSystem).setOnClickListener(v -> selectTheme(ThemeHelper.MODE_SYSTEM));
        findViewById(R.id.optionThemeLight).setOnClickListener(v -> selectTheme(ThemeHelper.MODE_LIGHT));
        findViewById(R.id.optionThemeDark).setOnClickListener(v -> selectTheme(ThemeHelper.MODE_DARK));

        // Inline Expandable Orientation Selector
        findViewById(R.id.rowOrientationSelection).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            toggleOrientationExpansion();
        });

        findViewById(R.id.optionOrientationSystem).setOnClickListener(v -> selectOrientation(PreferencesManager.ORIENTATION_SYSTEM));
        findViewById(R.id.optionOrientationPortrait).setOnClickListener(v -> selectOrientation(PreferencesManager.ORIENTATION_PORTRAIT));
        findViewById(R.id.optionOrientationLandscape).setOnClickListener(v -> selectOrientation(PreferencesManager.ORIENTATION_LANDSCAPE));

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

        View rowHeaders = findViewById(R.id.rowHeaders);
        if (rowHeaders != null) {
            rowHeaders.setOnClickListener(v -> {
                HapticUtil.vibrateClick(this);
                startActivity(new Intent(this, HeadersActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            });
        }

        View rowPostBody = findViewById(R.id.rowPostBody);
        if (rowPostBody != null) {
            rowPostBody.setOnClickListener(v -> {
                HapticUtil.vibrateClick(this);
                startActivity(new Intent(this, PostBodyActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            });
        }

        View rowDataPayload = findViewById(R.id.rowDataPayload);
        if (rowDataPayload != null) {
            rowDataPayload.setOnClickListener(v -> {
                HapticUtil.vibrateClick(this);
                startActivity(new Intent(this, PostBodyActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            });
        }

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

    private void toggleThemeExpansion() {
        isThemeExpanded = !isThemeExpanded;
        if (layoutThemeOptions != null) {
            layoutThemeOptions.setVisibility(isThemeExpanded ? View.VISIBLE : View.GONE);
        }
        if (ivThemeChevron != null) {
            ivThemeChevron.animate().rotation(isThemeExpanded ? 90f : 0f).setDuration(200).start();
        }
        updateThemeCheckmarks();
    }

    private void selectTheme(int mode) {
        HapticUtil.vibrateClick(this);
        ThemeHelper.saveThemeMode(this, mode);
        updateThemeLabel();
        updateThemeCheckmarks();
        toggleThemeExpansion();
    }

    private void updateThemeCheckmarks() {
        int current = ThemeHelper.getSavedThemeMode(this);
        if (ivCheckThemeSystem != null) ivCheckThemeSystem.setVisibility(current == ThemeHelper.MODE_SYSTEM ? View.VISIBLE : View.GONE);
        if (ivCheckThemeLight != null) ivCheckThemeLight.setVisibility((current == ThemeHelper.MODE_LIGHT || current == ThemeHelper.THEME_LIGHT) ? View.VISIBLE : View.GONE);
        if (ivCheckThemeDark != null) ivCheckThemeDark.setVisibility((current == ThemeHelper.MODE_DARK || current == ThemeHelper.THEME_DARK) ? View.VISIBLE : View.GONE);
    }

    private void toggleOrientationExpansion() {
        isOrientationExpanded = !isOrientationExpanded;
        if (layoutOrientationOptions != null) {
            layoutOrientationOptions.setVisibility(isOrientationExpanded ? View.VISIBLE : View.GONE);
        }
        if (ivOrientationChevron != null) {
            ivOrientationChevron.animate().rotation(isOrientationExpanded ? 90f : 0f).setDuration(200).start();
        }
        updateOrientationCheckmarks();
    }

    private void selectOrientation(int mode) {
        HapticUtil.vibrateClick(this);
        PreferencesManager.getInstance().setOrientationMode(mode);
        updateOrientationLabel();
        updateOrientationCheckmarks();
        applyOrientationMode();
        toggleOrientationExpansion();
    }

    private void updateOrientationCheckmarks() {
        int current = PreferencesManager.getInstance().getOrientationMode();
        if (ivCheckOrientationSystem != null) ivCheckOrientationSystem.setVisibility(current == PreferencesManager.ORIENTATION_SYSTEM ? View.VISIBLE : View.GONE);
        if (ivCheckOrientationPortrait != null) ivCheckOrientationPortrait.setVisibility(current == PreferencesManager.ORIENTATION_PORTRAIT ? View.VISIBLE : View.GONE);
        if (ivCheckOrientationLandscape != null) ivCheckOrientationLandscape.setVisibility(current == PreferencesManager.ORIENTATION_LANDSCAPE ? View.VISIBLE : View.GONE);
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

    private void showLogoutDialog() {
        CustomConfirmationDialog.show(
                this,
                "lottie/logout_success.json",
                "Sign Out?",
                "Are you sure you want to log out? Your authenticated device token and local message records will be cleared from this device.",
                "Sign Out",
                "Cancel",
                true,
                () -> {
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
                }
        );
    }
}
