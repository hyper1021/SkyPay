package com.pay.sky.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {

    public static final String PREF_THEME = "app_theme_mode";
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("skypay_prefs", Context.MODE_PRIVATE);
        int mode = prefs.getInt(PREF_THEME, THEME_SYSTEM);
        setThemeMode(mode);
    }

    public static void setThemeMode(int mode) {
        switch (mode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static int getSavedThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("skypay_prefs", Context.MODE_PRIVATE);
        return prefs.getInt(PREF_THEME, THEME_SYSTEM);
    }

    public static void saveThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences("skypay_prefs", Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_THEME, mode).apply();
        setThemeMode(mode);
    }
}
