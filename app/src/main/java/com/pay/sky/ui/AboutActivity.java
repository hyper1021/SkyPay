package com.pay.sky.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.pay.sky.R;
import com.pay.sky.util.HapticHelper;

public class AboutActivity extends AppCompatActivity {

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
            HapticHelper.performHaptic(v);
            finish();
        });

        findViewById(R.id.btnWebsite).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            openUrl("https://skypaybd.top");
        });

        findViewById(R.id.btnTelegram).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            openTelegram("BD_Prime_Minister");
        });

        findViewById(R.id.btnWhatsApp).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            openWhatsApp("+8801761844968");
        });
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No browser application found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openTelegram(String username) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=" + username));
        try {
            startActivity(intent);
        } catch (Exception e) {
            openUrl("https://t.me/" + username);
        }
    }

    private void openWhatsApp(String phoneNumber) {
        String cleaned = phoneNumber.replace("+", "").replace(" ", "").trim();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?phone=" + cleaned));
        try {
            startActivity(intent);
        } catch (Exception e) {
            openUrl("https://wa.me/" + cleaned);
        }
    }
}
