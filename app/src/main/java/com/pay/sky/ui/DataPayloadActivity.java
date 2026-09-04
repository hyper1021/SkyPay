package com.pay.sky.ui;

import android.content.Intent;
import android.os.Bundle;

public class DataPayloadActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, PostBodyActivity.class);
        startActivity(intent);
        finish();
    }
}
