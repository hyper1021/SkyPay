package com.pay.sky.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import androidx.core.content.ContextCompat;
import java.util.List;

public class SimHelper {

    public static int getSimSlotFromSubId(Context context, int subId) {
        if (subId < 0) {
            return -1;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    SubscriptionManager manager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                    if (manager != null) {
                        SubscriptionInfo info = manager.getActiveSubscriptionInfo(subId);
                        if (info != null) {
                            return info.getSimSlotIndex();
                        }
                        List<SubscriptionInfo> list = manager.getActiveSubscriptionInfoList();
                        if (list != null) {
                            for (SubscriptionInfo item : list) {
                                if (item.getSubscriptionId() == subId) {
                                    return item.getSimSlotIndex();
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    public static int getActiveSimCount(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    SubscriptionManager manager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                    if (manager != null) {
                        return manager.getActiveSubscriptionInfoCount();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    public static String getSimSummary(Context context) {
        int count = getActiveSimCount(context);
        if (count > 1) {
            return "Dual SIM";
        } else if (count == 1) {
            return "1 SIM";
        }
        return "Auto";
    }
}
