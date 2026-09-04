package com.pay.sky.api;

public class ApiConfig {
    public static final String DEFAULT_BASE_URL = "https://payv2.skypaybd.top";
    public static final String PUBLIC_WEBSITE_URL = "https://skypaybd.top";
    public static final String API_VERSION = "v1";
    public static final String LOGIN_ENDPOINT = "/api/login/device";
    public static final String SMS_DATA_ENDPOINT = "/api/add/sms/data";
    public static final String PING_ENDPOINT = "/api/device/ping";
    public static final int CONNECT_TIMEOUT_MS = 15000;
    public static final int READ_TIMEOUT_MS = 15000;
}
