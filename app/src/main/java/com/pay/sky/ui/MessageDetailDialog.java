package com.pay.sky.ui;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.pay.sky.R;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;

public class MessageDetailDialog extends Dialog {

    public interface OnMessageDeletedListener {
        void onMessageDeleted(long id);
    }

    private final SmsModel sms;
    private final OnMessageDeletedListener deleteListener;

    public MessageDetailDialog(@NonNull Context context, SmsModel sms, OnMessageDeletedListener deleteListener) {
        super(context, R.style.Theme_SkyPay_Dialog);
        this.sms = sms;
        this.deleteListener = deleteListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_message_detail);

        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvSimBadge = findViewById(R.id.tvDetailSimBadge);
        TextView tvSender = findViewById(R.id.tvDetailSender);
        TextView tvTimestamp = findViewById(R.id.tvDetailTimestamp);
        TextView tvBody = findViewById(R.id.tvDetailBody);
        TextView tvSubId = findViewById(R.id.tvDetailSubId);
        TextView tvSmsId = findViewById(R.id.tvDetailSmsId);
        TextView tvThreadId = findViewById(R.id.tvDetailThreadId);
        TextView tvServiceCenter = findViewById(R.id.tvDetailServiceCenter);
        TextView tvReadStatus = findViewById(R.id.tvDetailReadStatus);

        tvSimBadge.setText(sms.getSimSlotDisplay());
        tvSender.setText(sms.getSender());
        tvTimestamp.setText(sms.getFormattedDate());
        tvBody.setText(sms.getBody());
        tvSubId.setText(sms.getSubId() >= 0 ? String.valueOf(sms.getSubId()) : "N/A");
        tvSmsId.setText(sms.getSmsId() != null ? sms.getSmsId() : String.valueOf(sms.getId()));
        tvThreadId.setText(sms.getThreadId() >= 0 ? String.valueOf(sms.getThreadId()) : "N/A");
        tvServiceCenter.setText(sms.getServiceCenter() != null ? sms.getServiceCenter() : "N/A");

        if (sms.getReadStatus() == 1) {
            tvReadStatus.setText(R.string.read_status);
            tvReadStatus.setTextColor(getContext().getResources().getColor(R.color.colorSuccessDark));
        } else {
            tvReadStatus.setText(R.string.unread_status);
            tvReadStatus.setTextColor(getContext().getResources().getColor(R.color.colorWarningDark));
        }

        SmsDatabaseHelper.getInstance().markAsRead(sms.getId());
        sms.setReadStatus(1);

        findViewById(R.id.btnCopyMessage).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                ClipData clip = ClipData.newPlainText("SkyPay SMS", sms.getBody());
                cm.setPrimaryClip(clip);
                Toast.makeText(getContext(), R.string.copied_toast, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnDeleteMessage).setOnClickListener(v -> {
            SmsDatabaseHelper.getInstance().deleteSms(sms.getId());
            Toast.makeText(getContext(), R.string.deleted_toast, Toast.LENGTH_SHORT).show();
            if (deleteListener != null) {
                deleteListener.onMessageDeleted(sms.getId());
            }
            dismiss();
        });

        findViewById(R.id.btnCloseDetail).setOnClickListener(v -> dismiss());
    }
}
