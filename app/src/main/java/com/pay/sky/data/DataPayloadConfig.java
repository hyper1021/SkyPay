package com.pay.sky.data;

import org.json.JSONObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DataPayloadConfig {
    private String httpMethod = "POST";
    private String contentType = "application/json";
    private boolean includeSender = true;
    private boolean includeBody = true;
    private boolean includeTimestamp = true;
    private boolean includeSimSlot = true;
    private boolean includeDeviceId = true;
    private String keySender = "sender";
    private String keyBody = "body";
    private String keyTimestamp = "received_at";
    private String keySimSlot = "sim_slot";
    private String keyDeviceId = "device_id";
    private String customHeaders = "";
    private String customFields = "";

    public String getHttpMethod() {
        return (httpMethod != null && !httpMethod.isEmpty()) ? httpMethod.toUpperCase() : "POST";
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getContentType() {
        return (contentType != null && !contentType.isEmpty()) ? contentType : "application/json";
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isIncludeSender() {
        return includeSender;
    }

    public void setIncludeSender(boolean includeSender) {
        this.includeSender = includeSender;
    }

    public boolean isIncludeBody() {
        return includeBody;
    }

    public void setIncludeBody(boolean includeBody) {
        this.includeBody = includeBody;
    }

    public boolean isIncludeTimestamp() {
        return includeTimestamp;
    }

    public void setIncludeTimestamp(boolean includeTimestamp) {
        this.includeTimestamp = includeTimestamp;
    }

    public boolean isIncludeSimSlot() {
        return includeSimSlot;
    }

    public void setIncludeSimSlot(boolean includeSimSlot) {
        this.includeSimSlot = includeSimSlot;
    }

    public boolean isIncludeDeviceId() {
        return includeDeviceId;
    }

    public void setIncludeDeviceId(boolean includeDeviceId) {
        this.includeDeviceId = includeDeviceId;
    }

    public String getKeySender() {
        return (keySender != null && !keySender.trim().isEmpty()) ? keySender.trim() : "sender";
    }

    public void setKeySender(String keySender) {
        this.keySender = keySender;
    }

    public String getKeyBody() {
        return (keyBody != null && !keyBody.trim().isEmpty()) ? keyBody.trim() : "body";
    }

    public void setKeyBody(String keyBody) {
        this.keyBody = keyBody;
    }

    public String getKeyTimestamp() {
        return (keyTimestamp != null && !keyTimestamp.trim().isEmpty()) ? keyTimestamp.trim() : "received_at";
    }

    public void setKeyTimestamp(String keyTimestamp) {
        this.keyTimestamp = keyTimestamp;
    }

    public String getKeySimSlot() {
        return (keySimSlot != null && !keySimSlot.trim().isEmpty()) ? keySimSlot.trim() : "sim_slot";
    }

    public void setKeySimSlot(String keySimSlot) {
        this.keySimSlot = keySimSlot;
    }

    public String getKeyDeviceId() {
        return (keyDeviceId != null && !keyDeviceId.trim().isEmpty()) ? keyDeviceId.trim() : "device_id";
    }

    public void setKeyDeviceId(String keyDeviceId) {
        this.keyDeviceId = keyDeviceId;
    }

    public String getCustomHeaders() {
        return customHeaders != null ? customHeaders : "";
    }

    public void setCustomHeaders(String customHeaders) {
        this.customHeaders = customHeaders;
    }

    public String getCustomFields() {
        return customFields != null ? customFields : "";
    }

    public void setCustomFields(String customFields) {
        this.customFields = customFields;
    }

    public Map<String, String> parseCustomHeaders() {
        Map<String, String> map = new HashMap<>();
        if (customHeaders == null || customHeaders.trim().isEmpty()) {
            return map;
        }
        String[] lines = customHeaders.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains(":")) {
                int idx = trimmed.indexOf(':');
                String k = trimmed.substring(0, idx).trim();
                String v = trimmed.substring(idx + 1).trim();
                if (!k.isEmpty()) {
                    map.put(k, v);
                }
            }
        }
        return map;
    }

    public Map<String, String> parseCustomFields() {
        Map<String, String> map = new HashMap<>();
        if (customFields == null || customFields.trim().isEmpty()) {
            return map;
        }
        String[] lines = customFields.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains(":")) {
                int idx = trimmed.indexOf(':');
                String k = trimmed.substring(0, idx).trim();
                String v = trimmed.substring(idx + 1).trim();
                if (!k.isEmpty()) {
                    map.put(k, v);
                }
            }
        }
        return map;
    }
}
