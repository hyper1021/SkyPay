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
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.pay.sky.R;
import com.pay.sky.api.ApiClient;
import com.pay.sky.api.SessionManager;
import com.pay.sky.util.HapticHelper;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

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
        if (!cachedEmail.isEmpty()) {
            etEmail.setText(cachedEmail);
        }

        btnLogin.setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            attemptLogin();
        });

        findViewById(R.id.tvLoginHelp).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            Intent helpIntent = new Intent(LoginActivity.this, HelpActivity.class);
            helpIntent.putExtra(HelpActivity.EXTRA_HELP_MODE, HelpActivity.MODE_LOGIN);
            startActivity(helpIntent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });
    }

    private void attemptLogin() {
        if (isLoggingIn) {
            return;
        }

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
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
                String token = response.optString("token", deviceKey);
                JSONObject userObj = response.optJSONObject("user");
                String name = (userObj != null) ? userObj.optString("name", "SkyPay Merchant") : "SkyPay Merchant";
                JSONObject devObj = response.optJSONObject("device");
                String devId = (devObj != null) ? devObj.optString("id", deviceKey) : deviceKey;

                SessionManager.init(LoginActivity.this);
                SessionManager.getInstance().saveSession(token, email, name, devId);

                SharedPreferences sp = getSharedPreferences("skypay_login_cache", Context.MODE_PRIVATE);
                if (cbRemember.isChecked()) {
                    sp.edit().putString("saved_email", email).apply();
                } else {
                    sp.edit().remove("saved_email").apply();
                }

                HapticHelper.performActionHaptic(LoginActivity.this);
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
                HapticHelper.performActionHaptic(LoginActivity.this);
                Toast.makeText(LoginActivity.this, message != null ? message : "Invalid email or password.", Toast.LENGTH_LONG).show();
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
