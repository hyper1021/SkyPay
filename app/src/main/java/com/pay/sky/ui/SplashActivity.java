package com.pay.sky.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.pay.sky.R;
import com.pay.sky.api.SessionManager;
import com.pay.sky.util.ThemeHelper;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView ivLogo = findViewById(R.id.ivSplashLogo);
        TextView tvTitle = findViewById(R.id.tvSplashTitle);
        TextView tvSubtitle = findViewById(R.id.tvSplashSubtitle);

        Animation anim = AnimationUtils.loadAnimation(this, R.anim.splash_anim);
        ivLogo.startAnimation(anim);
        tvTitle.startAnimation(anim);
        tvSubtitle.startAnimation(anim);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing()) {
                return;
            }
            SessionManager.init(this);
            Intent next;
            if (SessionManager.getInstance().isLoggedIn()) {
                next = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                next = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(next);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, 900);
    }
}
