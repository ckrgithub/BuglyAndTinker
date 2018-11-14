package com.ckr.upgrade.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.ckr.upgrade.R;
import com.ckr.upgrade.listener.DownloadListener;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.ckr.upgrade.DownloadReceiver.APK_DOWNLOAD_RECEIVER;
import static com.ckr.upgrade.util.UpgradeLog.Logd;
import static com.ckr.upgrade.util.UpgradeLog.Loge;

/**
 * Created by ckr on 2018/11/13.
 */

public class DownloadManager implements Runnable {
	private static final String TAG = "DownloadManager";
	public static final String DOWNLOAD_PROGRESS = "download_progress";
	public static final String APK_URL = "apk_url";
	private static final String CHANNEL_ID = "ckrgithub";
	private static final String CHANNEL_NAME = "upgrade";
	public final static int INIT = 0;
	public final static int COMPLETE = 1;
	public final static int DOWNLOADING = 2;
	public final static int PAUSED = 3;
	public final static int RESUMED = 4;
	public final static int DELETED = 5;
	public final static int FAILED = 6;
	private final static int NOTIFY_ID = 1129;

	private static DownloadManager INSTANCE;
	private final Context mContext;
	private LinkedList<DownloadListener> mListeners;
	private int mDownloadStatus = INIT;
	private MyHandler mHandler;
	private NotificationManager notificationManager;
	private NotificationCompat.Builder builder;
	private ExecutorService mExecutor;
	private Future<?> mFuture;


	private DownloadManager(Context context) {
		mContext = context;
	}

	public static DownloadManager with(@NonNull Context context) {
		if (INSTANCE == null) {
			synchronized (DownloadManager.class) {
				if (INSTANCE == null) {
					INSTANCE = new DownloadManager(context);
				}
			}
		}
		return INSTANCE;
	}

	public Context getContext() {
		return mContext;
	}

	/**
	 * 下载状态
	 *
	 * @return
	 */
	public int getDownloadStatus() {
		return mDownloadStatus;
	}

	/**
	 * 注册下载监听器
	 *
	 * @param listener
	 */
	public void registerDownloadListener(@NonNull DownloadListener listener) {
		if (mListeners == null) {
			mListeners = new LinkedList<>();
		}
		if (!mListeners.contains(listener)) {
			mListeners.add(listener);
		}
	}

	/**
	 * 移除下载监听器
	 *
	 * @param listener
	 * @return
	 */
	public boolean unregisterDownloadListener(@NonNull DownloadListener listener) {
		return mListeners.remove(listener);
	}

	/**
	 * 移除所有监听器
	 */
	public void clear() {
		mListeners.clear();
	}

	/**
	 * 释放资源
	 */
	public void release() {
		Logd(TAG, "release: ");
		notificationManager = null;
		builder = null;
		if (mFuture != null) {
			mFuture.cancel(true);
			mFuture = null;
		}
		mExecutor = null;
	}

	/**
	 * 停止下载
	 */
	public void pauseDownload() {
		Logd(TAG, "pauseDownload: ");
		mDownloadStatus = PAUSED;
		if (mFuture != null) {
			boolean isPause = mFuture.cancel(true);
			mFuture = null;
			Loge(TAG, "pauseDownload: isPause:" + isPause);
		}
	}

	/**
	 * 继续下载
	 */
	public void resumeDownload() {
		Logd(TAG, "resumeDownload: ");
		mDownloadStatus = RESUMED;
		submit();
	}

	/**
	 * 开始下载
	 */
	public void startDownload() {
		Logd(TAG, "startDownload: mDownloadStatus:" + mDownloadStatus);
		if (mDownloadStatus == DownloadManager.DOWNLOADING) {
			return;
		}
		mDownloadStatus = INIT;
		if (mHandler == null) {
			mHandler = new MyHandler();
		}
		submit();
	}

	/**
	 * 发送通知
	 */
	public void sendNotification() {
		Logd(TAG, "sendNotification: ");
		notificationManager = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
		// 8.0适配
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			final NotificationChannel channel = new NotificationChannel(
					CHANNEL_ID,
					CHANNEL_NAME,
					NotificationManager.IMPORTANCE_HIGH);
			notificationManager.createNotificationChannel(channel);
		}
		// 设置参数
		builder = new NotificationCompat.Builder(mContext, CHANNEL_ID);
		String string = mContext.getResources().getString(R.string.app_name);
		builder.setSmallIcon(R.mipmap.ic_launcher)//设置小图标
				.setContentTitle(string)//设置通知标题
				.setContentText("下载中")//设置通知内容
				.setWhen(System.currentTimeMillis())//设置通知时间
				.setAutoCancel(false)//点击通知后是否自动清除
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setContentInfo("0%")
				.setTicker(string + "更新中")
				.setDefaults(Notification.DEFAULT_LIGHTS)
				.setLights(Color.BLUE, 3000, 100)
				.setContentIntent(getPendingIntent(INIT))
				.setOngoing(true);

