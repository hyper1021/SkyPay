package com.pay.sky.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import com.pay.sky.R;
import com.pay.sky.api.ApiClient;
import com.pay.sky.api.SessionManager;
import com.pay.sky.data.SmsDatabaseHelper;

public class SplashActivity extends BaseActivity {

    private boolean isNavigated = false;

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

        if (!SessionManager.getInstance().isLoggedIn()) {
            navigateTo(false, false, "");
            return;
        }

        if (!isInternetAvailable()) {
            navigateTo(true, false, "");
            return;
        }

        String token = SessionManager.getInstance().getToken();
        String devId = SessionManager.getInstance().getDeviceId();
        String email = SessionManager.getInstance().getUserEmail();

        ApiClient.pingDevice(token, devId, email, (ok, description, logout) ->
                navigateTo(true, !ok || logout, (description != null ? description : ""))
        );
    }

    private void navigateTo(boolean isLoggedIn, boolean hasError, String errorDesc) {
        if (isFinishing() || isNavigated) return;
        isNavigated = true;

        new Handler(Looper.getMainLooper()).post(() -> {
            if (isFinishing()) return;

            if (!isLoggedIn) {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return;
            }

            if (hasError) {
                boolean shouldLogout = errorDesc != null && !errorDesc.isEmpty();
                String desc = (errorDesc != null && !errorDesc.isEmpty())
                        ? errorDesc
                        : "Device token has expired or is unauthorized.";
                CustomConfirmationDialog.show(
                        this,
                        "lottie/warning.json",
                        "Connection Alert",
                        desc,
                        "OK",
                        null,
                        shouldLogout,
                        () -> {
                            if (shouldLogout) {
                                SessionManager.getInstance().logout();
                                SmsDatabaseHelper.init(this);
                                SmsDatabaseHelper.getInstance().clearAll();
                                Intent intent = new Intent(this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                finish();
                            } else {
                                goToDashboard();
                            }
                        }
                );
                return;
            }

            goToDashboard();
        });
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
