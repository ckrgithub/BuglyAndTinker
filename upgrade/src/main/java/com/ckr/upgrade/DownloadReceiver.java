package com.ckr.upgrade;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ckr.upgrade.util.ApkUtil;
import com.ckr.upgrade.util.UpgradeLog;

import static com.ckr.upgrade.DownloadManager.APK_URL;
import static com.ckr.upgrade.DownloadManager.COMPLETE;
import static com.ckr.upgrade.DownloadManager.DOWNLOADING;
import static com.ckr.upgrade.DownloadManager.DOWNLOAD_STATUS;
import static com.ckr.upgrade.DownloadManager.FAILED;
import static com.ckr.upgrade.DownloadManager.INIT;
import static com.ckr.upgrade.DownloadManager.PAUSED;

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
		UpgradeLog.Logd(TAG, "onReceive: " + this);
		if (intent != null) {
			int status = intent.getIntExtra(DOWNLOAD_STATUS, INIT);
			String url = intent.getStringExtra(APK_URL);
			UpgradeLog.Logd(TAG, "onReceive: status:" + status + ",url:" + url);
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
