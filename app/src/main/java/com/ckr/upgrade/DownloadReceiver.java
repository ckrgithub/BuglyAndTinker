package com.ckr.upgrade;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ckr.upgrade.util.ApkUtil;
import com.ckr.upgrade.util.DownloadManager;

import static com.ckr.upgrade.util.DownloadManager.APK_URL;
import static com.ckr.upgrade.util.DownloadManager.COMPLETE;
import static com.ckr.upgrade.util.DownloadManager.DOWNLOADING;
import static com.ckr.upgrade.util.DownloadManager.DOWNLOAD_PROGRESS;
import static com.ckr.upgrade.util.DownloadManager.FAILED;
import static com.ckr.upgrade.util.DownloadManager.INIT;
import static com.ckr.upgrade.util.DownloadManager.PAUSED;
import static com.ckr.upgrade.util.UpgradeLog.Logd;

/**
 * Created by ckr on 2018/11/12.
 */

public class DownloadReceiver extends BroadcastReceiver {
	private static final String TAG = "DownloadReceiver";
	public static final String APK_DOWNLOAD_RECEIVER = "apk_download_receiver";

	public DownloadReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Logd(TAG, "onReceive: " + this);
		if (intent != null) {
			int status = intent.getIntExtra(DOWNLOAD_PROGRESS, INIT);
			String url = intent.getStringExtra(APK_URL);
			Logd(TAG, "onReceive: status:" + status + ",url:" + url);
			switch (status) {
				case DOWNLOADING:
					DownloadManager.with(context.getApplicationContext()).pauseDownload();
					break;
				case COMPLETE:
					ApkUtil.installApk(url, context);
					break;
				case PAUSED:
					DownloadManager.with(context.getApplicationContext()).resumeDownload();
					break;
				case FAILED:
					DownloadManager.with(context.getApplicationContext()).startDownload();
					break;
			}
		}
	}
}
