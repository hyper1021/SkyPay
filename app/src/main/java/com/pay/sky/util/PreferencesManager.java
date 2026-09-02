package com.pay.sky.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREF_NAME = "skypay_prefs";
    private static final String KEY_READER_ENABLED = "key_reader_enabled";
    private static final String KEY_SESSION_COUNT = "key_session_count";
    private static final String KEY_WEBHOOK_ENABLED = "key_webhook_enabled";
    private static final String KEY_WEBHOOK_URL = "key_webhook_url";
    private static final String KEY_WEBHOOK_SECRET = "key_webhook_secret";
    private static final String KEY_AUTOSTART_BOOT = "key_autostart_boot";

    private static PreferencesManager instance;
    private final SharedPreferences prefs;

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context.getApplicationContext());
        }
    }

    public static synchronized PreferencesManager getInstance() {
        return instance;
    }

    private PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isReaderEnabled() {
        return prefs.getBoolean(KEY_READER_ENABLED, false);
    }

    public void setReaderEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_READER_ENABLED, enabled).apply();
    }

    public int getSessionSmsCount() {
        return prefs.getInt(KEY_SESSION_COUNT, 0);
    }

    public synchronized int incrementSessionSmsCount() {
        int count = getSessionSmsCount() + 1;
        prefs.edit().putInt(KEY_SESSION_COUNT, count).apply();
        return count;
    }

    public void resetSessionSmsCount() {
        prefs.edit().putInt(KEY_SESSION_COUNT, 0).apply();
    }

    public boolean isWebhookEnabled() {
        return prefs.getBoolean(KEY_WEBHOOK_ENABLED, false);
    }

    public void setWebhookEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_WEBHOOK_ENABLED, enabled).apply();
    }

    public String getWebhookUrl() {
        return prefs.getString(KEY_WEBHOOK_URL, "");
    }

    public void setWebhookUrl(String url) {
        prefs.edit().putString(KEY_WEBHOOK_URL, url).apply();
    }

    public String getWebhookSecret() {
        return prefs.getString(KEY_WEBHOOK_SECRET, "");
    }

    public void setWebhookSecret(String secret) {
        prefs.edit().putString(KEY_WEBHOOK_SECRET, secret).apply();
    }

    public boolean isAutoStartOnBoot() {
        return prefs.getBoolean(KEY_AUTOSTART_BOOT, true);
    }

    public void setAutoStartOnBoot(boolean autoStart) {
        prefs.edit().putBoolean(KEY_AUTOSTART_BOOT, autoStart).apply();
    }
}
