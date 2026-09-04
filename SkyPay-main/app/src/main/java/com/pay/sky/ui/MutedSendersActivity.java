package com.pay.sky.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.pay.sky.R;
import com.pay.sky.data.SmsDatabaseHelper;
import java.util.ArrayList;
import java.util.List;

public class MutedSendersActivity extends BaseActivity {

    private EditText etSearch;
    private LinearLayout layoutEmpty;
    private RecyclerView rvMuted;
    private MutedAdapter adapter;
    private final List<String> fullList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_muted_senders);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etSearch = findViewById(R.id.etSearchMuted);
        layoutEmpty = findViewById(R.id.layoutEmptyMuted);
        rvMuted = findViewById(R.id.rvMutedSenders);

        adapter = new MutedAdapter();
        rvMuted.setLayoutManager(new LinearLayoutManager(this));
        rvMuted.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadMutedSenders();
    }

    private void loadMutedSenders() {
        SmsDatabaseHelper.init(this);
        fullList.clear();
        fullList.addAll(SmsDatabaseHelper.getInstance().getAllMutedSenders());
        filterList(etSearch.getText() != null ? etSearch.getText().toString() : "");
    }

    private void filterList(String query) {
        List<String> filtered = new ArrayList<>();
        String q = query.trim().toLowerCase();
        for (String s : fullList) {
            if (q.isEmpty() || s.toLowerCase().contains(q)) {
                filtered.add(s);
            }
        }
        adapter.setData(filtered);

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvMuted.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvMuted.setVisibility(View.VISIBLE);
        }
    }

    class MutedAdapter extends RecyclerView.Adapter<MutedAdapter.Holder> {

        private final List<String> items = new ArrayList<>();

        public void setData(List<String> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_muted_sender, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            String sender = items.get(position);
            holder.tvSender.setText(sender);
            holder.btnUnmute.setOnClickListener(v -> {
                SmsDatabaseHelper.getInstance().unmuteSender(sender);
                Toast.makeText(MutedSendersActivity.this, "Unmuted " + sender, Toast.LENGTH_SHORT).show();
                loadMutedSenders();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvSender;
            View btnUnmute;

            public Holder(@NonNull View itemView) {
                super(itemView);
                tvSender = itemView.findViewById(R.id.tvMutedSender);
                btnUnmute = itemView.findViewById(R.id.btnUnmute);
            }
        }
    }
}
