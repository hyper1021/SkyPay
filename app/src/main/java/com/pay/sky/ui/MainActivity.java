package com.pay.sky.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.pay.sky.R;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import com.pay.sky.receiver.SmsReceiver;
import com.pay.sky.ui.view.SmsBarChartView;
import com.pay.sky.util.HapticHelper;
import com.pay.sky.util.OrientationHelper;
import com.pay.sky.util.PreferencesManager;
import com.pay.sky.util.ThemeHelper;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SmsAdapter.OnItemClickListener {

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvStatusBadge;
    private TextView tvListenerModeSubtext;
    private TextView tvStatToday;
    private TextView tvStatTotal;
    private TextView tvStat7Days;
    private TextView tvStat30Days;
    private TextView tvChartHighestDay;
    private TextView btnMode7Days;
    private TextView btnMode30Days;
    private SmsBarChartView barChartView;
    private LinearLayout layoutSenderAnalytics;
    private TextView tvEmptySenders;
    private TextView tvTotalMessagesCount;
    private LinearLayout layoutEmptyRecent;
    private RecyclerView rvRecentSms;
    private SmsAdapter smsAdapter;
    private MaterialCardView cardPermissionWarning;

    private int currentChartDays = 7;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private MenuItem powerPauseMenuItem;

    private final BroadcastReceiver smsUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadDashboardData();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        OrientationHelper.applyOrientation(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        initViews();
        setupRecyclerView();
        setupPermissionLauncher();
        setupListeners();
        loadDashboardData();
        checkPermissionsState();
    }

    private void initViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvListenerModeSubtext = findViewById(R.id.tvListenerModeSubtext);
        tvStatToday = findViewById(R.id.tvStatToday);
        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStat7Days = findViewById(R.id.tvStat7Days);
        tvStat30Days = findViewById(R.id.tvStat30Days);
        tvChartHighestDay = findViewById(R.id.tvChartHighestDay);
        btnMode7Days = findViewById(R.id.btnMode7Days);
        btnMode30Days = findViewById(R.id.btnMode30Days);
        barChartView = findViewById(R.id.barChartView);
        layoutSenderAnalytics = findViewById(R.id.layoutSenderAnalytics);
        tvEmptySenders = findViewById(R.id.tvEmptySenders);
        tvTotalMessagesCount = findViewById(R.id.tvTotalMessagesCount);
        layoutEmptyRecent = findViewById(R.id.layoutEmptyRecent);
        rvRecentSms = findViewById(R.id.rvRecentSms);
        cardPermissionWarning = findViewById(R.id.cardPermissionWarning);
    }

    private void setupRecyclerView() {
        smsAdapter = new SmsAdapter(this);
        rvRecentSms.setLayoutManager(new LinearLayoutManager(this));
        rvRecentSms.setAdapter(smsAdapter);
    }

    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    checkPermissionsState();
                    loadDashboardData();
                }
        );
    }

    private void setupListeners() {
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary, R.color.colorSecondary);
        swipeRefresh.setOnRefreshListener(() -> {
            loadDashboardData();
            new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefresh.setRefreshing(false), 500);
        });

        btnMode7Days.setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            setChartMode(7);
        });

        btnMode30Days.setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            setChartMode(30);
        });

        Button btnGrant = findViewById(R.id.btnGrantPermissions);
        btnGrant.setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            requestRequiredPermissions();
        });

        tvStatusBadge.setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            handlePowerToggle();
        });
    }

    private void setChartMode(int days) {
        currentChartDays = days;
        if (days == 7) {
            btnMode7Days.setBackgroundResource(R.drawable.bg_btn_primary);
            btnMode7Days.setTextColor(0xFFFFFFFF);
            btnMode30Days.setBackgroundColor(0x00000000);
            btnMode30Days.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));

            ViewGroup.LayoutParams lp = barChartView.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            barChartView.setLayoutParams(lp);
        } else {
            btnMode30Days.setBackgroundResource(R.drawable.bg_btn_primary);
            btnMode30Days.setTextColor(0xFFFFFFFF);
            btnMode7Days.setBackgroundColor(0x00000000);
            btnMode7Days.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));

            int targetWidth = (int) (30 * getResources().getDisplayMetrics().density * 26);
            ViewGroup.LayoutParams lp = barChartView.getLayoutParams();
            lp.width = Math.max(targetWidth, getResources().getDisplayMetrics().widthPixels);
            barChartView.setLayoutParams(lp);
        }
        updateChartData();
    }

    private void handlePowerToggle() {
        PreferencesManager prefs = PreferencesManager.getInstance();
        boolean isPaused = prefs.isSystemPaused();
        if (isPaused) {
            prefs.setSystemPaused(false);
            updateStatusPill();
            updatePowerMenuIcon();
            Toast.makeText(this, "SkyPay resumed", Toast.LENGTH_SHORT).show();
        } else {
            showPauseConfirmationDialog();
        }
    }

    private void showPauseConfirmationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_pause, null);
        dialog.setView(view);

        MaterialButton btnCancel = view.findViewById(R.id.btnCancelPause);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmPause);
        ProgressBar pbLoading = view.findViewById(R.id.pbPauseLoading);

        btnCancel.setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            dialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            HapticHelper.performActionHaptic(this);
            btnConfirm.setEnabled(false);
            btnConfirm.setText("");
            pbLoading.setVisibility(View.VISIBLE);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                PreferencesManager.getInstance().setSystemPaused(true);
                updateStatusPill();
                updatePowerMenuIcon();
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "SkyPay paused", Toast.LENGTH_SHORT).show();
            }, 400);
        });

        dialog.show();
    }

    private void updateStatusPill() {
        PreferencesManager prefs = PreferencesManager.getInstance();
        boolean isPaused = prefs.isSystemPaused();
        if (!isPaused) {
            tvStatusBadge.setText(R.string.status_active);
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_active);
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.colorSuccessDark));
            tvListenerModeSubtext.setText("Event-driven standby • Wakes on SMS");
        } else {
            tvStatusBadge.setText(R.string.status_paused);
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_stopped);
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
            tvListenerModeSubtext.setText("Forwarding suspended • Tap to resume");
        }
    }

    private void updatePowerMenuIcon() {
        if (powerPauseMenuItem != null) {
            boolean isPaused = PreferencesManager.getInstance().isSystemPaused();
            if (isPaused) {
                powerPauseMenuItem.setIcon(R.drawable.ic_play);
                powerPauseMenuItem.setTitle("Resume");
            } else {
                powerPauseMenuItem.setIcon(R.drawable.ic_power);
                powerPauseMenuItem.setTitle("Pause");
            }
        }
    }

    private void loadDashboardData() {
        updateStatusPill();
        updatePowerMenuIcon();

        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        if (db == null) {
            return;
        }

        int total = db.getTotalSmsCount();
        int today = db.getTodaySmsCount();
        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        int last7 = db.getCountSince(sevenDaysAgo);
        int last30 = db.getCountSince(thirtyDaysAgo);

        tvStatToday.setText(String.valueOf(today));
        tvStatTotal.setText(String.valueOf(total));
        tvStat7Days.setText(String.valueOf(last7));
        tvStat30Days.setText(String.valueOf(last30));
        tvTotalMessagesCount.setText(total + (total == 1 ? " record" : " records"));

        updateChartData();
        updateSenderAnalytics(db);
        updateRecentMessages(db);
    }

    private void updateChartData() {
        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        if (db == null) return;

        List<SmsDatabaseHelper.DailyStat> stats = db.getDailyCounts(currentChartDays);
        barChartView.setData(stats);

        int max = 0;
        String maxDate = "N/A";
        for (SmsDatabaseHelper.DailyStat st : stats) {
            if (st.count > max) {
                max = st.count;
                maxDate = st.dateLabel;
            }
        }
        if (max > 0) {
            tvChartHighestDay.setText("Peak: " + max + " SMS on " + maxDate);
        } else {
            tvChartHighestDay.setText("Tap bar for daily details");
        }
    }

    private void updateSenderAnalytics(SmsDatabaseHelper db) {
        layoutSenderAnalytics.removeAllViews();
        List<SmsDatabaseHelper.SenderStat> senders = db.getTopSenders(5);

        if (senders.isEmpty()) {
            tvEmptySenders.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptySenders.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        int rank = 1;
        for (SmsDatabaseHelper.SenderStat stat : senders) {
            View view = inflater.inflate(R.layout.item_sender_analytic, layoutSenderAnalytics, false);
            TextView tvSenderName = view.findViewById(R.id.tvAnalyticSender);
            TextView tvSenderCount = view.findViewById(R.id.tvAnalyticCount);
            ProgressBar pbSender = view.findViewById(R.id.pbAnalyticPercent);

            tvSenderName.setText(rank + ". " + stat.sender);
            tvSenderCount.setText(stat.count + " (" + String.format("%.0f%%", stat.percentage) + ")");
            pbSender.setProgress((int) stat.percentage);

            layoutSenderAnalytics.addView(view);
            rank++;
        }
    }

    private void updateRecentMessages(SmsDatabaseHelper db) {
        List<SmsModel> recent = db.getRecentSms(10);
        smsAdapter.setData(recent);

        if (recent.isEmpty()) {
            layoutEmptyRecent.setVisibility(View.VISIBLE);
            rvRecentSms.setVisibility(View.GONE);
        } else {
            layoutEmptyRecent.setVisibility(View.GONE);
            rvRecentSms.setVisibility(View.VISIBLE);
        }
    }

    private boolean hasRequiredPermissions() {
        boolean sms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean notif = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            return sms && notif;
        }
        return sms;
    }

    private void checkPermissionsState() {
        if (!hasRequiredPermissions()) {
            cardPermissionWarning.setVisibility(View.VISIBLE);
        } else {
            cardPermissionWarning.setVisibility(View.GONE);
        }
    }

    private void requestRequiredPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.RECEIVE_SMS);
        perms.add(Manifest.permission.READ_SMS);
        perms.add(Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        permissionLauncher.launch(perms.toArray(new String[0]));
    }

    @Override
    public void onItemClick(SmsModel sms) {
        Intent intent = new Intent(this, MessageDetailsActivity.class);
        intent.putExtra(MessageDetailsActivity.EXTRA_MESSAGE_ID, sms.getId());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        powerPauseMenuItem = menu.findItem(R.id.action_power_pause);
        updatePowerMenuIcon();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        HapticHelper.performHaptic(findViewById(android.R.id.content));
        int id = item.getItemId();
        if (id == R.id.action_power_pause) {
            handlePowerToggle();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            return true;
        } else if (id == R.id.action_clear_history) {
            showClearHistoryDialog();
            return true;
        } else if (id == R.id.action_help) {
            Intent helpIntent = new Intent(this, HelpActivity.class);
            helpIntent.putExtra(HelpActivity.EXTRA_HELP_MODE, HelpActivity.MODE_USER_DOCS);
            startActivity(helpIntent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            return true;
        } else if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showClearHistoryDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All SMS History?")
                .setMessage("This will permanently delete all stored SMS records and reset dashboard statistics.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    SmsDatabaseHelper.getInstance().clearAll();
                    loadDashboardData();
                    Toast.makeText(this, R.string.all_cleared_toast, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionsState();
        loadDashboardData();

        IntentFilter filter = new IntentFilter();
        filter.addAction(SmsReceiver.ACTION_SMS_RECEIVED_EVENT);
        ContextCompat.registerReceiver(this, smsUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(smsUpdateReceiver);
        } catch (Exception ignored) {
        }
    }
}
