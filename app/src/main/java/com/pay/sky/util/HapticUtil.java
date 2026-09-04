package com.pay.sky.util;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

public class HapticUtil {

    public static void feedback(View view) {
        if (!PreferencesManager.getInstance().isHapticEnabled()) {
            return;
        }
        try {
            if (view != null) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        } catch (Exception ignored) {
        }
    }

    public static void vibrateClick(Context context) {
        if (!PreferencesManager.getInstance().isHapticEnabled() || context == null) {
            return;
        }
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(20);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
