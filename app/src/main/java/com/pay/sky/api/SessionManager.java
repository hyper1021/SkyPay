package com.pay.sky.api;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "skypay_session";
    private static final String KEY_DEVICE_KEY = "device_key";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_LOGGED_IN = "is_logged_in";

    private static SessionManager instance;
    private final SharedPreferences prefs;

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
    }

    public static synchronized SessionManager getInstance() {
        return instance;
    }

    private SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String deviceKey, String email, String name) {
        prefs.edit()
                .putString(KEY_DEVICE_KEY, deviceKey)
                .putString(KEY_EMAIL, email)
                .putString(KEY_NAME, name)
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false)
                && getDeviceKey() != null && !getDeviceKey().isEmpty()
                && getUserEmail() != null && !getUserEmail().isEmpty();
    }

    /** আগের কোডের সাথে compatibility রাখতে getToken() → device_key রিটার্ন করে */
    public String getToken() {
        return prefs.getString(KEY_DEVICE_KEY, "");
    }

    public String getDeviceKey() {
        return prefs.getString(KEY_DEVICE_KEY, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getUserName() {
        return prefs.getString(KEY_NAME, "SkyPay User");
    }

    /** Backward-compat: getDeviceId() → device_key */
    public String getDeviceId() {
        return prefs.getString(KEY_DEVICE_KEY, "");
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
