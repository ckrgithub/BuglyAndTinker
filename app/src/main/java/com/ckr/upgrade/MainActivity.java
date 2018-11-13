package com.ckr.upgrade;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.view.View;
import android.widget.TextView;

import com.ckr.upgrade.util.DownloadManager;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;
import com.tencent.bugly.beta.tinker.TinkerManager;

import static com.ckr.upgrade.util.UpgradeLog.Logd;

public class MainActivity extends BaseActivity implements View.OnClickListener {
	private static final String TAG = "MainActivity";

	private TextView versionView;
	private TextView tinkerIdView;
	private View checkUpgrade;
	private DownloadReceiver downloadReceiver;

	public static void start(Context context) {
		Intent starter = new Intent(context, MainActivity.class);
		context.startActivity(starter);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		versionView = findViewById(R.id.version);
		tinkerIdView = findViewById(R.id.tinkerId);
		checkUpgrade = findViewById(R.id.checkUpgrade);
		checkUpgrade.setOnClickListener(this);
		setOnClickListener(R.id.btnService);
		setOnClickListener(R.id.btnReceiver);
		setOnClickListener(R.id.btnNotification);
		setOnClickListener(R.id.btnDownload);
		setOnClickListener(R.id.btnPause);

		UpgradeInfo upgradeInfo = Beta.getUpgradeInfo();
		String versionName = null;
		if (upgradeInfo != null) {
			versionName = upgradeInfo.versionName;
			versionView.append(versionName);
		}
		String tinkerId = TinkerManager.getTinkerId();
		Logd(TAG, "onCreate: versionName:" + versionName + ",tinkerId:" + tinkerId);
		tinkerIdView.append(tinkerId);
//        register();
	}

	private void setOnClickListener(@IdRes int viewId) {
		findViewById(viewId).setOnClickListener(this);
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (downloadReceiver != null) {
			unregisterReceiver(downloadReceiver);
			downloadReceiver = null;
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.checkUpgrade:
				Logd(TAG, "onClick: 检查更新");
				Beta.checkUpgrade(false, false);
				break;
			case R.id.btnService:
				startService(new Intent(this, DownLoadService.class));
				break;
			case R.id.btnReceiver:
				register();
				break;
			case R.id.btnNotification:
				new Thread(new Runnable() {
					@Override
					public void run() {
						DownloadManager with = DownloadManager.with(MainActivity.this.getApplicationContext());
						with.sendNotification();
					}
				}).start();
				break;
			case R.id.btnDownload:
				DownloadManager with = DownloadManager.with(this.getApplicationContext());
				if (with.getDownloadStatus() == DownloadManager.PAUSED) {
					with.resumeDownload();
				} else {
					with.startDownload();
				}
				break;
			case R.id.btnPause:
				DownloadManager.with(this.getApplicationContext()).pauseDownload();
				break;
		}
	}

	private void register() {
		downloadReceiver = new DownloadReceiver();
		IntentFilter filter = new IntentFilter(DownloadReceiver.APK_DOWNLOAD_RECEIVER);
		registerReceiver(downloadReceiver, filter);
	}
}
