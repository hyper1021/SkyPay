package com.pay.sky.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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

        tvQueueCount = findViewById(R.id.tvQueueCount);
        btnRetryAllQueue = findViewById(R.id.btnRetryAllQueue);
        btnClearAllQueue = findViewById(R.id.btnClearAllQueue);
        rvQueueMessages = findViewById(R.id.rvQueueMessages);
        layoutEmptyQueue = findViewById(R.id.layoutEmptyQueue);

        adapter = new QueueAdapter();
        rvQueueMessages.setLayoutManager(new LinearLayoutManager(this));
        rvQueueMessages.setAdapter(adapter);

        btnRetryAllQueue.setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            btnRetryAllQueue.setEnabled(false);
            Toast.makeText(this, "Retrying delivery for queued messages...", Toast.LENGTH_SHORT).show();
            QueueDispatcher.retryQueueAsync(this, (successCount, failedCount) -> runOnUiThread(() -> {
                btnRetryAllQueue.setEnabled(true);
                Toast.makeText(this, "Retry finished: " + successCount + " sent, " + failedCount + " failed", Toast.LENGTH_SHORT).show();
                loadQueueData();
            }));
        });

        btnClearAllQueue.setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            CustomConfirmationDialog.show(
                    this,
                    "lottie/delete_confirmation.json",
                    "Clear Queue?",
                    "Are you sure you want to remove all pending queued messages? These delivery attempts will not be retried.",
                    "Clear All",
                    "Cancel",
                    true,
                    () -> {
                        SmsDatabaseHelper.getInstance().clearQueuedMessages();
                        Toast.makeText(this, "Queue cleared", Toast.LENGTH_SHORT).show();
                        loadQueueData();
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

        tvQueueCount.setText(list.size() + " Queued");
        if (list.isEmpty()) {
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
}
