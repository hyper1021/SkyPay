package com.pay.sky.ui;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import com.pay.sky.R;
import com.pay.sky.data.KeyValuePair;
import com.pay.sky.util.HapticUtil;
import com.pay.sky.util.PreferencesManager;
import java.util.List;

public class HeadersActivity extends BaseActivity {

    private KeyValueGridController gridController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_headers);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            finish();
        });

        LinearLayout gridContainer = findViewById(R.id.layoutHeadersGridContainer);
        PreferencesManager prefs = PreferencesManager.getInstance();

        gridController = new KeyValueGridController(this, gridContainer);
        List<KeyValuePair> rows = prefs.getHeaderRows();
        gridController.setItems(rows);

        findViewById(R.id.btnSaveHeaders).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            saveConfiguration();
        });
    }

    private void saveConfiguration() {
        PreferencesManager prefs = PreferencesManager.getInstance();
        List<KeyValuePair> validItems = gridController.getValidItems();
        prefs.saveHeaderRows(validItems);

        Toast.makeText(this, "Headers configuration saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
