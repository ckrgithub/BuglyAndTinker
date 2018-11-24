package com.ckr.upgrade.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.ckr.upgrade.R;
import com.ckr.upgrade.UpgradeInfo;
import com.ckr.upgrade.listener.DownloadListener;
import com.ckr.walle.ChannelUtil;

import org.apache.http.conn.ConnectTimeoutException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
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
	public final static int INIT = 0;
	public final static int COMPLETE = 1;
	public final static int DOWNLOADING = 2;
	public final static int PAUSED = 3;
	public final static int RESUMED = 4;
	public final static int FAILED = 5;
	private final static int NOTIFY_ID = 1129;

	private static DownloadManager INSTANCE;
	private final Context mContext;
	private LinkedList<DownloadListener> mListeners;
	private int mDownloadStatus = INIT;
	private InternalHandler mHandler;
	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder mBuilder;
	private ExecutorService mExecutor;
	private Future<?> mFuture;
	private boolean isAutoInstall = true;//是否自动安装
	private boolean enableNotification = true;//是否发送通知
	private boolean enableWriteChannelInfo = true;//是否写入渠道
	private UpgradeInfo mUpgradeInfo = null;
	private static final int MAX_PROGRESS = 100;

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

	public void setAutoInstall(boolean autoInstall) {
		isAutoInstall = autoInstall;
	}

	public void setEnableNotification(boolean enableNotification) {
		this.enableNotification = enableNotification;
	}

	public void setEnableWriteChannelInfo(boolean enableWriteChannelInfo) {
		this.enableWriteChannelInfo = enableWriteChannelInfo;
	}

	public boolean isAutoInstall() {
		return isAutoInstall;
	}

	public boolean isEnableNotification() {
		return enableNotification;
	}

	public boolean isEnableWriteChannelInfo() {
		return enableWriteChannelInfo;
	}

	public UpgradeInfo getUpgradeInfo() {
		return mUpgradeInfo;
	}

	public void setUpgradeInfo(UpgradeInfo upgradeInfo) {
		this.mUpgradeInfo = upgradeInfo;
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
		mNotificationManager = null;
		mUpgradeInfo = null;
		mBuilder = null;
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
		Logd(TAG, "resumeDownload: mDownloadStatus:" + mDownloadStatus);
		if (mDownloadStatus != PAUSED) {
			return;
		}
		mDownloadStatus = RESUMED;
		submit();
	}

	/**
	 * 开始下载
	 */
	public void startDownload() {
		if (mUpgradeInfo == null) {
			throw new NullPointerException("mUpgradeInfo is null");
		}
		Logd(TAG, "startDownload: mDownloadStatus:" + mDownloadStatus);
		if (mDownloadStatus == DownloadManager.DOWNLOADING) {
			return;
		}
		mDownloadStatus = INIT;
		if (mHandler == null) {
			mHandler = new InternalHandler();
		}
		submit();
	}

	/**
	 * 发送通知
	 */
	public void sendNotification() {
		Logd(TAG, "sendNotification: ");
		mNotificationManager = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
		String channelId = getString(R.string.notification_channel_id);
		// 8.0适配
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			final NotificationChannel channel = new NotificationChannel(
					channelId,
					getString(R.string.notification_channel_name),
					NotificationManager.IMPORTANCE_HIGH);
			channel.setDescription(getString(R.string.notification_channel_description));
			mNotificationManager.createNotificationChannel(channel);
		}
		// 设置参数
		mBuilder = new NotificationCompat.Builder(mContext, channelId);
		String string = getString(R.string.notification_content_title);
		mBuilder.setSmallIcon(R.mipmap.ic_launcher)//设置小图标
				.setContentTitle(string)//设置通知标题
				.setContentText(getString(R.string.download_status_downloading))//设置通知内容
				.setContentInfo(0 + getString(R.string.upgrade_progress_symbol))
				.setTicker(string + getString(R.string.notification_ticker))
				.setWhen(System.currentTimeMillis())//设置通知时间
				.setAutoCancel(false)//点击通知后是否自动清除
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setDefaults(Notification.DEFAULT_LIGHTS)
				.setLights(ContextCompat.getColor(mContext, R.color.notification_light_color),
						mContext.getResources().getInteger(R.integer.time_light_on),
						mContext.getResources().getInteger(R.integer.time_light_off))
				.setContentIntent(getPendingIntent(INIT))
				.setOngoing(true);

		Notification notification = mBuilder.build();
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;

		mNotificationManager.notify(NOTIFY_ID, notification);
	}

	@NonNull
	private String getString(@StringRes int resId) {
		return mContext.getString(resId);
	}

	private PendingIntent getPendingIntent(int downloadStatus) {
		Intent intent = new Intent();
		intent.setAction(APK_DOWNLOAD_RECEIVER);
		intent.putExtra(DOWNLOAD_PROGRESS, downloadStatus);
		if (mUpgradeInfo != null) {
			String apkUrl = mUpgradeInfo.apkUrl;
			intent.putExtra(APK_URL, ApkUtil.getApkPath(apkUrl, mContext));
		}
		PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 2018, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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
		if (enableNotification) {
			if (mDownloadStatus != RESUMED && mDownloadStatus != DOWNLOADING) {
				sendNotification();
			}
		}
		downloadApk();
	}

	/**
	 * 下载apk
	 */
	private void downloadApk() {
		Logd(TAG, "downloadApk: ");
		if (mUpgradeInfo != null) {
			String apkUrl = mUpgradeInfo.apkUrl;
			String apkName = ApkUtil.getApkName(apkUrl);
			Logd(TAG, "downloadApk: apkName:" + apkName);
			if (!TextUtils.isEmpty(apkName)) {
				final String path = ApkUtil.getApkPath(apkUrl, mContext);
				final File apkFile = new File(path);
				long startLen = apkFile.length();
				long contentLen = mUpgradeInfo.fileSize;
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
						if (e instanceof SocketTimeoutException) {
							sendFailureMsg(e);
						} else if (e instanceof ConnectTimeoutException) {
							sendFailureMsg(e);
						} else {
							sendPauseMsg();
						}
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
	 * 发送暂停下载消息
	 */
	private void sendPauseMsg() {
		Logd(TAG, "sendPauseMsg  mHandler:" + mHandler);
		if (mHandler != null) {
			Message message = mHandler.obtainMessage();
			message.what = PAUSED;
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
		boolean isComplete = false;
		try {
			inputStream = response.body().byteStream();
			outputStream = new FileOutputStream(apkFile, true);
			byte[] buffer = new byte[2048];
			int len = 0;
			while ((len = inputStream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, len);
				downloadLen += len;
				int progress = (int) (downloadLen * MAX_PROGRESS / contentLen);
				Logd(TAG, "writeApk: progress:" + progress);
				if (progress < MAX_PROGRESS) {
					updateProgress(mDownloadStatus, progress);
					sendProgressMsg((int) contentLen, (int) startLen, progress);
				}
			}
			outputStream.flush();
			isComplete = true;
		} catch (IOException e) {
			e.printStackTrace();
			Loge(TAG, "writeApk: e");
			if (e instanceof InterruptedIOException) {
				sendPauseMsg();
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
		if (isComplete) {
			if (enableWriteChannelInfo) {
				ChannelUtil.setChannelInfo(apkFile, ChannelUtil.getChannelInfo(mContext), true);
			}
			sendCompleteMsg(apkFile.getAbsolutePath());
		}
	}

	private final class InternalHandler extends Handler {
		public InternalHandler() {
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
					onComplete();
					obj = msg.obj;
					if (obj != null) {
						if (mListeners != null) {
							for (DownloadListener mListener : mListeners) {
								if (mListener != null) {
									mListener.onCompleted(obj.toString());
								}
							}
						}
						if (isAutoInstall) {
							ApkUtil.installApk(obj.toString(), mContext);
						}
					}
					break;
				case PAUSED:
					mDownloadStatus = PAUSED;
					onPause();
					if (mListeners != null) {
						for (DownloadListener mListener : mListeners) {
							if (mListener != null) {
								mListener.onPaused();
							}
						}
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
		mBuilder.setContentText(getString(R.string.notification_status_pause));
		mBuilder.setContentIntent(getPendingIntent(PAUSED));
		Notification notification = mBuilder.build();
		mNotificationManager.notify(NOTIFY_ID, notification);
	}

	/**
	 * 下载失败处理
	 */
	private void onFailure() {
		Loge(TAG, "onFailure: ");
		if (mBuilder != null) {
			mBuilder.setContentText(getString(R.string.download_status_failed));
			mBuilder.setContentIntent(getPendingIntent(FAILED));
			Notification notification = mBuilder.build();
			mNotificationManager.notify(NOTIFY_ID, notification);
		}
	}

	/**
	 * 下载完成处理
	 */
	private void onComplete() {
		Logd(TAG, "onComplete: ");
		if (mBuilder != null) {
			mNotificationManager.cancel(NOTIFY_ID);
		}
	}

	/**
	 * 更新广播进度
	 *
	 * @param mStatus
	 * @param progress
	 */
	private void updateProgress(int mStatus, int progress) {
		Logd(TAG, "updateProgress: mDownloadStatus:" + mStatus + ",progress:" + progress);
		if (mBuilder != null) {
			mBuilder.setProgress(MAX_PROGRESS, progress, false);
			mBuilder.setContentInfo(progress + getString(R.string.upgrade_progress_symbol));
			mBuilder.setContentText(getString(R.string.download_status_downloading));
			if (mStatus != DOWNLOADING) {
				mBuilder.setContentIntent(getPendingIntent(DOWNLOADING));
			}
			Notification notification = mBuilder.build();
			mNotificationManager.notify(NOTIFY_ID, notification);
		}
	}
}
