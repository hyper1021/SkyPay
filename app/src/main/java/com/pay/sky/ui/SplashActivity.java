package com.pay.sky.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import com.pay.sky.R;
import com.pay.sky.api.ApiClient;
import com.pay.sky.api.SessionManager;

public class SplashActivity extends BaseActivity {

    private boolean isFinished = false;
    private boolean pingErrorOccurred = false;
    private String pingErrorDescription = "";
    private boolean pingRequireLogout = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View indicator = findViewById(R.id.viewGestureIndicator);
        ObjectAnimator animator = ObjectAnimator.ofFloat(indicator, "translationX", 0f, dpToPx(92));
        animator.setDuration(700);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();

        SessionManager.init(this);
        if (SessionManager.getInstance().isLoggedIn()) {
            String token = SessionManager.getInstance().getToken();
            String devId = SessionManager.getInstance().getDeviceId();
            String email = SessionManager.getInstance().getUserEmail();

            ApiClient.pingDevice(token, devId, email, (ok, description, logout) -> {
                if (!ok) {
                    pingErrorOccurred = true;
                    pingErrorDescription = (description != null && !description.isEmpty())
                            ? description : "Device token has expired or is unauthorized.";
                    pingRequireLogout = logout;
                }
            });
        }

        new Handler(Looper.getMainLooper()).postDelayed(this::proceedToNextScreen, 1200);
    }

    private void proceedToNextScreen() {
        if (isFinishing() || isFinished) {
            return;
        }
        isFinished = true;

        Intent next;
        if (SessionManager.getInstance().isLoggedIn()) {
            next = new Intent(SplashActivity.this, MainActivity.class);
            if (pingErrorOccurred) {
                next.putExtra("has_ping_error", true);
                next.putExtra("ping_error_desc", pingErrorDescription);
                next.putExtra("ping_logout", pingRequireLogout);
            }
        } else {
            next = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(next);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
