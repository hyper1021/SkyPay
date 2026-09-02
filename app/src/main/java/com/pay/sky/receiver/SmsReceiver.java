package com.pay.sky.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import com.pay.sky.data.SmsDatabaseHelper;
import com.pay.sky.data.SmsModel;
import com.pay.sky.service.SmsReaderService;
import com.pay.sky.util.PaymentGatewayDispatcher;
import com.pay.sky.util.PreferencesManager;
import com.pay.sky.util.SimHelper;

public class SmsReceiver extends BroadcastReceiver {

    public static final String ACTION_SMS_RECEIVED_EVENT = "com.pay.sky.broadcast.SMS_RECEIVED_EVENT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }

        SmsMessage[] messages;
        try {
            messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        } catch (Exception e) {
            return;
        }

        if (messages == null || messages.length == 0) {
            return;
        }

        StringBuilder fullBody = new StringBuilder();
        String sender = null;
        long timestamp = 0;
        String serviceCenter = null;
        int protocolId = -1;
        int statusOnIcc = -1;
        int subId = extractSubscriptionId(intent);

        for (SmsMessage msg : messages) {
            if (msg == null) {
                continue;
            }
            if (sender == null) {
                sender = msg.getDisplayOriginatingAddress();
                if (sender == null) {
                    sender = msg.getOriginatingAddress();
                }
            }
            if (timestamp == 0) {
                timestamp = msg.getTimestampMillis();
            }
            if (serviceCenter == null) {
                serviceCenter = msg.getServiceCenterAddress();
            }
            protocolId = msg.getProtocolIdentifier();
            statusOnIcc = msg.getStatusOnIcc();

            String bodyPart = msg.getDisplayMessageBody();
            if (bodyPart == null) {
                bodyPart = msg.getMessageBody();
            }
            if (bodyPart != null) {
                fullBody.append(bodyPart);
            }
        }

        if (sender == null) {
            sender = "Unknown";
        }
        if (timestamp <= 0) {
            timestamp = System.currentTimeMillis();
        }

        int simSlot = SimHelper.getSimSlotFromSubId(context, subId);
        if (simSlot < 0 && intent.hasExtra("slot")) {
            simSlot = intent.getIntExtra("slot", -1);
        }

        long threadId = -1;
        try {
            threadId = Telephony.Threads.getOrCreateThreadId(context, sender);
        } catch (Exception ignored) {
        }

        SmsModel sms = new SmsModel();
        sms.setSender(sender);
        sms.setBody(fullBody.toString());
        sms.setTimestamp(timestamp);
        sms.setSubId(subId);
        sms.setSimSlot(simSlot);
        sms.setThreadId(threadId);
        sms.setServiceCenter(serviceCenter != null ? serviceCenter : "N/A");
        sms.setProtocolId(protocolId);
        sms.setStatusOnIcc(statusOnIcc);
        sms.setReadStatus(0);
        sms.setSmsType("INCOMING");
        sms.setCreatedAt(System.currentTimeMillis());

        SmsDatabaseHelper db = SmsDatabaseHelper.getInstance();
        if (db != null) {
            long insertedId = db.insertSms(sms);
            sms.setSmsId(String.valueOf(insertedId));
        }

        PreferencesManager prefs = PreferencesManager.getInstance();
        if (prefs != null) {
            prefs.incrementSessionSmsCount();
        }

        SmsReaderService.notifySmsReceived(context);
        PaymentGatewayDispatcher.dispatch(context, sms);

        Intent broadcast = new Intent(ACTION_SMS_RECEIVED_EVENT);
        broadcast.setPackage(context.getPackageName());
        context.sendBroadcast(broadcast);
    }

    private int extractSubscriptionId(Intent intent) {
        if (intent == null) {
            return -1;
        }
        if (intent.hasExtra("subscription")) {
            return intent.getIntExtra("subscription", -1);
        }
        if (intent.hasExtra("sub_id")) {
            return intent.getIntExtra("sub_id", -1);
        }
        if (intent.hasExtra("android.telephony.extra.SUBSCRIPTION_INDEX")) {
            return intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1);
        }
        if (intent.hasExtra("simId")) {
            return intent.getIntExtra("simId", -1);
        }
        if (intent.hasExtra("phone")) {
            return intent.getIntExtra("phone", -1);
        }
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                if (key.toLowerCase().contains("sub")) {
                    Object val = bundle.get(key);
                    if (val instanceof Integer) {
                        return (Integer) val;
                    }
                }
            }
        }
        return -1;
    }
}
