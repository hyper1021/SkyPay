package com.pay.sky.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.pay.sky.R;
import com.pay.sky.data.SmsModel;
import com.pay.sky.util.HapticHelper;
import java.util.ArrayList;
import java.util.List;

public class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.SmsViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(SmsModel sms);
    }

    private final List<SmsModel> smsList = new ArrayList<>();
    private final OnItemClickListener listener;

    public SmsAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<SmsModel> list) {
        smsList.clear();
        if (list != null) {
            smsList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sms, parent, false);
        return new SmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
        SmsModel sms = smsList.get(position);
        Context context = holder.itemView.getContext();

        String sender = sms.getSender();
        holder.tvSender.setText(sender != null ? sender : "Unknown");

        String avatar = "S";
        if (sender != null && !sender.isEmpty()) {
            char first = sender.charAt(0);
            if (first == '+' && sender.length() > 1) {
                avatar = String.valueOf(sender.charAt(1)).toUpperCase();
            } else {
                avatar = String.valueOf(first).toUpperCase();
            }
        }
        holder.tvAvatar.setText(avatar);

        holder.tvTimestamp.setText(sms.getRelativeTimeSpan());
        holder.tvBody.setText(sms.getBody());
        holder.tvSimBadge.setText(sms.getSimSlotDisplay());

        if (sms.getReadStatus() == 0) {
            holder.tvSender.setTypeface(null, Typeface.BOLD);
            holder.tvBody.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));
        } else {
            holder.tvSender.setTypeface(null, Typeface.NORMAL);
            holder.tvBody.setTextColor(ContextCompat.getColor(context, R.color.colorTextSecondary));
        }

        holder.itemView.setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            if (listener != null) {
                listener.onItemClick(sms);
            }
        });
    }

    @Override
    public int getItemCount() {
        return smsList.size();
    }

    static class SmsViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar;
        TextView tvSender;
        TextView tvTimestamp;
        TextView tvSimBadge;
        TextView tvBody;

        public SmsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvSimBadge = itemView.findViewById(R.id.tvSimBadge);
            tvBody = itemView.findViewById(R.id.tvBody);
        }
    }
}
