package com.pay.sky.util;

import com.pay.sky.data.SmsModel;

public class UnifiedPlaceholderResolver {

    public static String resolve(String template, SmsModel sms, String contentType, String authToken, String deviceId) {
        if (template == null) {
            return "";
        }
        if (!template.contains("${")) {
            return template;
        }

        String sender = "";
        String body = "";
        String receiveAt = "";
        String simSlot = "0";

        if (sms != null) {
            sender = sms.getSender() != null ? sms.getSender() : "";
            body = sms.getBody() != null ? sms.getBody() : "";
            receiveAt = sms.getFormattedDate() != null ? sms.getFormattedDate() : String.valueOf(sms.getTimestamp());
            simSlot = String.valueOf(sms.getSimSlot());
        }

        String devId = deviceId != null ? deviceId : "";
        String auth = authToken != null ? authToken : "";
        
        String typeHeader = "application/json";
        if (contentType != null) {
            if (contentType.toLowerCase().contains("form") || contentType.toLowerCase().contains("urlencoded")) {
                typeHeader = "application/x-www-form-urlencoded";
            } else {
                typeHeader = "application/json";
            }
        }

        String result = template;
        result = replaceVariable(result, "${sender}", sender);
        result = replaceVariable(result, "${body}", body);
        result = replaceVariable(result, "${receive_at}", receiveAt);
        result = replaceVariable(result, "${sim_slot}", simSlot);
        result = replaceVariable(result, "${device_id}", devId);
        result = replaceVariable(result, "${auth_token}", auth);
        result = replaceVariable(result, "${type}", typeHeader);

        return result;
    }

    private static String replaceVariable(String input, String placeholder, String value) {
        if (input == null || placeholder == null || !input.contains(placeholder)) {
            return input;
        }
        return input.replace(placeholder, value != null ? value : "");
    }
}
