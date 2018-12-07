package com.ckr.upgrade.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ckr.upgrade.util.ApkUtil;
import com.ckr.upgrade.util.ClearApkService;

import static com.ckr.upgrade.util.UpgradeLog.Logd;

/**
 * Created by ckr on 2018/12/5.
 */

public class ApkInstallReceiver extends BroadcastReceiver {
    private static final String TAG = "ApkInstallReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Logd(TAG, "onReceive: " + intent.getAction());
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
            Logd(TAG, "onReceive: 安装成功");
            Intent clearService = new Intent(context, ClearApkService.class);
            context.startService(clearService);
        }
    }
}
