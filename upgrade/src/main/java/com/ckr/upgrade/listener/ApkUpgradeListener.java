package com.ckr.upgrade.listener;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.ckr.upgrade.DownloadManager;
import com.ckr.upgrade.R;
import com.ckr.upgrade.UpgradeConfig;
import com.ckr.upgrade.UpgradeManager;
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

	private final Context context;

	public ApkUpgradeListener(@NonNull Context context) {
		this.context = context;
	}

	@Override
	public void onUpgrade(int i, UpgradeInfo upgradeInfo, boolean b, boolean b1) {
		Logd(TAG, "onUpgrade: i:" + i + ",b:" + b + ",b1:" + b1 + ",upgradeInfo:" + upgradeInfo);
		if (upgradeInfo != null) {
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
			if (UpgradeConfig.toastWhenNoUpgrade) {
				UpgradeConfig.toastWhenNoUpgrade = false;
				if (context != null) {
					Toast.makeText(context, context.getString(R.string.tips_no_upgrade), Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	@Override
	public void run() {
		UpgradeLog.Loge(TAG, "run: 需要更新，存在更新策略");
		AppTracker appTracker = UpgradeManager.getAppTracker();
		if (appTracker != null) {
			appTracker.showDialog(true);
		}
	}
}
