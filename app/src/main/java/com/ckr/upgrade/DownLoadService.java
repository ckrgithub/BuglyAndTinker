package com.ckr.upgrade;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.ckr.upgrade.util.UpgradeLog.Logd;

/**
 * Created on 2018/11/11
 *
 * @author ckr
 */
public class DownLoadService extends Service {
	private static final String TAG = "DownLoadService";

	private final static int INIT = 0;
	private final static int COMPLETE = 1;
	private final static int DOWNLOADING = 2;
	private final static int PAUSED = 3;
	private final static int DELETED =4;
	private final static int FAILED =5;

	private final static int NOTIFY_ID = 1111;
	private final static int VERSION = 4;
	private DownLoadBinder binder;
	private NotificationManager notificationManager;
	private DownloadListener downloadListener;

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		Logd(TAG, "onBind");
		return binder = new DownLoadBinder();
	}

	public class DownLoadBinder extends Binder {
		public void update() {
			Log.i("download", "更新");
			setUpNotification();
			download();
		}

		public void downloadAppVersion(DownloadListener downloadListener) {
			DownLoadService.this.downloadListener = downloadListener;
			new Thread() {
				@Override
				public void run() {
					Log.i("download", "run");
					int line = -1;
					byte[] buffer = new byte[1024];
					try {
						UpgradeInfo upgradeInfo = Beta.getUpgradeInfo();
						if (upgradeInfo == null) {
							return;
						}
						String apkUrl = upgradeInfo.apkUrl;
						String path = getApkPath(apkUrl);
						HttpURLConnection connection = (HttpURLConnection) new URL(apkUrl).openConnection();
						connection.setRequestMethod("GET");
						connection.setConnectTimeout(5000);
						connection.setDoInput(true);
						connection.setDoOutput(false);
						connection.connect();
						InputStream inputStream = null;
						FileOutputStream outputStream = null;
						if (connection.getResponseCode() == 200) {
							inputStream = connection.getInputStream();
							outputStream = new FileOutputStream(path);
							while ((line = inputStream.read(buffer)) != -1) {
								outputStream.write(buffer, 0, line);
							}
							outputStream.flush();
							mHandler.sendEmptyMessage(VERSION);
						}
						if (outputStream != null)
							outputStream.close();
						if (inputStream != null)
							inputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}.start();

		}

		public String getAppVersion(Context context) {
			return null;
		}

		private void download() {
			new Thread() {
				@Override
				public void run() {
					Log.i("download", "run");
					try {
						UpgradeInfo upgradeInfo = Beta.getUpgradeInfo();
						if (upgradeInfo == null) {
							return;
						}
						String apkUrl = upgradeInfo.apkUrl;
						mHandler.sendEmptyMessage(INIT);
						cancel = false;
						HttpURLConnection connection = (HttpURLConnection) new URL(apkUrl).openConnection();
						connection.setRequestMethod("GET");
						connection.setConnectTimeout(5000);
						connection.setDoInput(true);
						connection.setDoOutput(false);
						connection.connect();
						InputStream inputStream = null;
						FileOutputStream outputStream = null;
						if (connection.getResponseCode() == 200) {
							int length = connection.getContentLength();
							inputStream = connection.getInputStream();
							File file = new File(getApkPath(apkUrl));
							if (!file.exists()) {
								file.mkdirs();
							}
//                            File apkFile = new File();
							/*if (apkFile.exists()) {
                                apkFile.delete();
                            }*/
							outputStream = new FileOutputStream(getApkPath(apkUrl));
							int count = 0, line = -1, progress = 0, lastProgress = 0;
							byte[] buffer = new byte[1024];
							while (!cancel) {
								line = inputStream.read(buffer);
								count += line;
								progress = (int) (((float) count / length) * 100);
								if (progress >= lastProgress + 1) {
									// 更新进度
									Log.i("download", "mHandler" + progress);
									Message msg = mHandler.obtainMessage();
									msg.what = DOWNLOADING;
									msg.arg1 = progress;
									mHandler.sendMessage(msg);
									lastProgress = progress;
								}
								if (line <= 0) {
									// 下载完成通知安装
									mHandler.sendEmptyMessage(COMPLETE);
									// 下载完了，cancelled也要设置
									cancel = true;
									break;
								}
								outputStream.write(buffer, 0, line);
							}

						}
						if (outputStream != null)
							outputStream.close();
						if (inputStream != null)
							inputStream.close();
					} catch (IOException e) {
						mHandler.sendEmptyMessage(PAUSED);
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	@NonNull
	private String getApkPath(String apkUrl) {
		String fileName = apkUrl.substring(apkUrl.lastIndexOf("/"), apkUrl.length());
		File externalCacheDir = getExternalCacheDir();
		return externalCacheDir.getAbsoluteFile() + File.separator + fileName;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Log.e("DownLoadService", "DownLoadService的handler运行中");
			switch (msg.what) {
				case INIT:
//					if (downloadListener != null) {
//						downloadListener.startDownLoad();
//					}
					break;
				case COMPLETE:       // 下载完毕
					Log.i("download", "COMPLETE：");
					// 取消通知
					notificationManager.cancel(NOTIFY_ID);
//					if (downloadListener != null) {
//						downloadListener.onFinish();
//					}
					installApk();
					break;
				case PAUSED:
//					if (downloadListener != null) {
//						downloadListener.downLoadCancel();
//					}
					break;
				case DOWNLOADING:
					int rate = msg.arg1;
					if (rate < 100) {
						builder.setProgress(100, rate, false);
						builder.setContentInfo(rate + "%");
						notification = builder.build();
					} else {
						notification = builder.build();
						notification.flags = Notification.FLAG_AUTO_CANCEL;
						stopSelf();// 停掉服务自身
					}
					notificationManager.notify(NOTIFY_ID, notification);
					if (downloadListener != null) {
						downloadListener.onReceive(rate);
					}
					break;
				case VERSION:
					String appVersion = binder.getAppVersion(DownLoadService.this);
					Log.i("download", "app版本：" + appVersion);
					if (TextUtils.isEmpty(appVersion))
						return;
					Log.d("download", "handleMessage: downloadListener:" + downloadListener);
//					if (downloadListener != null) {
//						downloadListener.appVersion(appVersion);
//					}
					break;
			}
		}
	};

	Notification notification;
	boolean cancel = false;
	Notification.Builder builder;

	private void setUpNotification() {
		builder = new Notification.Builder(this);

		Intent intent = new Intent(this, MainActivity.class);
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(contentIntent);

		builder.setSmallIcon(R.mipmap.ic_launcher).setTicker("app更新中")
				.setWhen(System.currentTimeMillis()).setContentTitle("app")
				.setContentText("下载中").setContentInfo("0%");
		builder.setOngoing(true);
		notification = builder.build();
		notification.flags = Notification.FLAG_ONGOING_EVENT;

		notificationManager.notify(NOTIFY_ID, notification);
	}

	/**
	 * 安装apk
	 */
	private void installApk() {
		UpgradeInfo upgradeInfo = Beta.getUpgradeInfo();
		if (upgradeInfo == null) {
			return;
		}
		String path = getApkPath(upgradeInfo.apkUrl);
		File apkfile = new File(path);
		if (!apkfile.exists()) {
			return;
		}
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");
		startActivity(i);

	}

	public interface DownloadListener {

		void onReceive(int rate);

		void onCompleted();

		void onFailed();

	}
}
