package com.pay.sky.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.pay.sky.R;
import com.pay.sky.data.QueuedMessage;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.util.HapticUtil;
import com.pay.sky.util.QueueDispatcher;
import java.util.List;

public class QueueMessagesActivity extends BaseActivity {

    private TextView tvQueueCount;
    private MaterialButton btnRetryAllQueue;
    private MaterialButton btnClearAllQueue;
    private RecyclerView rvQueueMessages;
    private LinearLayout layoutEmptyQueue;
    private QueueAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue_messages);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            finish();
        });

        tvQueueCount    = findViewById(R.id.tvQueueCount);
        btnRetryAllQueue = findViewById(R.id.btnRetryAllQueue);
        btnClearAllQueue = findViewById(R.id.btnClearAllQueue);
        rvQueueMessages  = findViewById(R.id.rvQueueMessages);
        layoutEmptyQueue = findViewById(R.id.layoutEmptyQueue);

        adapter = new QueueAdapter();
        rvQueueMessages.setLayoutManager(new LinearLayoutManager(this));
        rvQueueMessages.setAdapter(adapter);

        btnRetryAllQueue.setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);

            // ── Internet / server error message ───────────────────────────
            if (!isInternetAvailable()) {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                return;
            }

            btnRetryAllQueue.setEnabled(false);
            Toast.makeText(this, "Retrying delivery...", Toast.LENGTH_SHORT).show();

            QueueDispatcher.retryQueueAsync(this, new QueueDispatcher.QueueRetryListener() {
                @Override
                public void onCompleted(int successCount, int failedCount) {
                    runOnUiThread(() -> {
                        btnRetryAllQueue.setEnabled(true);
                        String msg;
                        if (failedCount == 0 && successCount > 0) {
                            msg = successCount + " message(s) sent successfully";
                        } else if (successCount == 0 && failedCount > 0) {
                            msg = "Server connection error. Will retry automatically.";
                        } else if (successCount > 0) {
                            msg = successCount + " sent, " + failedCount + " pending retry";
                        } else {
                            msg = "No messages to retry";
                        }
                        Toast.makeText(QueueMessagesActivity.this, msg, Toast.LENGTH_SHORT).show();
                        loadQueueData();
                    });
                }

                @Override
                public void onItemSent(long queueId) {
                    // Real-time: সফল হওয়া মাত্র list থেকে সরাও
                    runOnUiThread(() -> {
                        adapter.removeItem(queueId);
                        updateCountHeader();
                        updateEmptyState();
                    });
                }
            });
        });

        btnClearAllQueue.setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            CustomConfirmationDialog.show(
                    this,
                    "lottie/emty.json",
                    "Clear Queue?",
                    "Are you sure you want to remove all pending queued messages? These delivery attempts will not be retried.",
                    "Clear All",
                    "Cancel",
                    true,
                    new CustomConfirmationDialog.DialogActionListener() {
                        @Override
                        public void onConfirmed() {
                            SmsDatabaseHelper.getInstance().clearQueuedMessages();
                            Toast.makeText(QueueMessagesActivity.this, "Queue cleared", Toast.LENGTH_SHORT).show();
                            loadQueueData();
                        }
                    }
            );
        });

        loadQueueData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadQueueData();
    }

    private void loadQueueData() {
        SmsDatabaseHelper.init(this);
        List<QueuedMessage> list = SmsDatabaseHelper.getInstance().getQueuedMessages();
        adapter.setData(list);
        updateCountHeader();
        updateEmptyState();
    }

    private void updateCountHeader() {
        int count = adapter.getItemCount();
        tvQueueCount.setText(count + " Queued");
    }

    private void updateEmptyState() {
        int count = adapter.getItemCount();
        if (count == 0) {
            layoutEmptyQueue.setVisibility(View.VISIBLE);
            rvQueueMessages.setVisibility(View.GONE);
            btnRetryAllQueue.setVisibility(View.GONE);
            btnClearAllQueue.setVisibility(View.GONE);
        } else {
            layoutEmptyQueue.setVisibility(View.GONE);
            rvQueueMessages.setVisibility(View.VISIBLE);
            btnRetryAllQueue.setVisibility(View.VISIBLE);
            btnClearAllQueue.setVisibility(View.VISIBLE);
        }
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
