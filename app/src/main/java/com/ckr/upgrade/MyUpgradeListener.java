package com.ckr.upgrade;

import android.os.Handler;
import android.util.Log;

import com.ckr.upgrade.lifecycle.AppTracker;
import com.tencent.bugly.beta.UpgradeInfo;
import com.tencent.bugly.beta.upgrade.UpgradeListener;


import static com.ckr.upgrade.UpgradeLog.Logd;
import static com.ckr.upgrade.UpgradeLog.Loge;

/**
 * Created by ckr on 2018/11/10.
 */

public class MyUpgradeListener implements UpgradeListener, Runnable {
    private static final String TAG = "MyUpgradeListener";
    private final AppTracker appTracker;

    public MyUpgradeListener(AppTracker appTracker) {
        this.appTracker = appTracker;
    }

    @Override
    public void onUpgrade(int i, UpgradeInfo upgradeInfo, boolean b, boolean b1) {
        Logd(TAG, "onUpgrade: i" + i + ",b:" + b + ",b1:" + b1 + ",upgradeInfo:" + upgradeInfo);
        if (upgradeInfo != null) {
            new Handler().postDelayed(this, 500);
        } else {
            Loge(TAG, "onUpgrade: 不需要更新，没有更新策略");
        }
    }

    @Override
    public void run() {
        Loge(TAG, "run: 需要更新，存在更新策略");
        appTracker.showDialog(true);
    }
}
