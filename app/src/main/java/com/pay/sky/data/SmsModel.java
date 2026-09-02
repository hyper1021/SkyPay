package com.pay.sky.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    public SmsModel() {
        this.simSlot = -1;
        this.subId = -1;
        this.readStatus = 0;
        this.smsType = "INCOMING";
        this.createdAt = System.currentTimeMillis();
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

    public String getFormattedDate() {
        long timeToFormat = timestamp > 0 ? timestamp : createdAt;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy, h:mm:ss a", Locale.ENGLISH);
        return sdf.format(new Date(timeToFormat));
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
}
