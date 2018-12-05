package com.ckr.upgrade.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.ckr.upgrade.util.ApkUtil;

import java.io.File;

import static com.ckr.upgrade.DownloadManager.APK_URL;
import static com.ckr.upgrade.DownloadManager.FILE_NAME;
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
        if (action.equals(Intent.ACTION_PACKAGE_ADDED) || action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
            Logd(TAG, "onReceive: 安装成功");
            SharedPreferences preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
            String saveApkUrl = preferences.getString(APK_URL, null);
            if (!TextUtils.isEmpty(saveApkUrl)) {
                final String path = ApkUtil.getApkPath(saveApkUrl, context);
                final File file = new File(path);
                if (file.exists()) {
                    boolean delete = file.delete();
                    Logd(TAG, "onReceive: delete:" + delete);
                }
            }
        }
    }
}
