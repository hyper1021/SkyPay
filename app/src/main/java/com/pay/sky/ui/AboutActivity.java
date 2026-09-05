package com.pay.sky.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import com.pay.sky.R;
import com.pay.sky.api.ApiConfig;
import com.pay.sky.util.HapticUtil;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            finish();
        });

        findViewById(R.id.btnWebsite).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            openUrl(ApiConfig.PUBLIC_WEBSITE_URL);
        });

        findViewById(R.id.btnTelegram).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            openTelegram("BD_Prime_Minister");
        });

        findViewById(R.id.btnWhatsapp).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            openWhatsapp("+8801761844968");
        });
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void openTelegram(String username) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://" + "resolve?domain=" + username));
            intent.setPackage("org.telegram.messenger");
            startActivity(intent);
        } catch (Exception e) {
            openUrl("https://t.me/" + username);
        }
    }

    private void openWhatsapp(String phone) {
        try {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + cleanPhone));
            intent.setPackage("com.whatsapp");
            startActivity(intent);
        } catch (Exception e) {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            openUrl("https://api.whatsapp.com/send?phone=" + cleanPhone);
        }
    }
}
