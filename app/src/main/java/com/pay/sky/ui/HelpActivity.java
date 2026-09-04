package com.pay.sky.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.widget.Toolbar;
import com.pay.sky.R;
import com.pay.sky.util.HapticUtil;

public class HelpActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        boolean fromLogin = getIntent().getBooleanExtra("from_login", false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            finish();
        });

        LinearLayout layoutLoginHelp = findViewById(R.id.layoutLoginHelp);
        LinearLayout layoutAppHelp = findViewById(R.id.layoutAppHelp);

        if (fromLogin) {
            toolbar.setTitle("Login Help & Device Tokens");
            layoutLoginHelp.setVisibility(View.VISIBLE);
            layoutAppHelp.setVisibility(View.GONE);
        } else {
            toolbar.setTitle("Help & Documentation");
            layoutLoginHelp.setVisibility(View.GONE);
            layoutAppHelp.setVisibility(View.VISIBLE);
        }
    }
}
