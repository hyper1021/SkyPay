package com.pay.sky.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.pay.sky.R;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.util.HapticHelper;
import java.util.List;

public class DataPayloadActivity extends AppCompatActivity {

    private RadioGroup rgHttpMethod;
    private RadioGroup rgContentType;
    private RadioButton rbMethodPost, rbMethodGet, rbMethodPut, rbMethodPatch;
    private RadioButton rbContentJson, rbContentForm;

    private CheckBox cbIncludeSender, cbIncludeBody, cbIncludeTimestamp;
    private CheckBox cbIncludeSimSlot, cbIncludeDeviceModel, cbIncludeAndroidVersion;

    private EditText etKeySender, etKeyBody, etKeyTimestamp;
    private EditText etKeySimSlot, etKeyDeviceModel, etKeyAndroidVersion;

    private LinearLayout containerCustomHeaders;
    private LinearLayout containerCustomFields;
    private TextView tvEmptyHeaders;
    private TextView tvEmptyFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_payload);

        Toolbar toolbar = findViewById(R.id.toolbarPayload);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            finish();
        });

        initViews();
        loadSavedConfig();

        findViewById(R.id.btnAddHeader).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            showAddKeyValueDialog(true);
        });

        findViewById(R.id.btnAddField).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            showAddKeyValueDialog(false);
        });

        findViewById(R.id.btnSavePayloadConfig).setOnClickListener(v -> {
            HapticHelper.performHaptic(v);
            savePayloadConfiguration();
        });
    }

    private void initViews() {
        rgHttpMethod = findViewById(R.id.rgHttpMethod);
        rgContentType = findViewById(R.id.rgContentType);
        rbMethodPost = findViewById(R.id.rbMethodPost);
        rbMethodGet = findViewById(R.id.rbMethodGet);
        rbMethodPut = findViewById(R.id.rbMethodPut);
        rbMethodPatch = findViewById(R.id.rbMethodPatch);
        rbContentJson = findViewById(R.id.rbContentJson);
        rbContentForm = findViewById(R.id.rbContentForm);

        cbIncludeSender = findViewById(R.id.cbIncludeSender);
        cbIncludeBody = findViewById(R.id.cbIncludeBody);
        cbIncludeTimestamp = findViewById(R.id.cbIncludeTimestamp);
        cbIncludeSimSlot = findViewById(R.id.cbIncludeSimSlot);
        cbIncludeDeviceModel = findViewById(R.id.cbIncludeDeviceModel);
        cbIncludeAndroidVersion = findViewById(R.id.cbIncludeAndroidVersion);

        etKeySender = findViewById(R.id.etKeySender);
        etKeyBody = findViewById(R.id.etKeyBody);
        etKeyTimestamp = findViewById(R.id.etKeyTimestamp);
        etKeySimSlot = findViewById(R.id.etKeySimSlot);
        etKeyDeviceModel = findViewById(R.id.etKeyDeviceModel);
        etKeyAndroidVersion = findViewById(R.id.etKeyAndroidVersion);

        containerCustomHeaders = findViewById(R.id.containerCustomHeaders);
        containerCustomFields = findViewById(R.id.containerCustomFields);
        tvEmptyHeaders = findViewById(R.id.tvEmptyHeaders);
        tvEmptyFields = findViewById(R.id.tvEmptyFields);
    }

    private void loadSavedConfig() {
        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        if (db == null) return;

        SmsDatabaseHelper.PayloadConfig cfg = db.getPayloadConfig();
        if ("GET".equalsIgnoreCase(cfg.httpMethod)) {
            rbMethodGet.setChecked(true);
        } else if ("PUT".equalsIgnoreCase(cfg.httpMethod)) {
            rbMethodPut.setChecked(true);
        } else if ("PATCH".equalsIgnoreCase(cfg.httpMethod)) {
            rbMethodPatch.setChecked(true);
        } else {
            rbMethodPost.setChecked(true);
        }

        if ("application/x-www-form-urlencoded".equalsIgnoreCase(cfg.contentType)) {
            rbContentForm.setChecked(true);
        } else {
            rbContentJson.setChecked(true);
        }

        cbIncludeSender.setChecked(cfg.includeSender);
        etKeySender.setText(cfg.keySender);

        cbIncludeBody.setChecked(cfg.includeBody);
        etKeyBody.setText(cfg.keyBody);

        cbIncludeTimestamp.setChecked(cfg.includeTimestamp);
        etKeyTimestamp.setText(cfg.keyTimestamp);

        cbIncludeSimSlot.setChecked(cfg.includeSimSlot);
        etKeySimSlot.setText(cfg.keySimSlot);

        cbIncludeDeviceModel.setChecked(cfg.includeDeviceModel);
        etKeyDeviceModel.setText(cfg.keyDeviceModel);

        cbIncludeAndroidVersion.setChecked(cfg.includeAndroidVersion);
        etKeyAndroidVersion.setText(cfg.keyAndroidVersion);

        refreshCustomHeadersList();
        refreshCustomFieldsList();
    }

    private void refreshCustomHeadersList() {
        containerCustomHeaders.removeAllViews();
        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        if (db == null) return;

        List<SmsDatabaseHelper.KeyValueItem> headers = db.getCustomHeaders();
        if (headers.isEmpty()) {
            containerCustomHeaders.addView(tvEmptyHeaders);
            tvEmptyHeaders.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyHeaders.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (SmsDatabaseHelper.KeyValueItem item : headers) {
            View row = inflater.inflate(R.layout.item_custom_key_value, containerCustomHeaders, false);
            TextView tvText = row.findViewById(R.id.tvItemKey);
            TextView tvSub = row.findViewById(R.id.tvItemValue);
            ImageView ivDel = row.findViewById(R.id.btnDeleteItem);

            tvText.setText(item.key);
            tvSub.setText(item.value);
            ivDel.setOnClickListener(v -> {
                HapticHelper.performHaptic(v);
                db.deleteCustomHeader(item.id);
                refreshCustomHeadersList();
            });
            containerCustomHeaders.addView(row);
        }
    }

    private void refreshCustomFieldsList() {
        containerCustomFields.removeAllViews();
        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        if (db == null) return;

        List<SmsDatabaseHelper.KeyValueItem> fields = db.getCustomFields();
        if (fields.isEmpty()) {
            containerCustomFields.addView(tvEmptyFields);
            tvEmptyFields.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyFields.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (SmsDatabaseHelper.KeyValueItem item : fields) {
            View row = inflater.inflate(R.layout.item_custom_key_value, containerCustomFields, false);
            TextView tvText = row.findViewById(R.id.tvItemKey);
            TextView tvSub = row.findViewById(R.id.tvItemValue);
            ImageView ivDel = row.findViewById(R.id.btnDeleteItem);

            tvText.setText(item.key);
            tvSub.setText(item.value);
            ivDel.setOnClickListener(v -> {
                HapticHelper.performHaptic(v);
                db.deleteCustomField(item.id);
                refreshCustomFieldsList();
            });
            containerCustomFields.addView(row);
        }
    }

    private void showAddKeyValueDialog(boolean isHeader) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_key_value, null);
        dialog.setView(view);

        TextView tvTitle = view.findViewById(R.id.tvKeyValueDialogTitle);
        EditText etKey = view.findViewById(R.id.etDialogKey);
        EditText etVal = view.findViewById(R.id.etDialogValue);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancelKeyValue);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveKeyValue);

        if (isHeader) {
            tvTitle.setText("Add Custom Header");
            etKey.setHint("Header Name (e.g. X-API-KEY)");
            etVal.setHint("Header Value");
        } else {
            tvTitle.setText("Add Custom Static Field");
            etKey.setHint("Field Name (e.g. store_id)");
            etVal.setHint("Field Value");
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String k = etKey.getText().toString().trim();
            String val = etVal.getText().toString().trim();
            if (k.isEmpty()) {
                etKey.setError("Key cannot be empty");
                return;
            }
            SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
            if (db != null) {
                if (isHeader) {
                    db.addCustomHeader(k, val);
                    refreshCustomHeadersList();
                } else {
                    db.addCustomField(k, val);
                    refreshCustomFieldsList();
                }
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void savePayloadConfiguration() {
        SmsDatabaseHelper.PayloadConfig cfg = new SmsDatabaseHelper.PayloadConfig();

        int selectedMethodId = rgHttpMethod.getCheckedRadioButtonId();
        if (selectedMethodId == R.id.rbMethodGet) cfg.httpMethod = "GET";
        else if (selectedMethodId == R.id.rbMethodPut) cfg.httpMethod = "PUT";
        else if (selectedMethodId == R.id.rbMethodPatch) cfg.httpMethod = "PATCH";
        else cfg.httpMethod = "POST";

        int selectedContentId = rgContentType.getCheckedRadioButtonId();
        if (selectedContentId == R.id.rbContentForm) {
            cfg.contentType = "application/x-www-form-urlencoded";
        } else {
            cfg.contentType = "application/json";
        }

        cfg.includeSender = cbIncludeSender.isChecked();
        String kSender = etKeySender.getText().toString().trim();
        cfg.keySender = kSender.isEmpty() ? "sender" : kSender;

        cfg.includeBody = cbIncludeBody.isChecked();
        String kBody = etKeyBody.getText().toString().trim();
        cfg.keyBody = kBody.isEmpty() ? "body" : kBody;

        cfg.includeTimestamp = cbIncludeTimestamp.isChecked();
        String kTime = etKeyTimestamp.getText().toString().trim();
        cfg.keyTimestamp = kTime.isEmpty() ? "timestamp" : kTime;

        cfg.includeSimSlot = cbIncludeSimSlot.isChecked();
        String kSim = etKeySimSlot.getText().toString().trim();
        cfg.keySimSlot = kSim.isEmpty() ? "sim_slot" : kSim;

        cfg.includeDeviceModel = cbIncludeDeviceModel.isChecked();
        String kModel = etKeyDeviceModel.getText().toString().trim();
        cfg.keyDeviceModel = kModel.isEmpty() ? "device_model" : kModel;

        cfg.includeAndroidVersion = cbIncludeAndroidVersion.isChecked();
        String kAndroid = etKeyAndroidVersion.getText().toString().trim();
        cfg.keyAndroidVersion = kAndroid.isEmpty() ? "android_version" : kAndroid;

        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        if (db != null) {
            db.savePayloadConfig(cfg);
        }

        Toast.makeText(this, "Data Payload settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
