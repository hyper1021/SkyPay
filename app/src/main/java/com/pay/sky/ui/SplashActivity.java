package com.pay.sky.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.pay.sky.R;
import com.pay.sky.api.ApiClient;
import com.pay.sky.api.SessionManager;
import com.pay.sky.util.ThemeHelper;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean hasNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SessionManager.init(this);
        SessionManager session = SessionManager.getInstance();

        if (session.isLoggedIn()) {
            String token = session.getToken();
            ApiClient.pingDevice(token, (ok, description, logout) -> {
                if (isFinishing() || hasNavigated) return;

                if (ok) {
                    navigateToMain();
                } else {
                    showPingNoticeDialog(description, logout);
                }
            });

            handler.postDelayed(() -> {
                if (!hasNavigated && !isFinishing()) {
                    navigateToMain();
                }
            }, 1800);
        } else {
            handler.postDelayed(() -> {
                if (!hasNavigated && !isFinishing()) {
                    navigateToLogin();
                }
            }, 1000);
        }
    }

    private void showPingNoticeDialog(String description, boolean logout) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_ping_error, null);
        dialog.setView(view);
        dialog.setCancelable(false);

        TextView tvMsg = view.findViewById(R.id.tvPingErrorMessage);
        if (description != null && !description.trim().isEmpty()) {
            tvMsg.setText(description);
        }

        MaterialButton btnOk = view.findViewById(R.id.btnPingErrorOk);
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            if (logout) {
                SessionManager.getInstance().logout();
                navigateToLogin();
            } else {
                navigateToMain();
            }
        });

        try {
            dialog.show();
        } catch (Exception e) {
            if (logout) navigateToLogin();
            else navigateToMain();
        }
    }

    private void navigateToMain() {
        if (hasNavigated) return;
        hasNavigated = true;
        Intent next = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(next);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void navigateToLogin() {
        if (hasNavigated) return;
        hasNavigated = true;
        Intent next = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(next);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}
