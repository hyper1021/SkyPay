package com.pay.sky.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SmsModel {

    private long id;
    private String smsId;
    private long threadId;
    private String sender;
    private String body;
    private long timestamp;
    private int simSlot;
    private int subId;
    private int readStatus;
    private String smsType;
    private String serviceCenter;
    private int protocolId;
    private int statusOnIcc;
    private long createdAt;
    private String webhookStatus;
    private long webhookTime;

    public SmsModel() {
        this.simSlot = -1;
        this.subId = -1;
        this.readStatus = 0;
        this.smsType = "INCOMING";
        this.createdAt = System.currentTimeMillis();
        this.webhookStatus = "pending";
        this.webhookTime = 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSmsId() {
        return smsId;
    }

    public void setSmsId(String smsId) {
        this.smsId = smsId;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getSimSlot() {
        return simSlot;
    }

    public void setSimSlot(int simSlot) {
        this.simSlot = simSlot;
    }

    public int getSubId() {
        return subId;
    }

    public void setSubId(int subId) {
        this.subId = subId;
    }

    public int getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(int readStatus) {
        this.readStatus = readStatus;
    }

    public String getSmsType() {
        return smsType;
    }

    public void setSmsType(String smsType) {
        this.smsType = smsType;
    }

    public String getServiceCenter() {
        return serviceCenter;
    }

    public void setServiceCenter(String serviceCenter) {
        this.serviceCenter = serviceCenter;
    }

    public int getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(int protocolId) {
        this.protocolId = protocolId;
    }

    public int getStatusOnIcc() {
        return statusOnIcc;
    }

    public void setStatusOnIcc(int statusOnIcc) {
        this.statusOnIcc = statusOnIcc;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getWebhookStatus() {
        return webhookStatus;
    }

    public void setWebhookStatus(String webhookStatus) {
        this.webhookStatus = webhookStatus;
    }

    public long getWebhookTime() {
        return webhookTime;
    }

    public void setWebhookTime(long webhookTime) {
        this.webhookTime = webhookTime;
    }

    public String getFormattedDate() {
        long timeToFormat = timestamp > 0 ? timestamp : createdAt;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy, h:mm:ss a", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
        return sdf.format(new Date(timeToFormat));
    }

    public String getRelativeTimeSpan() {
        long timeToFormat = timestamp > 0 ? timestamp : createdAt;
        long now = System.currentTimeMillis();
        long diff = now - timeToFormat;

        if (diff < 60_000) {
            return "Just now";
        } else if (diff < 3600_000) {
            long mins = Math.max(1, diff / 60_000);
            return mins + (mins == 1 ? " minute ago" : " minutes ago");
        } else if (diff < 86400_000) {
            long hours = Math.max(1, diff / 3600_000);
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (diff < 172800_000) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
            return "Yesterday, " + timeFormat.format(new Date(timeToFormat));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
            return sdf.format(new Date(timeToFormat));
        }
    }

    public String getSimSlotDisplay() {
        if (simSlot >= 0) {
            return "SIM " + (simSlot + 1);
        }
        if (subId >= 0) {
            return "Sub " + subId;
        }
        return "SIM";
    }

    public String getRecognizedSenderLabel() {
        if (sender == null || sender.trim().isEmpty()) {
            return "Unknown";
        }
        String s = sender.trim().toUpperCase(Locale.ENGLISH);
        if (s.contains("BKASH")) return "bKash";
        if (s.contains("NAGAD")) return "Nagad";
        if (s.contains("ROCKET")) return "Rocket";
        if (s.contains("UPAY")) return "Upay";
        if (s.contains("BANK")) return "Bank Alert";
        if (s.contains("PAY")) return "Payment";
        if (s.contains("OTP")) return "Verification OTP";
        return sender;
    }
}
