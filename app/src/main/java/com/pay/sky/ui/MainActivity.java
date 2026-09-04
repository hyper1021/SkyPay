package com.pay.sky.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.card.MaterialCardView;
import com.pay.sky.R;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import com.pay.sky.receiver.SmsReceiver;
import com.pay.sky.ui.view.SmsBarChartView;
import com.pay.sky.util.HapticUtil;
import com.pay.sky.util.PreferencesManager;
import com.pay.sky.util.QueueDispatcher;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements SmsAdapter.OnItemClickListener {

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
    private HorizontalScrollView scrollChart;
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
    private MenuItem itemPower;

    private final BroadcastReceiver smsUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadDashboardData();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        checkPingErrorDialog();
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
        scrollChart = findViewById(R.id.scrollChart);
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
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(() -> {
            HapticUtil.vibrateClick(this);
            loadDashboardData();
            new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefresh.setRefreshing(false), 500);
        });

        findViewById(R.id.btnGrantPermissions).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            requestSmsPermissions();
        });

        btnMode7Days.setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            setChartMode(7);
        });

        btnMode30Days.setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            setChartMode(30);
        });
    }

    private void checkPingErrorDialog() {
        boolean hasError = getIntent().getBooleanExtra("has_ping_error", false);
        if (hasError) {
            String desc = getIntent().getStringExtra("ping_error_desc");
            boolean logout = getIntent().getBooleanExtra("ping_logout", false);
            showPingErrorCustomDialog(desc, logout);
        }
    }

    private void showPingErrorCustomDialog(String desc, boolean logout) {
        CustomConfirmationDialog.show(
                this,
                "lottie/warning.json",
                "Connection Alert",
                (desc != null && !desc.trim().isEmpty()) ? desc.trim() : "Device token has expired or is unauthorized.",
                "OK",
                null,
                logout,
                () -> {
                    if (logout) {
                        SessionManager.init(this);
                        SessionManager.getInstance().logout();
                        SmsDatabaseHelper.init(this);
                        SmsDatabaseHelper.getInstance().clearAll();

                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        itemPower = menu.findItem(R.id.action_power);
        updatePowerMenuIcon();
        return true;
    }

    private void updatePowerMenuIcon() {
        if (itemPower == null) return;
        boolean isPaused = PreferencesManager.getInstance().isPaused();
        if (isPaused) {
            itemPower.setIcon(R.drawable.ic_play);
            itemPower.setTitle("Resume");
            tvStatusBadge.setText("PAUSED");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_stopped);
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.colorDanger));
            tvListenerModeSubtext.setText("Forwarding paused • Local capture active");
        } else {
            itemPower.setIcon(R.drawable.ic_power);
            itemPower.setTitle("Pause");
            tvStatusBadge.setText(R.string.status_active);
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_active);
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.colorSuccessDark));
            tvListenerModeSubtext.setText("Event-driven standby • Wakes on SMS");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_power) {
            HapticUtil.vibrateClick(this);
            handlePowerClick();
            return true;
        } else if (id == R.id.action_settings) {
            HapticUtil.vibrateClick(this);
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            return true;
        } else if (id == R.id.action_clear_history) {
            HapticUtil.vibrateClick(this);
            confirmClearHistory();
            return true;
        } else if (id == R.id.action_about) {
            HapticUtil.vibrateClick(this);
            startActivity(new Intent(this, AboutActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            return true;
        } else if (id == R.id.action_logout) {
            HapticUtil.vibrateClick(this);
            confirmSignOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handlePowerClick() {
        boolean isPaused = PreferencesManager.getInstance().isPaused();
        if (isPaused) {
            PreferencesManager.getInstance().setPaused(false);
            updatePowerMenuIcon();
            Toast.makeText(this, "SkyPay resumed", Toast.LENGTH_SHORT).show();
            QueueDispatcher.retryQueueAsync(this, null);
        } else {
            showPauseConfirmationDialog();
        }
    }

    private void showPauseConfirmationDialog() {
        CustomConfirmationDialog.show(
                this,
                "lottie/stop.json",
                "Pause POS Service?",
                "Are you sure you want to pause real-time SMS capture and webhook forwarding? The service will remain in standby until resumed.",
                "Pause Service",
                "Cancel",
                true,
                () -> {
                    PreferencesManager.getInstance().setPaused(true);
                    updatePowerMenuIcon();
                    Toast.makeText(this, "SkyPay paused", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void confirmClearHistory() {
        CustomConfirmationDialog.show(
                this,
                "lottie/emty.json",
                "Clear Message History?",
                "Are you sure you want to clear all message records from this device? Queued messages will also be removed.",
                "Clear All",
                "Cancel",
                true,
                () -> {
                    SmsDatabaseHelper.init(this);
                    SmsDatabaseHelper.getInstance().clearAll();
                    loadDashboardData();
                    Toast.makeText(this, "Message history cleared", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void confirmSignOut() {
        CustomConfirmationDialog.show(
                this,
                "lottie/sure.json",
                "Sign Out?",
                "Are you sure you want to sign out? Your authenticated device token and local message records will be cleared from this device.",
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
    private void setChartMode(int days) {
        currentChartDays = days;
        if (days == 7) {
            btnMode7Days.setBackgroundResource(R.drawable.bg_btn_primary);
            btnMode7Days.setTextColor(0xFFFFFFFF);
            btnMode30Days.setBackgroundColor(Color.TRANSPARENT);
            btnMode30Days.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));

            ViewGroup.LayoutParams lp = barChartView.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            barChartView.setLayoutParams(lp);
        } else {
            btnMode30Days.setBackgroundResource(R.drawable.bg_btn_primary);
            btnMode30Days.setTextColor(0xFFFFFFFF);
            btnMode7Days.setBackgroundColor(Color.TRANSPARENT);
            btnMode7Days.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));

            ViewGroup.LayoutParams lp = barChartView.getLayoutParams();
            lp.width = (int) (dpToPx(720));
            barChartView.setLayoutParams(lp);
            scrollChart.post(() -> scrollChart.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
        }

        loadChartData();
    }

    private void loadDashboardData() {
        SmsDatabaseHelper.init(this);
        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();

        int todayCount = db.getTodaySmsCount();
        int totalCount = db.getTotalSmsCount();
        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        int sevenDaysCount = db.getCountSince(sevenDaysAgo);
        int thirtyDaysCount = db.getCountSince(thirtyDaysAgo);

        tvStatToday.setText(String.valueOf(todayCount));
        tvStatTotal.setText(String.valueOf(totalCount));
        tvStat7Days.setText(String.valueOf(sevenDaysCount));
        tvStat30Days.setText(String.valueOf(thirtyDaysCount));
        tvTotalMessagesCount.setText(totalCount + " records");

        loadChartData();
        loadSenderAnalytics();
        loadRecentMessages();
        updatePowerMenuIcon();
    }

    private void loadChartData() {
        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        List<SmsDatabaseHelper.DailyStat> stats = db.getDailyCounts(currentChartDays);
        barChartView.setData(stats);

        int max = 0;
        String maxDate = "";
        for (SmsDatabaseHelper.DailyStat s : stats) {
            if (s.count > max) {
                max = s.count;
                maxDate = s.dateLabel;
            }
        }
        if (max > 0) {
            tvChartHighestDay.setText("Peak: " + max + " on " + maxDate);
        } else {
            tvChartHighestDay.setText("Tap bar for date details");
        }
    }

    private void loadSenderAnalytics() {
        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        List<SmsDatabaseHelper.SenderStat> senders = db.getTopSenders(5);

        layoutSenderAnalytics.removeAllViews();
        if (senders.isEmpty()) {
            tvEmptySenders.setVisibility(View.VISIBLE);
        } else {
            tvEmptySenders.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(this);
            for (SmsDatabaseHelper.SenderStat s : senders) {
                View itemView = inflater.inflate(R.layout.item_sender_analytic, layoutSenderAnalytics, false);
                TextView tvSender = itemView.findViewById(R.id.tvAnalyticSender);
                TextView tvCount = itemView.findViewById(R.id.tvAnalyticCount);
                ProgressBar pb = itemView.findViewById(R.id.pbAnalyticPercent);

                tvSender.setText(s.sender);
                tvCount.setText(s.count + " (" + String.format("%.0f", s.percentage) + "%)");
                pb.setProgress((int) Math.min(100, Math.max(0, s.percentage)));

                layoutSenderAnalytics.addView(itemView);
            }
        }
    }

    private void loadRecentMessages() {
        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        List<SmsModel> list = db.getRecentSms(10);
        smsAdapter.setData(list);

        if (list.isEmpty()) {
            layoutEmptyRecent.setVisibility(View.VISIBLE);
            rvRecentSms.setVisibility(View.GONE);
        } else {
            layoutEmptyRecent.setVisibility(View.GONE);
            rvRecentSms.setVisibility(View.VISIBLE);
        }
    }

    private void checkPermissionsState() {
        boolean receiveSms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean readSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
        if (!receiveSms || !readSms) {
            cardPermissionWarning.setVisibility(View.VISIBLE);
        } else {
            cardPermissionWarning.setVisibility(View.GONE);
        }
    }

    private void requestSmsPermissions() {
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
        HapticUtil.vibrateClick(this);
        Intent intent = new Intent(this, MessageDetailsActivity.class);
        intent.putExtra(MessageDetailsActivity.EXTRA_MESSAGE_ID, sms.getId());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
        checkPermissionsState();

        IntentFilter filter = new IntentFilter(SmsReceiver.ACTION_SMS_SAVED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smsUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(smsUpdateReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(smsUpdateReceiver);
        } catch (Exception ignored) {
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
