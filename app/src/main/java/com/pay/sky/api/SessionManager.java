package com.pay.sky.api;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "skypay_session";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_DEVICE_ID = "device_id";
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

    public void saveSession(String token, String email, String name, String deviceId) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_EMAIL, email)
                .putString(KEY_NAME, name)
                .putString(KEY_DEVICE_ID, deviceId)
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false) && getToken() != null && !getToken().isEmpty();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getUserName() {
        return prefs.getString(KEY_NAME, "SkyPay User");
    }

    public String getDeviceId() {
        return prefs.getString(KEY_DEVICE_ID, "");
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
