package com.pay.sky.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class SmsDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "skypay_sms.db";
    private static final int DATABASE_VERSION = 1;

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

    private static SmsDatabaseHelper instance;

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
        String createTableQuery = "CREATE TABLE " + TABLE_SMS + " ("
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
                + COLUMN_CREATED_AT + " INTEGER);";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SMS);
        onCreate(db);
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

        long rowId = db.insert(TABLE_SMS, null, values);
        sms.setId(rowId);
        return rowId;
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
                cursor = db.query(TABLE_SMS, null, selection, selectionArgs, null, null, COLUMN_TIMESTAMP + " DESC, " + COLUMN_ID + " DESC");
            } else {
                cursor = db.query(TABLE_SMS, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC, " + COLUMN_ID + " DESC");
            }

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(cursorToModel(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
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
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public synchronized int getSmsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int count = 0;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SMS, null);
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
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
        return sms;
    }
}
