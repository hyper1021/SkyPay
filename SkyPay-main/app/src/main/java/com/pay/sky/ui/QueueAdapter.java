package com.pay.sky.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pay.sky.R;
import com.pay.sky.data.QueuedMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    private final List<QueuedMessage> list = new ArrayList<>();

    public void setData(List<QueuedMessage> data) {
        list.clear();
        if (data != null) {
            list.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queued_message, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        QueuedMessage qm = list.get(position);
        holder.tvSender.setText(qm.getSender() != null && !qm.getSender().trim().isEmpty() ? qm.getSender().trim() : "Unknown");

        int slot = qm.getSimSlot();
        String simText = slot >= 0 ? ("SIM " + (slot + 1)) : "SIM 1";
        holder.tvSimBadge.setText(simText);

        holder.tvAttempts.setText("Retry #" + qm.getAttempts());
        holder.tvBody.setText(qm.getBody());
        holder.tvReason.setText("Error: " + (qm.getErrorReason() != null ? qm.getErrorReason() : "Failed"));

        long time = qm.getCreatedAt();
        long diff = System.currentTimeMillis() - time;
        if (diff < 60000) {
            holder.tvTime.setText("Just now");
        } else if (diff < 3600000) {
            holder.tvTime.setText((diff / 60000) + "m ago");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.ENGLISH);
            holder.tvTime.setText(sdf.format(new Date(time)));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender;
        TextView tvSimBadge;
        TextView tvAttempts;
        TextView tvBody;
        TextView tvReason;
        TextView tvTime;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvQueueSender);
            tvSimBadge = itemView.findViewById(R.id.tvQueueSimBadge);
            tvAttempts = itemView.findViewById(R.id.tvQueueAttempts);
            tvBody = itemView.findViewById(R.id.tvQueueBody);
            tvReason = itemView.findViewById(R.id.tvQueueReason);
            tvTime = itemView.findViewById(R.id.tvQueueTime);
        }
    }
}
