package com.pay.sky.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.pay.sky.R;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.util.HapticHelper;
import com.pay.sky.util.PaymentGatewayDispatcher;
import java.util.List;

public class QueueMessagesActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvQueue;
    private LinearLayout layoutEmpty;
    private QueueAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue_messages);

        Toolbar toolbar = findViewById(R.id.toolbarQueue);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            finish();
        });

        swipeRefresh = findViewById(R.id.swipeRefreshQueue);
        rvQueue = findViewById(R.id.rvQueueMessages);
        layoutEmpty = findViewById(R.id.layoutEmptyQueue);

        adapter = new QueueAdapter();
        rvQueue.setLayoutManager(new LinearLayoutManager(this));
        rvQueue.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadQueueData);

        loadQueueData();
    }

    private void loadQueueData() {
        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        if (db == null) return;

        List<SmsDatabaseHelper.QueuedMessage> messages = db.getQueuedMessages();
        adapter.setData(messages);

        if (messages.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvQueue.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvQueue.setVisibility(View.VISIBLE);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefresh.setRefreshing(false), 300);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Retry All").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 2, 1, "Clear Queue").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        HapticHelper.performHaptic(findViewById(android.R.id.content));
        if (item.getItemId() == 1) {
            Toast.makeText(this, "Retrying delivery for queued messages...", Toast.LENGTH_SHORT).show();
            PaymentGatewayDispatcher.retryQueue(this);
            new Handler(Looper.getMainLooper()).postDelayed(this::loadQueueData, 1500);
            return true;
        } else if (item.getItemId() == 2) {
            new AlertDialog.Builder(this)
                    .setTitle("Clear Queue?")
                    .setMessage("Are you sure you want to discard all queued webhook messages?")
                    .setPositiveButton("Clear", (d, w) -> {
                        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
                        if (db != null) db.clearQueue();
                        loadQueueData();
                        Toast.makeText(this, "Queue cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
