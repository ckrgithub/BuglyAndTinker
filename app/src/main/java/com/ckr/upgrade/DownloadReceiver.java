package com.ckr.upgrade;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static com.ckr.upgrade.DownLoadService.APK_URL;
import static com.ckr.upgrade.DownLoadService.DOWNLOAD_PROGRESS;
import static com.ckr.upgrade.DownLoadService.INIT;
import static com.ckr.upgrade.util.UpgradeLog.Logd;

/**
 * Created by ckr on 2018/11/12.
 */

public class DownloadReceiver extends BroadcastReceiver {
    private static final String TAG = "DownloadReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            int status = intent.getIntExtra(DOWNLOAD_PROGRESS, INIT);
            String url = intent.getStringExtra(APK_URL);
            Logd(TAG, "onReceive: status:" + status + ",url:" + url);
        }
    }
}
