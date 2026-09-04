package com.pay.sky.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class SmsDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "skypay_sms.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_SMS = "received_sms";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SMS_ID = "sms_id";
    public static final String COLUMN_THREAD_ID = "thread_id";
    public static final String COLUMN_SENDER = "sender";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_SIM_SLOT = "sim_slot";
    public static final String COLUMN_SUB_ID = "sub_id";
    public static final String COLUMN_READ_STATUS = "read_status";
    public static final String COLUMN_SMS_TYPE = "sms_type";
    public static final String COLUMN_SERVICE_CENTER = "service_center";
    public static final String COLUMN_PROTOCOL_ID = "protocol_id";
    public static final String COLUMN_STATUS_ON_ICC = "status_on_icc";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_WEBHOOK_STATUS = "webhook_status";
    public static final String COLUMN_WEBHOOK_TIME = "webhook_time";

    public static final String TABLE_MUTED = "muted_senders";
    public static final String COLUMN_MUTED_ID = "_id";
    public static final String COLUMN_MUTED_SENDER = "sender";
    public static final String COLUMN_MUTED_CREATED_AT = "created_at";

    public static final String TABLE_SETTINGS = "app_settings";
    public static final String COLUMN_SETTING_KEY = "setting_key";
    public static final String COLUMN_SETTING_VAL = "setting_value";

    public static final String TABLE_QUEUE = "message_queue";
    public static final String COLUMN_Q_ID = "_id";
    public static final String COLUMN_Q_SMS_ID = "sms_id";
    public static final String COLUMN_Q_SENDER = "sender";
    public static final String COLUMN_Q_BODY = "body";
    public static final String COLUMN_Q_TIMESTAMP = "timestamp";
    public static final String COLUMN_Q_QUEUED_AT = "queued_at";
    public static final String COLUMN_Q_RETRY_COUNT = "retry_count";
    public static final String COLUMN_Q_LAST_RETRY = "last_retry_at";
    public static final String COLUMN_Q_ERROR = "error_reason";
    public static final String COLUMN_Q_PAYLOAD = "payload";
    public static final String COLUMN_Q_STATUS = "status";

    public static final String TABLE_PAYLOAD_CONFIG = "data_payload_config";
    public static final String COLUMN_CFG_ID = "_id";
    public static final String COLUMN_CFG_METHOD = "http_method";
    public static final String COLUMN_CFG_CONTENT_TYPE = "content_type";
    public static final String COLUMN_CFG_INC_SENDER = "include_sender";
    public static final String COLUMN_CFG_KEY_SENDER = "key_sender";
    public static final String COLUMN_CFG_INC_BODY = "include_body";
    public static final String COLUMN_CFG_KEY_BODY = "key_body";
    public static final String COLUMN_CFG_INC_TIMESTAMP = "include_timestamp";
    public static final String COLUMN_CFG_KEY_TIMESTAMP = "key_timestamp";
    public static final String COLUMN_CFG_INC_SIM = "include_sim_slot";
    public static final String COLUMN_CFG_KEY_SIM = "key_sim_slot";
    public static final String COLUMN_CFG_INC_MODEL = "include_device_model";
    public static final String COLUMN_CFG_KEY_MODEL = "key_device_model";
    public static final String COLUMN_CFG_INC_ANDROID = "include_android_version";
    public static final String COLUMN_CFG_KEY_ANDROID = "key_android_version";

    public static final String TABLE_CUSTOM_HEADERS = "custom_headers";
    public static final String COLUMN_HDR_ID = "_id";
    public static final String COLUMN_HDR_NAME = "header_name";
    public static final String COLUMN_HDR_VALUE = "header_value";

    public static final String TABLE_CUSTOM_FIELDS = "custom_fields";
    public static final String COLUMN_FLD_ID = "_id";
    public static final String COLUMN_FLD_KEY = "field_key";
    public static final String COLUMN_FLD_VALUE = "field_value";

    private static SmsDatabaseHelper instance;

    public static class DailyStat {
        public String dateLabel;
        public long timestamp;
        public int count;

        public DailyStat(String dateLabel, long timestamp, int count) {
            this.dateLabel = dateLabel;
            this.timestamp = timestamp;
            this.count = count;
        }
    }

    public static class SenderStat {
        public String sender;
        public int count;
        public float percentage;

        public SenderStat(String sender, int count, float percentage) {
            this.sender = sender;
            this.count = count;
            this.percentage = percentage;
        }
    }

    public static class QueuedMessage {
        public long id;
        public long smsId;
        public String sender;
        public String body;
        public long timestamp;
        public long queuedAt;
        public int retryCount;
        public long lastRetryAt;
        public String errorReason;
        public String payload;
        public String status;

        public QueuedMessage() {}
    }

    public static class PayloadConfig {
        public String httpMethod = "POST";
        public String contentType = "application/json";
        public boolean includeSender = true;
        public String keySender = "sender";
        public boolean includeBody = true;
        public String keyBody = "body";
        public boolean includeTimestamp = true;
        public String keyTimestamp = "timestamp";
        public boolean includeSimSlot = true;
        public String keySimSlot = "sim_slot";
        public boolean includeDeviceModel = true;
        public String keyDeviceModel = "device_model";
        public boolean includeAndroidVersion = true;
        public String keyAndroidVersion = "android_version";

        public PayloadConfig() {}
    }

    public static class KeyValueItem {
        public long id;
        public String key;
        public String value;

        public KeyValueItem(long id, String key, String value) {
            this.id = id;
            this.key = key;
            this.value = value;
        }
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new SmsDatabaseHelper(context.getApplicationContext());
        }
    }

    public static synchronized SmsDatabaseHelper getInstance() {
        return instance;
    }

    public SmsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createSmsTable = "CREATE TABLE " + TABLE_SMS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_SMS_ID + " TEXT, "
                + COLUMN_THREAD_ID + " INTEGER, "
                + COLUMN_SENDER + " TEXT, "
                + COLUMN_BODY + " TEXT, "
                + COLUMN_TIMESTAMP + " INTEGER, "
                + COLUMN_SIM_SLOT + " INTEGER, "
                + COLUMN_SUB_ID + " INTEGER, "
                + COLUMN_READ_STATUS + " INTEGER, "
                + COLUMN_SMS_TYPE + " TEXT, "
                + COLUMN_SERVICE_CENTER + " TEXT, "
                + COLUMN_PROTOCOL_ID + " INTEGER, "
                + COLUMN_STATUS_ON_ICC + " INTEGER, "
                + COLUMN_CREATED_AT + " INTEGER, "
                + COLUMN_WEBHOOK_STATUS + " TEXT DEFAULT 'pending', "
                + COLUMN_WEBHOOK_TIME + " INTEGER DEFAULT 0);";
        db.execSQL(createSmsTable);

        String createMutedTable = "CREATE TABLE IF NOT EXISTS " + TABLE_MUTED + " ("
                + COLUMN_MUTED_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_MUTED_SENDER + " TEXT UNIQUE, "
                + COLUMN_MUTED_CREATED_AT + " INTEGER);";
        db.execSQL(createMutedTable);

        String createSettingsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS + " ("
                + COLUMN_SETTING_KEY + " TEXT PRIMARY KEY, "
                + COLUMN_SETTING_VAL + " TEXT);";
        db.execSQL(createSettingsTable);

        createV3Tables(db);
    }

    private void createV3Tables(SQLiteDatabase db) {
        String createQueueTable = "CREATE TABLE IF NOT EXISTS " + TABLE_QUEUE + " ("
                + COLUMN_Q_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_Q_SMS_ID + " INTEGER, "
                + COLUMN_Q_SENDER + " TEXT, "
                + COLUMN_Q_BODY + " TEXT, "
                + COLUMN_Q_TIMESTAMP + " INTEGER, "
                + COLUMN_Q_QUEUED_AT + " INTEGER, "
                + COLUMN_Q_RETRY_COUNT + " INTEGER DEFAULT 0, "
                + COLUMN_Q_LAST_RETRY + " INTEGER DEFAULT 0, "
                + COLUMN_Q_ERROR + " TEXT, "
                + COLUMN_Q_PAYLOAD + " TEXT, "
                + COLUMN_Q_STATUS + " TEXT DEFAULT 'pending');";
        db.execSQL(createQueueTable);

        String createPayloadTable = "CREATE TABLE IF NOT EXISTS " + TABLE_PAYLOAD_CONFIG + " ("
                + COLUMN_CFG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_CFG_METHOD + " TEXT DEFAULT 'POST', "
                + COLUMN_CFG_CONTENT_TYPE + " TEXT DEFAULT 'application/json', "
                + COLUMN_CFG_INC_SENDER + " INTEGER DEFAULT 1, "
                + COLUMN_CFG_KEY_SENDER + " TEXT DEFAULT 'sender', "
                + COLUMN_CFG_INC_BODY + " INTEGER DEFAULT 1, "
                + COLUMN_CFG_KEY_BODY + " TEXT DEFAULT 'body', "
                + COLUMN_CFG_INC_TIMESTAMP + " INTEGER DEFAULT 1, "
                + COLUMN_CFG_KEY_TIMESTAMP + " TEXT DEFAULT 'timestamp', "
                + COLUMN_CFG_INC_SIM + " INTEGER DEFAULT 1, "
                + COLUMN_CFG_KEY_SIM + " TEXT DEFAULT 'sim_slot', "
                + COLUMN_CFG_INC_MODEL + " INTEGER DEFAULT 1, "
                + COLUMN_CFG_KEY_MODEL + " TEXT DEFAULT 'device_model', "
                + COLUMN_CFG_INC_ANDROID + " INTEGER DEFAULT 1, "
                + COLUMN_CFG_KEY_ANDROID + " TEXT DEFAULT 'android_version');";
        db.execSQL(createPayloadTable);

        String createHeadersTable = "CREATE TABLE IF NOT EXISTS " + TABLE_CUSTOM_HEADERS + " ("
                + COLUMN_HDR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_HDR_NAME + " TEXT, "
                + COLUMN_HDR_VALUE + " TEXT);";
        db.execSQL(createHeadersTable);

        String createFieldsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_CUSTOM_FIELDS + " ("
                + COLUMN_FLD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_FLD_KEY + " TEXT, "
                + COLUMN_FLD_VALUE + " TEXT);";
        db.execSQL(createFieldsTable);

        ContentValues initCfg = new ContentValues();
        initCfg.put(COLUMN_CFG_METHOD, "POST");
        initCfg.put(COLUMN_CFG_CONTENT_TYPE, "application/json");
        initCfg.put(COLUMN_CFG_INC_SENDER, 1);
        initCfg.put(COLUMN_CFG_KEY_SENDER, "sender");
        initCfg.put(COLUMN_CFG_INC_BODY, 1);
        initCfg.put(COLUMN_CFG_KEY_BODY, "body");
        initCfg.put(COLUMN_CFG_INC_TIMESTAMP, 1);
        initCfg.put(COLUMN_CFG_KEY_TIMESTAMP, "timestamp");
        initCfg.put(COLUMN_CFG_INC_SIM, 1);
        initCfg.put(COLUMN_CFG_KEY_SIM, "sim_slot");
        initCfg.put(COLUMN_CFG_INC_MODEL, 1);
        initCfg.put(COLUMN_CFG_KEY_MODEL, "device_model");
        initCfg.put(COLUMN_CFG_INC_ANDROID, 1);
        initCfg.put(COLUMN_CFG_KEY_ANDROID, "android_version");
        db.insertWithOnConflict(TABLE_PAYLOAD_CONFIG, null, initCfg, SQLiteDatabase.CONFLICT_IGNORE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_SMS + " ADD COLUMN " + COLUMN_WEBHOOK_STATUS + " TEXT DEFAULT 'pending'");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_SMS + " ADD COLUMN " + COLUMN_WEBHOOK_TIME + " INTEGER DEFAULT 0");
            } catch (Exception ignored) {}
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MUTED + " ("
                    + COLUMN_MUTED_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_MUTED_SENDER + " TEXT UNIQUE, "
                    + COLUMN_MUTED_CREATED_AT + " INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS + " ("
                    + COLUMN_SETTING_KEY + " TEXT PRIMARY KEY, "
                    + COLUMN_SETTING_VAL + " TEXT);");
        }
        if (oldVersion < 3) {
            createV3Tables(db);
        }
    }

    public synchronized long insertSms(SmsModel sms) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SMS_ID, sms.getSmsId());
        values.put(COLUMN_THREAD_ID, sms.getThreadId());
        values.put(COLUMN_SENDER, sms.getSender());
        values.put(COLUMN_BODY, sms.getBody());
        values.put(COLUMN_TIMESTAMP, sms.getTimestamp());
        values.put(COLUMN_SIM_SLOT, sms.getSimSlot());
        values.put(COLUMN_SUB_ID, sms.getSubId());
        values.put(COLUMN_READ_STATUS, sms.getReadStatus());
        values.put(COLUMN_SMS_TYPE, sms.getSmsType());
        values.put(COLUMN_SERVICE_CENTER, sms.getServiceCenter());
        values.put(COLUMN_PROTOCOL_ID, sms.getProtocolId());
        values.put(COLUMN_STATUS_ON_ICC, sms.getStatusOnIcc());
        values.put(COLUMN_CREATED_AT, sms.getCreatedAt());
        values.put(COLUMN_WEBHOOK_STATUS, sms.getWebhookStatus());
        values.put(COLUMN_WEBHOOK_TIME, sms.getWebhookTime());

        long rowId = db.insert(TABLE_SMS, null, values);
        sms.setId(rowId);
        return rowId;
    }

    public synchronized void updateWebhookStatus(long id, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_WEBHOOK_STATUS, status);
        cv.put(COLUMN_WEBHOOK_TIME, System.currentTimeMillis());
        db.update(TABLE_SMS, cv, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }

    public synchronized int getTotalSmsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SMS, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
    }

    public synchronized int getTodaySmsCount() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SMS + " WHERE " + COLUMN_TIMESTAMP + " >= ?",
                    new String[]{String.valueOf(startOfDay)});
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
    }

    public synchronized int getCountSince(long timeMillis) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SMS + " WHERE " + COLUMN_TIMESTAMP + " >= ?",
                    new String[]{String.valueOf(timeMillis)});
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
    }

    public synchronized List<DailyStat> getDailyCounts(int days) {
        List<DailyStat> list = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
        SQLiteDatabase db = this.getReadableDatabase();

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        for (int i = days - 1; i >= 0; i--) {
            Calendar dayStart = (Calendar) cal.clone();
            dayStart.add(Calendar.DAY_OF_YEAR, -i);
            long start = dayStart.getTimeInMillis();

            Calendar dayEnd = (Calendar) dayStart.clone();
            dayEnd.add(Calendar.DAY_OF_YEAR, 1);
            long end = dayEnd.getTimeInMillis();

            int count = 0;
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SMS + " WHERE " + COLUMN_TIMESTAMP + " >= ? AND " + COLUMN_TIMESTAMP + " < ?",
                        new String[]{String.valueOf(start), String.valueOf(end)});
                if (cursor != null && cursor.moveToFirst()) {
                    count = cursor.getInt(0);
                }
            } finally {
                if (cursor != null) cursor.close();
            }

            list.add(new DailyStat(sdf.format(new Date(start)), start, count));
        }

        return list;
    }

    public synchronized List<SenderStat> getTopSenders(int limit) {
        List<SenderStat> list = new ArrayList<>();
        int total = getTotalSmsCount();
        if (total == 0) {
            return list;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT " + COLUMN_SENDER + ", COUNT(*) as c FROM " + TABLE_SMS
                    + " GROUP BY " + COLUMN_SENDER + " ORDER BY c DESC LIMIT " + limit, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String sender = cursor.getString(0);
                    int count = cursor.getInt(1);
                    float pct = (count * 100.0f) / total;
                    list.add(new SenderStat(sender != null ? sender : "Unknown", count, pct));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public synchronized List<SmsModel> getRecentSms(int limit) {
        List<SmsModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_SMS, null, null, null, null, null,
                    COLUMN_TIMESTAMP + " DESC, " + COLUMN_ID + " DESC", String.valueOf(limit));
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(cursorToModel(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public synchronized List<SmsModel> getAllSms(String query) {
        List<SmsModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            if (query != null && !query.trim().isEmpty()) {
                String wild = "%" + query.trim() + "%";
                String selection = COLUMN_SENDER + " LIKE ? OR " + COLUMN_BODY + " LIKE ?";
                String[] selectionArgs = new String[]{wild, wild};
                cursor = db.query(TABLE_SMS, null, selection, selectionArgs, null, null,
                        COLUMN_TIMESTAMP + " DESC, " + COLUMN_ID + " DESC");
            } else {
                cursor = db.query(TABLE_SMS, null, null, null, null, null,
                        COLUMN_TIMESTAMP + " DESC, " + COLUMN_ID + " DESC");
            }

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(cursorToModel(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public synchronized SmsModel getSmsById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_SMS, null, COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToModel(cursor);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public synchronized void markAsRead(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_READ_STATUS, 1);
        db.update(TABLE_SMS, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }

    public synchronized void deleteSms(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SMS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }

    public synchronized void clearAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SMS, null, null);
    }

    public synchronized boolean isSenderMuted(String sender) {
        if (sender == null || sender.trim().isEmpty()) {
            return false;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_MUTED, null, COLUMN_MUTED_SENDER + "=?",
                    new String[]{sender.trim()}, null, null, null);
            return (cursor != null && cursor.moveToFirst());
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public synchronized void muteSender(String sender) {
        if (sender == null || sender.trim().isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_MUTED_SENDER, sender.trim());
        cv.put(COLUMN_MUTED_CREATED_AT, System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_MUTED, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public synchronized void unmuteSender(String sender) {
        if (sender == null || sender.trim().isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MUTED, COLUMN_MUTED_SENDER + "=?", new String[]{sender.trim()});
    }

    public synchronized List<String> getAllMutedSenders() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_MUTED, new String[]{COLUMN_MUTED_SENDER},
                    null, null, null, null, COLUMN_MUTED_CREATED_AT + " DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public synchronized long queueMessage(long smsId, String sender, String body, long timestamp, String payload, String errorReason) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_Q_SMS_ID, smsId);
        cv.put(COLUMN_Q_SENDER, sender);
        cv.put(COLUMN_Q_BODY, body);
        cv.put(COLUMN_Q_TIMESTAMP, timestamp);
        cv.put(COLUMN_Q_QUEUED_AT, System.currentTimeMillis());
        cv.put(COLUMN_Q_RETRY_COUNT, 0);
        cv.put(COLUMN_Q_LAST_RETRY, 0);
        cv.put(COLUMN_Q_ERROR, errorReason);
        cv.put(COLUMN_Q_PAYLOAD, payload);
        cv.put(COLUMN_Q_STATUS, "pending");
        return db.insert(TABLE_QUEUE, null, cv);
    }

    public synchronized List<QueuedMessage> getQueuedMessages() {
        List<QueuedMessage> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_QUEUE, null, null, null, null, null, COLUMN_Q_QUEUED_AT + " DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    QueuedMessage qm = new QueuedMessage();
                    qm.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_Q_ID));
                    qm.smsId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_Q_SMS_ID));
                    qm.sender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_Q_SENDER));
                    qm.body = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_Q_BODY));
                    qm.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_Q_TIMESTAMP));
                    qm.queuedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_Q_QUEUED_AT));
                    qm.retryCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_Q_RETRY_COUNT));
                    qm.lastRetryAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_Q_LAST_RETRY));
                    qm.errorReason = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_Q_ERROR));
                    qm.payload = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_Q_PAYLOAD));
                    qm.status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_Q_STATUS));
                    list.add(qm);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public synchronized int getQueuedCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUEUE, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
    }

    public synchronized void updateQueueRetry(long id, String error) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_QUEUE + " SET "
                + COLUMN_Q_RETRY_COUNT + " = " + COLUMN_Q_RETRY_COUNT + " + 1, "
                + COLUMN_Q_LAST_RETRY + " = ?, "
                + COLUMN_Q_ERROR + " = ? WHERE " + COLUMN_Q_ID + " = ?",
                new Object[]{System.currentTimeMillis(), error, id});
    }

    public synchronized void deleteQueuedMessage(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_QUEUE, COLUMN_Q_ID + "=?", new String[]{String.valueOf(id)});
    }

    public synchronized void clearQueue() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_QUEUE, null, null);
    }

    public synchronized PayloadConfig getPayloadConfig() {
        PayloadConfig cfg = new PayloadConfig();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_PAYLOAD_CONFIG, null, null, null, null, null, null, "1");
            if (cursor != null && cursor.moveToFirst()) {
                cfg.httpMethod = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CFG_METHOD));
                cfg.contentType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CFG_CONTENT_TYPE));
                cfg.includeSender = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CFG_INC_SENDER)) == 1;
                cfg.keySender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CFG_KEY_SENDER));
                cfg.includeBody = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CFG_INC_BODY)) == 1;
                cfg.keyBody = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CFG_KEY_BODY));
                cfg.includeTimestamp = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CFG_INC_TIMESTAMP)) == 1;
                cfg.keyTimestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CFG_KEY_TIMESTAMP));
                cfg.includeSimSlot = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CFG_INC_SIM)) == 1;
                cfg.keySimSlot = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CFG_KEY_SIM));
                cfg.includeDeviceModel = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CFG_INC_MODEL)) == 1;
                cfg.keyDeviceModel = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CFG_KEY_MODEL));
                cfg.includeAndroidVersion = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CFG_INC_ANDROID)) == 1;
                cfg.keyAndroidVersion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CFG_KEY_ANDROID));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return cfg;
    }

    public synchronized void savePayloadConfig(PayloadConfig cfg) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_CFG_METHOD, cfg.httpMethod);
        cv.put(COLUMN_CFG_CONTENT_TYPE, cfg.contentType);
        cv.put(COLUMN_CFG_INC_SENDER, cfg.includeSender ? 1 : 0);
        cv.put(COLUMN_CFG_KEY_SENDER, cfg.keySender);
        cv.put(COLUMN_CFG_INC_BODY, cfg.includeBody ? 1 : 0);
        cv.put(COLUMN_CFG_KEY_BODY, cfg.keyBody);
        cv.put(COLUMN_CFG_INC_TIMESTAMP, cfg.includeTimestamp ? 1 : 0);
        cv.put(COLUMN_CFG_KEY_TIMESTAMP, cfg.keyTimestamp);
        cv.put(COLUMN_CFG_INC_SIM, cfg.includeSimSlot ? 1 : 0);
        cv.put(COLUMN_CFG_KEY_SIM, cfg.keySimSlot);
        cv.put(COLUMN_CFG_INC_MODEL, cfg.includeDeviceModel ? 1 : 0);
        cv.put(COLUMN_CFG_KEY_MODEL, cfg.keyDeviceModel);
        cv.put(COLUMN_CFG_INC_ANDROID, cfg.includeAndroidVersion ? 1 : 0);
        cv.put(COLUMN_CFG_KEY_ANDROID, cfg.keyAndroidVersion);

        int rows = db.update(TABLE_PAYLOAD_CONFIG, cv, null, null);
        if (rows == 0) {
            db.insert(TABLE_PAYLOAD_CONFIG, null, cv);
        }
    }

    public synchronized List<KeyValueItem> getCustomHeaders() {
        List<KeyValueItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_CUSTOM_HEADERS, null, null, null, null, null, COLUMN_HDR_ID + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_HDR_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HDR_NAME));
                    String val = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HDR_VALUE));
                    list.add(new KeyValueItem(id, name, val));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public synchronized long addCustomHeader(String name, String value) {
        if (name == null || name.trim().isEmpty()) return -1;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_HDR_NAME, name.trim());
        cv.put(COLUMN_HDR_VALUE, value != null ? value.trim() : "");
        return db.insert(TABLE_CUSTOM_HEADERS, null, cv);
    }

    public synchronized void deleteCustomHeader(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CUSTOM_HEADERS, COLUMN_HDR_ID + "=?", new String[]{String.valueOf(id)});
    }

    public synchronized List<KeyValueItem> getCustomFields() {
        List<KeyValueItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_CUSTOM_FIELDS, null, null, null, null, null, COLUMN_FLD_ID + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FLD_ID));
                    String key = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FLD_KEY));
                    String val = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FLD_VALUE));
                    list.add(new KeyValueItem(id, key, val));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public synchronized long addCustomField(String key, String value) {
        if (key == null || key.trim().isEmpty()) return -1;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_FLD_KEY, key.trim());
        cv.put(COLUMN_FLD_VALUE, value != null ? value.trim() : "");
        return db.insert(TABLE_CUSTOM_FIELDS, null, cv);
    }

    public synchronized void deleteCustomField(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CUSTOM_FIELDS, COLUMN_FLD_ID + "=?", new String[]{String.valueOf(id)});
    }

    private SmsModel cursorToModel(Cursor cursor) {
        SmsModel sms = new SmsModel();
        sms.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        sms.setSmsId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SMS_ID)));
        sms.setThreadId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_THREAD_ID)));
        sms.setSender(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER)));
        sms.setBody(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BODY)));
        sms.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
        sms.setSimSlot(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SIM_SLOT)));
        sms.setSubId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SUB_ID)));
        sms.setReadStatus(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_READ_STATUS)));
        sms.setSmsType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SMS_TYPE)));
        sms.setServiceCenter(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SERVICE_CENTER)));
        sms.setProtocolId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROTOCOL_ID)));
        sms.setStatusOnIcc(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STATUS_ON_ICC)));
        sms.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
        int whStatusIdx = cursor.getColumnIndex(COLUMN_WEBHOOK_STATUS);
        if (whStatusIdx >= 0) {
            sms.setWebhookStatus(cursor.getString(whStatusIdx));
        }
        int whTimeIdx = cursor.getColumnIndex(COLUMN_WEBHOOK_TIME);
        if (whTimeIdx >= 0) {
            sms.setWebhookTime(cursor.getLong(whTimeIdx));
        }
        return sms;
    }
}
