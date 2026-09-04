package com.pay.sky.ui;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.pay.sky.R;
import com.pay.sky.data.DataPayloadConfig;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.util.HapticUtil;

public class DataPayloadActivity extends BaseActivity {

    private RadioGroup rgHttpMethod;
    private RadioButton rbMethodPost;
    private RadioButton rbMethodGet;
    private RadioButton rbMethodPut;
    private RadioButton rbMethodPatch;

    private RadioGroup rgContentType;
    private RadioButton rbTypeJson;
    private RadioButton rbTypeForm;

    private CheckBox cbIncludeSender;
    private EditText etKeySender;
    private CheckBox cbIncludeBody;
    private EditText etKeyBody;
    private CheckBox cbIncludeTimestamp;
    private EditText etKeyTimestamp;
    private CheckBox cbIncludeSimSlot;
    private EditText etKeySimSlot;
    private CheckBox cbIncludeDeviceId;
    private EditText etKeyDeviceId;

    private EditText etCustomHeaders;
    private EditText etCustomFields;
    private MaterialButton btnSavePayloadConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_payload);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            finish();
        });

        initViews();
        loadSavedConfig();

        btnSavePayloadConfig.setOnClickListener(v -> {
            HapticUtil.vibrateClick(this);
            saveConfig();
        });
    }

    private void initViews() {
        rgHttpMethod = findViewById(R.id.rgHttpMethod);
        rbMethodPost = findViewById(R.id.rbMethodPost);
        rbMethodGet = findViewById(R.id.rbMethodGet);
        rbMethodPut = findViewById(R.id.rbMethodPut);
        rbMethodPatch = findViewById(R.id.rbMethodPatch);

        rgContentType = findViewById(R.id.rgContentType);
        rbTypeJson = findViewById(R.id.rbTypeJson);
        rbTypeForm = findViewById(R.id.rbTypeForm);

        cbIncludeSender = findViewById(R.id.cbIncludeSender);
        etKeySender = findViewById(R.id.etKeySender);
        cbIncludeBody = findViewById(R.id.cbIncludeBody);
        etKeyBody = findViewById(R.id.etKeyBody);
        cbIncludeTimestamp = findViewById(R.id.cbIncludeTimestamp);
        etKeyTimestamp = findViewById(R.id.etKeyTimestamp);
        cbIncludeSimSlot = findViewById(R.id.cbIncludeSimSlot);
        etKeySimSlot = findViewById(R.id.etKeySimSlot);
        cbIncludeDeviceId = findViewById(R.id.cbIncludeDeviceId);
        etKeyDeviceId = findViewById(R.id.etKeyDeviceId);

        etCustomHeaders = findViewById(R.id.etCustomHeaders);
        etCustomFields = findViewById(R.id.etCustomFields);
        btnSavePayloadConfig = findViewById(R.id.btnSavePayloadConfig);
    }

    private void loadSavedConfig() {
        SmsDatabaseHelper.init(this);
        DataPayloadConfig cfg = SmsDatabaseHelper.getInstance().getDataPayloadConfig();

        String method = cfg.getHttpMethod();
        if ("GET".equalsIgnoreCase(method)) {
            rbMethodGet.setChecked(true);
        } else if ("PUT".equalsIgnoreCase(method)) {
            rbMethodPut.setChecked(true);
        } else if ("PATCH".equalsIgnoreCase(method)) {
            rbMethodPatch.setChecked(true);
        } else {
            rbMethodPost.setChecked(true);
        }

        if ("application/x-www-form-urlencoded".equalsIgnoreCase(cfg.getContentType())) {
            rbTypeForm.setChecked(true);
        } else {
            rbTypeJson.setChecked(true);
        }

        cbIncludeSender.setChecked(cfg.isIncludeSender());
        etKeySender.setText(cfg.getKeySender());

        cbIncludeBody.setChecked(cfg.isIncludeBody());
        etKeyBody.setText(cfg.getKeyBody());

        cbIncludeTimestamp.setChecked(cfg.isIncludeTimestamp());
        etKeyTimestamp.setText(cfg.getKeyTimestamp());

        cbIncludeSimSlot.setChecked(cfg.isIncludeSimSlot());
        etKeySimSlot.setText(cfg.getKeySimSlot());

        cbIncludeDeviceId.setChecked(cfg.isIncludeDeviceId());
        etKeyDeviceId.setText(cfg.getKeyDeviceId());

        etCustomHeaders.setText(cfg.getCustomHeaders());
        etCustomFields.setText(cfg.getCustomFields());
    }

    private void saveConfig() {
        DataPayloadConfig cfg = new DataPayloadConfig();

        if (rbMethodGet.isChecked()) {
            cfg.setHttpMethod("GET");
        } else if (rbMethodPut.isChecked()) {
            cfg.setHttpMethod("PUT");
        } else if (rbMethodPatch.isChecked()) {
            cfg.setHttpMethod("PATCH");
        } else {
            cfg.setHttpMethod("POST");
        }

        if (rbTypeForm.isChecked()) {
            cfg.setContentType("application/x-www-form-urlencoded");
        } else {
            cfg.setContentType("application/json");
        }

        cfg.setIncludeSender(cbIncludeSender.isChecked());
        String kSender = etKeySender.getText() != null ? etKeySender.getText().toString().trim() : "";
        cfg.setKeySender(kSender.isEmpty() ? "sender" : kSender);

        cfg.setIncludeBody(cbIncludeBody.isChecked());
        String kBody = etKeyBody.getText() != null ? etKeyBody.getText().toString().trim() : "";
        cfg.setKeyBody(kBody.isEmpty() ? "body" : kBody);

        cfg.setIncludeTimestamp(cbIncludeTimestamp.isChecked());
        String kTime = etKeyTimestamp.getText() != null ? etKeyTimestamp.getText().toString().trim() : "";
        cfg.setKeyTimestamp(kTime.isEmpty() ? "received_at" : kTime);

        cfg.setIncludeSimSlot(cbIncludeSimSlot.isChecked());
        String kSim = etKeySimSlot.getText() != null ? etKeySimSlot.getText().toString().trim() : "";
        cfg.setKeySimSlot(kSim.isEmpty() ? "sim_slot" : kSim);

        cfg.setIncludeDeviceId(cbIncludeDeviceId.isChecked());
        String kDev = etKeyDeviceId.getText() != null ? etKeyDeviceId.getText().toString().trim() : "";
        cfg.setKeyDeviceId(kDev.isEmpty() ? "device_id" : kDev);

        cfg.setCustomHeaders(etCustomHeaders.getText() != null ? etCustomHeaders.getText().toString().trim() : "");
        cfg.setCustomFields(etCustomFields.getText() != null ? etCustomFields.getText().toString().trim() : "");

        SmsDatabaseHelper.getInstance().saveDataPayloadConfig(cfg);
        Toast.makeText(this, "Payload configuration saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
