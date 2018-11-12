package com.ckr.upgrade;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static com.ckr.upgrade.DownLoadService.APK_URL;
import static com.ckr.upgrade.DownLoadService.COMPLETE;
import static com.ckr.upgrade.DownLoadService.DOWNLOADING;
import static com.ckr.upgrade.DownLoadService.DOWNLOAD_PROGRESS;
import static com.ckr.upgrade.DownLoadService.INIT;
import static com.ckr.upgrade.util.UpgradeLog.Logd;

/**
 * Created by ckr on 2018/11/12.
 */

public class DownloadReceiver extends BroadcastReceiver {
    private static final String TAG = "DownloadReceiver";
    public static final String DOWNLOAD_RECEIVER = "apk_download_receiver";
    private MainActivity activity;
    private DownLoadService.DownloadListener mListener;

    public DownloadReceiver() {
    }

    public DownloadReceiver(MainActivity activity) {
        this.activity = activity;
    }
    public DownloadReceiver(DownLoadService.DownloadListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Logd(TAG, "onReceive: " + this + ",activity:" + activity);
        if (intent != null) {
            int status = intent.getIntExtra(DOWNLOAD_PROGRESS, INIT);
            String url = intent.getStringExtra(APK_URL);
            Logd(TAG, "onReceive: status:" + status + ",url:" + url);
            switch (status) {
                case DOWNLOADING:
                    break;
                case COMPLETE:
                    break;
            }
        }
    }
}
