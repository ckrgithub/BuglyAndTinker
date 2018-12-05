package com.ckr.upgrade.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.tencent.bugly.beta.UpgradeInfo;

import static com.ckr.upgrade.util.UpgradeLog.Logd;

/**
 * Created by ckr on 2018/12/5.
 */

public class PopUtil {
    private static final String TAG="PopUtil";
    
    private static final String KEY_POP_TIMES = "popTimes";
    private static final String KEY_POP_INTERVAL = "popInterval";
    
    public static boolean interceptPop(@NonNull Context context, @NonNull UpgradeInfo upgradeInfo) {
        int popTimes = upgradeInfo.popTimes;
        long popInterval = upgradeInfo.popInterval;
        String apkUrl = upgradeInfo.apkUrl;
        if (context != null) {
            String fileName = ApkUtil.getApkName(apkUrl);
            if (!TextUtils.isEmpty(fileName)) {
                SharedPreferences preferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
                boolean isStartPopConfig = false;
                if (popTimes > 0 && popTimes != Integer.MAX_VALUE) {
                    int savePopTimes = preferences.getInt(KEY_POP_TIMES, 0);
                    Logd(TAG, "interceptPop: savePopTimes:" + savePopTimes + ",popTime:" + popTimes);
                    if (popTimes <= savePopTimes) {
                        return true;
                    } else {
                        isStartPopConfig = true;
                    }
                }
                if (popInterval > 0) {
                    long currentTimeMillis = System.currentTimeMillis();
                    long savePopInterval = preferences.getLong(KEY_POP_INTERVAL, currentTimeMillis);
                    Logd(TAG, "interceptPop: savePopInterval:" + savePopInterval + ",popInterval:" + popInterval + ",currentTimeMillis:" + currentTimeMillis);
                    if ((savePopInterval + popInterval) < currentTimeMillis) {
                        return true;
                    } else {
                        isStartPopConfig = true;
                    }
                }
                if (isStartPopConfig) {
                    SharedPreferences.Editor edit = preferences.edit();
                    if (popTimes > 0) {
                        int savePopTimes = preferences.getInt(KEY_POP_TIMES, 0);
                        edit.putInt(KEY_POP_TIMES, savePopTimes + 1);
                    }
                    if (popInterval > 0) {
                        edit.putLong(KEY_POP_INTERVAL, System.currentTimeMillis());
                    }
                    edit.apply();
                }
            }
        }
        return false;
    }

}
