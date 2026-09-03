package com.pay.sky.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.pay.sky.R;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import com.pay.sky.receiver.SmsReceiver;
import com.pay.sky.util.PreferencesManager;
import com.pay.sky.util.SimHelper;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SmsAdapter.OnItemClickListener {

    private TextView tvStatusBadge;
    private TextView tvServiceSubtext;
    private TextView tvSessionCount;
    private TextView tvTotalCount;
    private TextView tvSimInfo;
    private MaterialButton btnToggleService;
    private MaterialCardView cardPermissionWarning;
    private EditText etSearch;
    private ImageView btnClearSearch;
    private TextView tvMessageListCount;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private RecyclerView rvSmsList;
    private SmsAdapter smsAdapter;

    private ActivityResultLauncher<String[]> permissionLauncher;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateServiceStatusUI();
            loadMessages();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        initViews();
        setupRecyclerView();
        setupPermissionLauncher();
        setupListeners();
    }

    private void initViews() {
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvServiceSubtext = findViewById(R.id.tvServiceSubtext);
        tvSessionCount = findViewById(R.id.tvSessionCount);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvSimInfo = findViewById(R.id.tvSimInfo);
        btnToggleService = findViewById(R.id.btnToggleService);
        cardPermissionWarning = findViewById(R.id.cardPermissionWarning);
        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        tvMessageListCount = findViewById(R.id.tvMessageListCount);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptyDesc = findViewById(R.id.tvEmptyDesc);
        rvSmsList = findViewById(R.id.rvSmsList);
    }

    private void setupRecyclerView() {
        smsAdapter = new SmsAdapter(this);
        rvSmsList.setLayoutManager(new LinearLayoutManager(this));
        rvSmsList.setAdapter(smsAdapter);
    }

    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    checkPermissionsState();
                    updateServiceStatusUI();
                }
        );
    }

    private void setupListeners() {
        btnToggleService.setOnClickListener(v -> handleToggleService());

        Button btnGrant = findViewById(R.id.btnGrantPermissions);
        btnGrant.setOnClickListener(v -> requestAppPermissions());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                if (s.length() > 0) {
                    btnClearSearch.setVisibility(View.VISIBLE);
                } else {
                    btnClearSearch.setVisibility(View.GONE);
                }
                loadMessages();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
    }

    private void handleToggleService() {
        PreferencesManager prefs = PreferencesManager.getInstance();
        if (prefs.isReaderEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_pause_title)
                    .setMessage(R.string.dialog_pause_message)
                    .setPositiveButton(R.string.dialog_pause_confirm, (dialog, which) -> {
                        prefs.setReaderEnabled(false);
                        updateServiceStatusUI();
                    })
                    .setNegativeButton(R.string.dialog_pause_cancel, null)
                    .show();
        } else {
            if (!hasRequiredPermissions()) {
                requestAppPermissions();
                return;
            }
            prefs.setReaderEnabled(true);
            updateServiceStatusUI();
        }
    }

    private void updateServiceStatusUI() {
        PreferencesManager prefs = PreferencesManager.getInstance();
        boolean enabled = prefs.isReaderEnabled();

        if (enabled) {
            tvStatusBadge.setText(R.string.status_active);
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_active);
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.colorSuccessDark));
            tvServiceSubtext.setText(R.string.status_subtext_active);

            btnToggleService.setText(R.string.pause_reader);
            btnToggleService.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
            btnToggleService.setIconResource(R.drawable.ic_power);
        } else {
            tvStatusBadge.setText(R.string.status_paused);
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_stopped);
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
            tvServiceSubtext.setText(R.string.status_subtext_paused);

            btnToggleService.setText(R.string.resume_reader);
            btnToggleService.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSuccess));
            btnToggleService.setIconResource(R.drawable.ic_check);
        }

        tvSessionCount.setText(String.valueOf(prefs.getSessionSmsCount()));
        tvTotalCount.setText(String.valueOf(SmsDatabaseHelper.getInstance().getSmsCount()));
        tvSimInfo.setText(SimHelper.getSimSummary(this));
    }

    private void loadMessages() {
        String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
        List<SmsModel> list = SmsDatabaseHelper.getInstance().getAllSms(query);
        smsAdapter.setData(list);

        tvMessageListCount.setText(list.size() + " items");

        if (list.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvSmsList.setVisibility(View.GONE);
            if (query.isEmpty()) {
                tvEmptyTitle.setText(R.string.empty_sms_title);
                tvEmptyDesc.setText(R.string.empty_sms_desc);
            } else {
                tvEmptyTitle.setText(R.string.empty_search_title);
                tvEmptyDesc.setText(R.string.empty_search_desc);
            }
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvSmsList.setVisibility(View.VISIBLE);
        }

        tvTotalCount.setText(String.valueOf(SmsDatabaseHelper.getInstance().getSmsCount()));
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

    private void requestAppPermissions() {
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
        MessageDetailDialog dialog = new MessageDetailDialog(this, sms, id -> {
            loadMessages();
            updateServiceStatusUI();
        });
        dialog.show();
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
            loadMessages();
            updateServiceStatusUI();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_clear_history) {
            showClearHistoryDialog();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showClearHistoryDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All SMS History?")
                .setMessage("This will permanently delete all stored SMS records from the local device database.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    SmsDatabaseHelper.getInstance().clearAll();
                    Toast.makeText(this, R.string.all_cleared_toast, Toast.LENGTH_SHORT).show();
                    loadMessages();
                    updateServiceStatusUI();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About SkyPay")
                .setMessage("SkyPay SMS Reader\nVersion 1.0.0\n\nEvent-driven SMS listener and local transaction store for future payment-gateway integration.")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionsState();
        updateServiceStatusUI();
        loadMessages();

        IntentFilter filter = new IntentFilter();
        filter.addAction(SmsReceiver.ACTION_SMS_RECEIVED_EVENT);
        ContextCompat.registerReceiver(this, updateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(updateReceiver);
        } catch (Exception ignored) {
        }
    }
}
