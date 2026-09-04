package com.pay.sky.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pay.sky.R;
import com.pay.sky.data.SmsDatabaseHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    private final List<SmsDatabaseHelper.QueuedMessage> items = new ArrayList<>();

    public void setData(List<SmsDatabaseHelper.QueuedMessage> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue_message, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        SmsDatabaseHelper.QueuedMessage item = items.get(position);
        holder.tvSender.setText(item.sender != null ? item.sender : "Unknown");
        holder.tvBody.setText(item.body != null ? item.body : "");
        holder.tvTime.setText(formatTime(item.queuedAt));
        holder.tvReason.setText("Error: " + (item.errorReason != null ? item.errorReason : "Delivery failed"));
        holder.tvRetryCount.setText("Retries: " + item.retryCount);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatTime(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 60_000) return "Just now";
        if (diff < 3600_000) return (diff / 60_000) + "m ago";
        if (diff < 86400_000) return (diff / 3600_000) + "h ago";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
        return sdf.format(new Date(timestamp));
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvTime, tvBody, tvReason, tvRetryCount;

        QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvQueueSender);
            tvTime = itemView.findViewById(R.id.tvQueueTime);
            tvBody = itemView.findViewById(R.id.tvQueueBody);
            tvReason = itemView.findViewById(R.id.tvQueueReason);
            tvRetryCount = itemView.findViewById(R.id.tvQueueRetryCount);
        }
    }
}
