package com.pay.sky.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import com.pay.sky.R;
import com.pay.sky.data.DataPayloadConfig;
import com.pay.sky.data.KeyValuePair;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.util.HapticUtil;
import com.pay.sky.util.PreferencesManager;
import java.util.List;

public class PostBodyActivity extends BaseActivity {

    private TextView tvSelectedMethod;
    private TextView tvSelectedContentType;
    private KeyValueGridController gridController;

    private String currentMethod = "POST";
    private String currentContentType = "Application JSON";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_body);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            finish();
        });

        tvSelectedMethod = findViewById(R.id.tvSelectedMethod);
        tvSelectedContentType = findViewById(R.id.tvSelectedContentType);
        LinearLayout gridContainer = findViewById(R.id.layoutPostBodyGridContainer);

        PreferencesManager prefs = PreferencesManager.getInstance();
        currentMethod = prefs.getHttpMethod();
        currentContentType = prefs.getPayloadContentType();

        tvSelectedMethod.setText(currentMethod);
        tvSelectedContentType.setText(currentContentType);

        findViewById(R.id.layoutMethodSelector).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            showMethodDropdown();
        });

        findViewById(R.id.layoutContentTypeSelector).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            showContentTypeDropdown();
        });

        gridController = new KeyValueGridController(this, gridContainer);
        List<KeyValuePair> rows = prefs.getPostBodyRows();
        gridController.setItems(rows);

        findViewById(R.id.btnSavePostBody).setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            saveConfiguration();
        });
    }

    private void showMethodDropdown() {
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.layoutMethodSelector));
        String[] methods = {"GET", "POST", "PUT", "PATCH", "DELETE"};
        for (int i = 0; i < methods.length; i++) {
            popup.getMenu().add(Menu.NONE, i, i, methods[i]);
        }
        popup.setOnMenuItemClickListener(item -> {
            HapticUtil.vibrateClick(this);
            currentMethod = methods[item.getItemId()];
            tvSelectedMethod.setText(currentMethod);
            return true;
        });
        popup.show();
    }

    private void showContentTypeDropdown() {
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.layoutContentTypeSelector));
        String[] types = {"Application JSON", "Form URL Encoded"};
        for (int i = 0; i < types.length; i++) {
            popup.getMenu().add(Menu.NONE, i, i, types[i]);
        }
        popup.setOnMenuItemClickListener(item -> {
            HapticUtil.vibrateClick(this);
            currentContentType = types[item.getItemId()];
            tvSelectedContentType.setText(currentContentType);
            return true;
        });
        popup.show();
    }

    private void saveConfiguration() {
        PreferencesManager prefs = PreferencesManager.getInstance();
        prefs.setHttpMethod(currentMethod);
        prefs.setPayloadContentType(currentContentType);

        List<KeyValuePair> validItems = gridController.getValidItems();
        prefs.savePostBodyRows(validItems);

        // Also update SmsDatabaseHelper DataPayloadConfig for full backwards compatibility
        try {
            SmsDatabaseHelper.init(this);
            DataPayloadConfig cfg = SmsDatabaseHelper.getInstance().getDataPayloadConfig();
            cfg.setHttpMethod(currentMethod);
            cfg.setContentType(currentContentType.toLowerCase().contains("form") ? "application/x-www-form-urlencoded" : "application/json");
            SmsDatabaseHelper.getInstance().saveDataPayloadConfig(cfg);
        } catch (Exception ignored) {
        }

        Toast.makeText(this, "Post Body configuration saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
