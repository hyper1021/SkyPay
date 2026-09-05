package com.pay.sky.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.pay.sky.R;
import com.pay.sky.api.ApiClient;
import com.pay.sky.api.SessionManager;
import com.pay.sky.util.HapticUtil;
import org.json.JSONObject;

public class LoginActivity extends BaseActivity {

    private EditText etEmail;
    private EditText etDeviceKey;
    private MaterialCheckBox cbRemember;
    private MaterialButton btnLogin;
    private ProgressBar pbLoading;
    private boolean isLoggingIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etLoginEmail);
        etDeviceKey = findViewById(R.id.etLoginDeviceKey);
        cbRemember = findViewById(R.id.cbRememberMe);
        btnLogin = findViewById(R.id.btnLogin);
        pbLoading = findViewById(R.id.pbLoginLoading);

        SharedPreferences sp = getSharedPreferences("skypay_login_cache", Context.MODE_PRIVATE);
        String cachedEmail = sp.getString("saved_email", "");
        String cachedKey   = sp.getString("saved_device_key", "");
        if (!cachedEmail.isEmpty()) {
            etEmail.setText(cachedEmail);
        }
        if (!cachedKey.isEmpty()) {
            etDeviceKey.setText(cachedKey);
        }

        btnLogin.setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            attemptLogin();
        });

        findViewById(R.id.tvLoginHelp).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            Intent intent = new Intent(LoginActivity.this, HelpActivity.class);
            intent.putExtra("from_login", true);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });
    }

    private void attemptLogin() {
        if (isLoggingIn) return;

        String email     = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String deviceKey = etDeviceKey.getText() != null ? etDeviceKey.getText().toString().trim() : "";

        if (email.isEmpty()) {
            etEmail.setError("Email address is required");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        if (deviceKey.isEmpty()) {
            etDeviceKey.setError("Device authorization key is required");
            etDeviceKey.requestFocus();
            return;
        }
        if (deviceKey.length() < 4) {
            etDeviceKey.setError("Device key must be at least 4 characters");
            etDeviceKey.requestFocus();
            return;
        }

        setLoading(true);

        ApiClient.login(email, deviceKey, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);

                // Server response: { "ok": true, "description": "<uid>" }
                // description-এ আসা uid সেশনে দরকার নেই — শুধু email ও device_key রাখা হয়।
                SessionManager.init(LoginActivity.this);
                SessionManager.getInstance().saveSession(deviceKey, email, "SkyPay User");

                // Remember-me cache
                SharedPreferences sp = getSharedPreferences("skypay_login_cache", Context.MODE_PRIVATE);
                if (cbRemember.isChecked()) {
                    sp.edit()
                      .putString("saved_email", email)
                      .putString("saved_device_key", deviceKey)
                      .apply();
                } else {
                    sp.edit()
                      .remove("saved_email")
                      .remove("saved_device_key")
                      .apply();
                }

                Toast.makeText(LoginActivity.this, "Authentication successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        isLoggingIn = loading;
        btnLogin.setEnabled(!loading);
        if (loading) {
            btnLogin.setText("");
            pbLoading.setVisibility(View.VISIBLE);
        } else {
            btnLogin.setText("Sign In");
            pbLoading.setVisibility(View.GONE);
        }
    }
}
