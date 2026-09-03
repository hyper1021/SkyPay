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
import java.util.List;
import java.util.Locale;

public class SmsDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "skypay_sms.db";
    private static final int DATABASE_VERSION = 2;

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_SMS + " ADD COLUMN " + COLUMN_WEBHOOK_STATUS + " TEXT DEFAULT 'pending'");
            } catch (Exception ignored) {
            }
            try {
                db.execSQL("ALTER TABLE " + TABLE_SMS + " ADD COLUMN " + COLUMN_WEBHOOK_TIME + " INTEGER DEFAULT 0");
            } catch (Exception ignored) {
            }
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MUTED + " ("
                    + COLUMN_MUTED_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_MUTED_SENDER + " TEXT UNIQUE, "
                    + COLUMN_MUTED_CREATED_AT + " INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS + " ("
                    + COLUMN_SETTING_KEY + " TEXT PRIMARY KEY, "
                    + COLUMN_SETTING_VAL + " TEXT);");
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
        Calendar cal = Calendar.getInstance();
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
        SQLiteDatabase db = this.getReadableDatabase();

        Calendar cal = Calendar.getInstance();
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
