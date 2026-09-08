package com.pay.sky.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.TextView;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.pay.sky.R;
import com.pay.sky.util.HapticUtil;

public class CustomConfirmationDialog {

    public interface DialogActionListener {
        void onConfirmed();
    }

    public static Dialog show(Context context,
                               String lottieAssetPath,
                               String title,
                               String description,
                               String positiveText,
                               String negativeText,
                               boolean isDestructive,
                               DialogActionListener listener) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom_confirmation);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        LottieAnimationView lottieView = dialog.findViewById(R.id.dialogLottieAnimation);
        TextView tvTitle = dialog.findViewById(R.id.dialogTitle);
        TextView tvDescription = dialog.findViewById(R.id.dialogDescription);
        MaterialButton btnCancel = dialog.findViewById(R.id.dialogBtnCancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.dialogBtnConfirm);

        if (lottieAssetPath != null && !lottieAssetPath.isEmpty()) {
            try {
                lottieView.setAnimation(lottieAssetPath);
                lottieView.playAnimation();
            } catch (Exception ignored) {
            }
        }

        if (title != null) {
            tvTitle.setText(title);
        }
        if (description != null) {
            tvDescription.setText(description);
        }

        if (negativeText != null) {
            btnCancel.setText(negativeText);
        }
        if (positiveText != null) {
            btnConfirm.setText(positiveText);
        }

        if (isDestructive) {
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(0xFFEF4444));
        } else {
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(0xFF0284C7));
        }

        btnCancel.setOnClickListener(v -> {
            HapticUtil.vibrateClick(context);
            dialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            HapticUtil.vibrateClick(context);
            dialog.dismiss();
            if (listener != null) {
                listener.onConfirmed();
            }
        });

        dialog.setOnDismissListener(d -> {
            try {
                lottieView.cancelAnimation();
            } catch (Exception ignored) {
            }
        });

        dialog.show();
        return dialog;
    }
}
