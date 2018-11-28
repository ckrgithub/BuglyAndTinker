package com.ckr.bugly;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.ckr.bugly.util.DownloadManager;

import static com.ckr.bugly.util.UpgradeLog.Logd;

/**
 * Created on 2018/11/11
 *
 * @author ckr
 */
public class DownLoadService extends Service {
	private static final String TAG = "DownLoadService";
	private DownLoadBinder binder;
	private DownloadManager downloadManager;

	@Override
	public void onCreate() {
		super.onCreate();
		Logd(TAG, "onCreate: " + this);
		downloadManager = DownloadManager.with(this.getApplicationContext());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Logd(TAG, "onStartCommand: flags:" + flags + ",startId:" + startId + ",this:" + this);
		if (downloadManager.getUpgradeInfo() != null) {
			downloadManager.startDownload();
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		Logd(TAG, "onBind");
		return binder = new DownLoadBinder();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Logd(TAG, "onUnbind: ");
		return super.onUnbind(intent);
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		Logd(TAG, "onRebind: ");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Logd(TAG, "onDestroy: ");
	}

	public class DownLoadBinder extends Binder {
	}

}
