package com.pay.sky.util;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

public class HapticHelper {

    public static void performHaptic(View view) {
        PreferencesManager prefs = PreferencesManager.getInstance();
        if (prefs == null || !prefs.isHapticEnabled()) {
            return;
        }
        if (view != null) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            } catch (Exception ignored) {
            }
        }
    }

    public static void performActionHaptic(Context context) {
        PreferencesManager prefs = PreferencesManager.getInstance();
        if (prefs == null || !prefs.isHapticEnabled() || context == null) {
            return;
        }
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(18);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
