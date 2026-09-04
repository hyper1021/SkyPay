package com.pay.sky.util;

import android.app.Activity;
import android.content.pm.ActivityInfo;

public class OrientationHelper {

    public static final int MODE_SYSTEM_DEFAULT = 0;
    public static final int MODE_PORTRAIT = 1;
    public static final int MODE_LANDSCAPE = 2;

    public static void applyOrientation(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        PreferencesManager prefs = PreferencesManager.getInstance();
        if (prefs == null) {
            return;
        }
        int mode = prefs.getOrientationMode();
        switch (mode) {
            case MODE_PORTRAIT:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case MODE_LANDSCAPE:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                break;
            case MODE_SYSTEM_DEFAULT:
            default:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;
        }
    }
}
