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
import com.google.android.material.card.MaterialCardView;
import com.pay.sky.R;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import com.pay.sky.receiver.SmsReceiver;
import com.pay.sky.ui.view.SmsBarChartView;
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

    private final BroadcastReceiver smsUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadDashboardData();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.setLogo(R.drawable.ic_skypay_logo);
        setSupportActionBar(toolbar);

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

        btnMode7Days.setOnClickListener(v -> setChartMode(7));
        btnMode30Days.setOnClickListener(v -> setChartMode(30));

        Button btnGrant = findViewById(R.id.btnGrantPermissions);
        btnGrant.setOnClickListener(v -> requestRequiredPermissions());

        tvStatusBadge.setOnClickListener(v -> toggleListenerState());
    }

    private void setChartMode(int days) {
        currentChartDays = days;
        if (days == 7) {
            btnMode7Days.setBackgroundResource(R.drawable.bg_btn_primary);
            btnMode7Days.setTextColor(0xFFFFFFFF);
            btnMode30Days.setBackgroundColor(0x00000000);
            btnMode30Days.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
        } else {
            btnMode30Days.setBackgroundResource(R.drawable.bg_btn_primary);
            btnMode30Days.setTextColor(0xFFFFFFFF);
            btnMode7Days.setBackgroundColor(0x00000000);
            btnMode7Days.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
        }
        updateChartData();
    }

    private void toggleListenerState() {
        PreferencesManager prefs = PreferencesManager.getInstance();
        boolean current = prefs.isReaderEnabled();
        if (current) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_pause_title)
                    .setMessage(R.string.dialog_pause_message)
                    .setPositiveButton(R.string.dialog_pause_confirm, (dialog, which) -> {
                        prefs.setReaderEnabled(false);
                        updateStatusPill();
                    })
                    .setNegativeButton(R.string.dialog_pause_cancel, null)
                    .show();
        } else {
            prefs.setReaderEnabled(true);
            updateStatusPill();
        }
    }

    private void updateStatusPill() {
        PreferencesManager prefs = PreferencesManager.getInstance();
        boolean enabled = prefs.isReaderEnabled();
        if (enabled) {
            tvStatusBadge.setText(R.string.status_active);
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_active);
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.colorSuccessDark));
            tvListenerModeSubtext.setText(R.string.status_subtext_active);
        } else {
            tvStatusBadge.setText(R.string.status_paused);
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_stopped);
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
            tvListenerModeSubtext.setText(R.string.status_subtext_paused);
        }
    }

    private void loadDashboardData() {
        updateStatusPill();
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
        tvTotalMessagesCount.setText(total + " records");

        updateChartData();
        updateSenderAnalytics(db);
        updateRecentTransactions(db);
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
            tvChartHighestDay.setText("Tap bar for daily transaction count");
        }
    }

    private void updateSenderAnalytics(SmsDatabaseHelper db) {
        layoutSenderAnalytics.removeAllViews();
        List<SmsDatabaseHelper.SenderStat> senders = db.getTopSenders(4);

        if (senders.isEmpty()) {
            tvEmptySenders.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptySenders.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (SmsDatabaseHelper.SenderStat stat : senders) {
            View view = inflater.inflate(R.layout.item_sender_analytic, layoutSenderAnalytics, false);
            TextView tvSenderName = view.findViewById(R.id.tvAnalyticSender);
            TextView tvSenderCount = view.findViewById(R.id.tvAnalyticCount);
            ProgressBar pbSender = view.findViewById(R.id.pbAnalyticPercent);

            tvSenderName.setText(stat.sender);
            tvSenderCount.setText(stat.count + " (" + String.format("%.0f%%", stat.percentage) + ")");
            pbSender.setProgress((int) stat.percentage);

            layoutSenderAnalytics.addView(view);
        }
    }

    private void updateRecentTransactions(SmsDatabaseHelper db) {
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            loadDashboardData();
            Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            return true;
        } else if (id == R.id.action_clear_history) {
            showClearHistoryDialog();
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
