package com.pay.sky.data;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class KeyValuePair implements Serializable {
    private String key;
    private String value;

    public KeyValuePair() {
        this.key = "";
        this.value = "";
    }

    public KeyValuePair(String key, String value) {
        this.key = key != null ? key : "";
        this.value = value != null ? value : "";
    }

    public String getKey() {
        return key != null ? key : "";
    }

    public void setKey(String key) {
        this.key = key != null ? key : "";
    }

    public String getValue() {
        return value != null ? value : "";
    }

    public void setValue(String value) {
        this.value = value != null ? value : "";
    }

    public boolean isEmpty() {
        return getKey().trim().isEmpty() && getValue().trim().isEmpty();
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("key", getKey());
            obj.put("value", getValue());
        } catch (Exception ignored) {
        }
        return obj;
    }

    public static KeyValuePair fromJsonObject(JSONObject obj) {
        if (obj == null) return new KeyValuePair();
        String k = obj.optString("key", "");
        String v = obj.optString("value", "");
        return new KeyValuePair(k, v);
    }

    public static String listToJson(List<KeyValuePair> list) {
        JSONArray arr = new JSONArray();
        if (list != null) {
            for (KeyValuePair pair : list) {
                if (pair != null) {
                    arr.put(pair.toJsonObject());
                }
            }
        }
        return arr.toString();
    }

    public static List<KeyValuePair> listFromJson(String jsonStr) {
        List<KeyValuePair> list = new ArrayList<>();
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return list;
        }
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(fromJsonObject(obj));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public static List<KeyValuePair> getDefaultPostBodyRows() {
        List<KeyValuePair> list = new ArrayList<>();
        list.add(new KeyValuePair("sender", "${sender}"));
        list.add(new KeyValuePair("body", "${body}"));
        list.add(new KeyValuePair("receive_at", "${receive_at}"));
        list.add(new KeyValuePair("sim_slot", "${sim_slot}"));
        list.add(new KeyValuePair("device_id", "${device_id}"));
        return list;
    }

    public static List<KeyValuePair> getDefaultHeaderRows() {
        List<KeyValuePair> list = new ArrayList<>();
        list.add(new KeyValuePair("Authorization", "${auth_token}"));
        list.add(new KeyValuePair("Content-Type", "${type}"));
        return list;
    }
}