		Notification notification = builder.build();
//		notification.defaults |= Notification.DEFAULT_LIGHTS;
//		notification.defaults = Notification.DEFAULT_VIBRATE;
//		notification.defaults = Notification.DEFAULT_SOUND;
//		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;

		notificationManager.notify(NOTIFY_ID, notification);
	}

	private PendingIntent getPendingIntent(int downloadStatus) {
		Intent intent = new Intent();
		intent.setAction(APK_DOWNLOAD_RECEIVER);
		intent.putExtra(DOWNLOAD_PROGRESS, downloadStatus);
		UpgradeInfo upgradeInfo = Beta.getUpgradeInfo();
		if (upgradeInfo != null) {
			String apkUrl = upgradeInfo.apkUrl;
			intent.putExtra(APK_URL, apkUrl);
		}
		PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		return pendingIntent;
	}

	/**
	 * 提交任务
	 */
	private void submit() {
		if (mExecutor == null) {
			mExecutor = Executors.newSingleThreadExecutor();
		}
		if (mFuture != null) {
			boolean cancel = mFuture.cancel(true);
			Logd(TAG, "submit: cancel:" + cancel);
		}
		mFuture = mExecutor.submit(this);
	}

	@Override
	public void run() {
		Logd(TAG, "run: ");
		if (mDownloadStatus != RESUMED && mDownloadStatus != DOWNLOADING) {
			sendNotification();
		}
		downloadApk();
	}

	/**
	 * 下载apk
	 */
	private void downloadApk() {
		Logd(TAG, "downloadApk: ");
		UpgradeInfo upgradeInfo = Beta.getUpgradeInfo();
		if (upgradeInfo != null) {
			String apkUrl = upgradeInfo.apkUrl;
			String apkName = ApkUtil.getApkName(apkUrl);
			Logd(TAG, "downloadApk: apkName:" + apkName);
			if (!TextUtils.isEmpty(apkName)) {
				final String path = ApkUtil.getApkPath(apkUrl);
				final File apkFile = new File(path);
				long startLen = apkFile.length();
				long contentLen = upgradeInfo.fileSize;
				Logd(TAG, "downloadApk: contentLen:" + contentLen + ",startLen:" + startLen);
				if (contentLen == startLen) {
					sendCompleteMsg(path);
					return;
				}
				final Request request = new Request.Builder()
						.addHeader("RANGE", "bytes=" + startLen + "-" + contentLen)
						.url(apkUrl)
						.build();
				Call call = OkHttpFactory.createOkHttp().newCall(request);
				try {
					Response response = call.execute();
					writeApk(response, apkFile, contentLen, startLen);
				} catch (IOException e) {
					e.printStackTrace();
					Loge(TAG, "downloadApk: e");
					if (e instanceof InterruptedIOException) {
						onPause();
					} else {
						sendFailureMsg(e);
					}
				}
			}
		}
	}

	/**
	 * 发送下载完成消息
	 *
	 * @param path apk路径
	 */
	private void sendCompleteMsg(String path) {
		Logd(TAG, "sendCompleteMsg: path:" + path + ",mHandler:" + mHandler);
		if (mHandler != null) {
			Message message = mHandler.obtainMessage();
			message.what = COMPLETE;
			message.obj = path;
			mHandler.sendMessage(message);
		}
	}

	/**
	 * 发送下载失败消息
	 *
	 * @param e 异常对象
	 */
	private void sendFailureMsg(IOException e) {
		Logd(TAG, "sendFailureMsg: e:" + e.getMessage() + ",mHandler:" + mHandler);
		if (mHandler != null) {
			Message message = mHandler.obtainMessage();
			message.what = FAILED;
			message.obj = e;
			mHandler.sendMessage(message);
		}
	}

	/**
	 * 发送下载进度消息
	 *
	 * @param contentLen
	 * @param downloadLen
	 * @param progress
	 */
	private void sendProgressMsg(int contentLen, int downloadLen, int progress) {
		Logd(TAG, "sendProgressMsg: contentLen:" + contentLen + ",downloadLen:" + downloadLen + "sendProgressMsg: progress:" + progress + ",mHandler:" + mHandler);
		if (mHandler != null) {
			Message message = mHandler.obtainMessage();
			message.what = DOWNLOADING;
			message.obj = progress;
			message.arg1 = contentLen;
			message.arg2 = downloadLen;
			mHandler.sendMessage(message);
		}
	}

	/**
	 * 写入apk文件
	 *
	 * @param response
	 * @param apkFile
	 * @param contentLen
	 * @param startLen
	 */
	private void writeApk(Response response, File apkFile, long contentLen, long startLen) {
		long downloadLen = startLen;
		InputStream inputStream = null;
		FileOutputStream outputStream = null;
		try {
			inputStream = response.body().byteStream();
			outputStream = new FileOutputStream(apkFile, true);
			byte[] buffer = new byte[2048];
			int len = 0;
			while ((len = inputStream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, len);
				downloadLen += len;
				int progress = (int) (downloadLen * 100 / contentLen);
				Logd(TAG, "writeApk: progress:" + progress);
				if (progress < 100) {
					updateProgress(mDownloadStatus, progress);
					sendProgressMsg((int) contentLen, (int) startLen, progress);
				}
			}
			outputStream.flush();
			onComplete();
			sendCompleteMsg(apkFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			Loge(TAG, "writeApk: e");
			if (e instanceof InterruptedIOException) {
				onPause();
			} else {
				sendFailureMsg(e);
			}
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private final class MyHandler extends Handler {
		public MyHandler() {
			super(Looper.myLooper());
		}

		@Override
		public void handleMessage(Message msg) {
			int what = msg.what;
			Logd(TAG, "handleMessage: what:" + what);
			switch (what) {
				case DOWNLOADING:
					Logd(TAG, "handleMessage: DOWNLOADING");
					mDownloadStatus = DOWNLOADING;
					Object obj = msg.obj;
					if (obj instanceof Integer) {
						int contentLen = msg.arg1;
						int downloadLen = msg.arg2;
						int progress = (int) obj;
						if (mListeners != null) {
							for (DownloadListener mListener : mListeners) {
								if (mListener != null) {
									mListener.onReceive(contentLen, downloadLen, progress);
								}
							}
						}
					}
					break;
				case FAILED:
					Logd(TAG, "handleMessage: FAILED");
					mDownloadStatus = FAILED;
					onFailure();
					obj = msg.obj;
					if (obj instanceof IOException) {
						if (mListeners != null) {
							for (DownloadListener mListener : mListeners) {
								if (mListener != null) {
									mListener.onFailed((IOException) obj);
								}
							}
						}
					}
					break;
				case COMPLETE:
					Logd(TAG, "handleMessage: COMPLETE");
					mDownloadStatus = COMPLETE;
					obj = msg.obj;
					if (obj != null) {
						if (mListeners != null) {
							for (DownloadListener mListener : mListeners) {
								if (mListener != null) {
									mListener.onCompleted(obj.toString());
								}
							}
						}
						ApkUtil.installApk(obj.toString());
					}
					break;
			}
		}
	}

	/**
	 * 下载暂停处理
	 */
	private void onPause() {
		Loge(TAG, "onPause: ");
		builder.setContentText("暂停中");
		builder.setContentIntent(getPendingIntent(PAUSED));
		Notification notification = builder.build();
		notificationManager.notify(NOTIFY_ID, notification);
	}

	/**
	 * 下载失败处理
	 */
	private void onFailure() {
		Loge(TAG, "onFailure: ");
		builder.setContentText("下载失败");
		builder.setContentIntent(getPendingIntent(FAILED));
		Notification notification = builder.build();
		notificationManager.notify(NOTIFY_ID, notification);
	}

	/**
	 * 下载完成处理
	 */
	private void onComplete() {
		builder.setContentText("下载完成");
		builder.setProgress(100, 100, false);
		builder.setContentInfo("100%");
		builder.setContentIntent(getPendingIntent(COMPLETE));
		Notification notification = builder.build();
//                            notification.flags = Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(NOTIFY_ID, notification);
//		notification.contentView.setTextViewText(android.support.compat.R.id.text,"下载完成");
//                    if (notificationManager != null) {
//                        notificationManager.cancel(NOTIFY_ID);
//                    }
	}

	/**
	 * 更新广播进度
	 *
	 * @param mStatus
	 * @param progress
	 */
	private void updateProgress(int mStatus, int progress) {
		Logd(TAG, "updateProgress: mDownloadStatus:" + mStatus + ",progress:" + progress);
		builder.setProgress(100, progress, false);
		builder.setContentInfo(progress + "%");
		builder.setContentText("下载中");
		if (mStatus != DOWNLOADING) {
			builder.setContentIntent(getPendingIntent(DOWNLOADING));
		}
		Notification notification = builder.build();
		notificationManager.notify(NOTIFY_ID, notification);
	}
}
