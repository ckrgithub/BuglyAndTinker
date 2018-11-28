package com.ckr.bugly.listener;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.ckr.bugly.lifecycle.AppTracker;
import com.ckr.bugly.util.DownloadManager;
import com.tencent.bugly.beta.UpgradeInfo;
import com.tencent.bugly.beta.upgrade.UpgradeListener;

import static com.ckr.bugly.util.UpgradeLog.Logd;
import static com.ckr.bugly.util.UpgradeLog.Loge;

/**
 * Created by ckr on 2018/11/10.
 */

public class MyUpgradeListener implements UpgradeListener, Runnable {
	private static final String TAG = "MyUpgradeListener";
	private final AppTracker appTracker;
	private final Context context;

	public MyUpgradeListener(@NonNull AppTracker appTracker, Context context) {
		this.appTracker = appTracker;
		this.context = context;
	}

	@Override
	public void onUpgrade(int i, UpgradeInfo upgradeInfo, boolean b, boolean b1) {
		Logd(TAG, "onUpgrade: i" + i + ",b:" + b + ",b1:" + b1 + ",upgradeInfo:" + upgradeInfo);
		if (upgradeInfo != null) {
			com.ckr.bugly.UpgradeInfo info = new com.ckr.bugly.UpgradeInfo();
			info.title = upgradeInfo.title;
			info.newFeature = upgradeInfo.newFeature;
			info.upgradeType = upgradeInfo.upgradeType;
			info.versionCode = upgradeInfo.versionCode;
			info.versionName = upgradeInfo.versionName;
			info.apkUrl = upgradeInfo.apkUrl;
			info.fileSize = upgradeInfo.fileSize;
			DownloadManager.with(context).setUpgradeInfo(info);
			new Handler().postDelayed(this, 500);
		} else {
			Loge(TAG, "onUpgrade: 不需要更新，没有更新策略");
		}
	}

	@Override
	public void run() {
		Loge(TAG, "run: 需要更新，存在更新策略");
		if (appTracker != null) {
			appTracker.showDialog(true);
		}
	}
}
