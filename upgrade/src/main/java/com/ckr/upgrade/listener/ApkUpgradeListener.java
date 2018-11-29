package com.ckr.upgrade.listener;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.ckr.upgrade.DownloadManager;
import com.ckr.upgrade.util.ApkUtil;
import com.ckr.upgrade.util.AppTracker;
import com.ckr.upgrade.util.UpgradeLog;
import com.tencent.bugly.beta.UpgradeInfo;
import com.tencent.bugly.beta.upgrade.UpgradeListener;

import static com.ckr.upgrade.util.UpgradeLog.Logd;

/**
 * Created by ckr on 2018/11/10.
 */

public class ApkUpgradeListener implements UpgradeListener, Runnable {
    private static final String TAG = "ApkUpgradeListener";
    private static final String KEY_POP_TIMES = "popTimes";
    private static final String KEY_POP_INTERVAL = "popInterval";
    private final AppTracker appTracker;
    private final Context context;

    public ApkUpgradeListener(@NonNull AppTracker appTracker, @NonNull Context context) {
        this.appTracker = appTracker;
        this.context = context;
    }

    @Override
    public void onUpgrade(int i, UpgradeInfo upgradeInfo, boolean b, boolean b1) {
        Logd(TAG, "onUpgrade: i" + i + ",b:" + b + ",b1:" + b1 + ",upgradeInfo:" + upgradeInfo);
        if (upgradeInfo != null) {
            if (interceptPop(upgradeInfo)) return;
            com.ckr.upgrade.UpgradeInfo info = new com.ckr.upgrade.UpgradeInfo();
            info.title = upgradeInfo.title;
            info.newFeature = upgradeInfo.newFeature;
            info.upgradeType = upgradeInfo.upgradeType;
            info.versionCode = upgradeInfo.versionCode;
            info.versionName = upgradeInfo.versionName;
            info.apkUrl = upgradeInfo.apkUrl;
            info.fileSize = upgradeInfo.fileSize;
            info.popTimes = upgradeInfo.popTimes;
            info.popInterval = upgradeInfo.popInterval;
            DownloadManager.with(context).setUpgradeInfo(info);
            new Handler().postDelayed(this, 500);
        } else {
            UpgradeLog.Loge(TAG, "onUpgrade: 不需要更新，没有更新策略");
        }
    }

    private boolean interceptPop(UpgradeInfo upgradeInfo) {
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

    @Override
    public void run() {
        UpgradeLog.Loge(TAG, "run: 需要更新，存在更新策略");
        if (appTracker != null) {
            appTracker.showDialog(true);
        }
    }
}
