package com.ckr.bugly.listener;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.ckr.bugly.MyApplication;
import com.ckr.walle.ChannelUtil;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.download.DownloadListener;
import com.tencent.bugly.beta.download.DownloadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.ckr.bugly.util.UpgradeLog.Logd;
import static com.ckr.bugly.util.UpgradeLog.Loge;
import static com.tencent.bugly.beta.tinker.TinkerManager.getApplication;

/**
 * Created on 2018/11/11
 *
 * @author ckr
 */

public class MyDownloadListener implements DownloadListener {
	private static final String TAG = "MyDownloadListener";
	public static final String APK_NAME = "_channel.apk";
	private ApkDownloadListener mApkDownloadListener;

	@Override
	public void onReceive(DownloadTask downloadTask) {
		Logd(TAG, "onReceive: ");
		if (mApkDownloadListener != null) {
			mApkDownloadListener.onReceive(downloadTask);
		}
	}

	@Override
	public void onCompleted(DownloadTask downloadTask) {
		Logd(TAG, "onCompleted: ");
		if (mApkDownloadListener != null) {
			mApkDownloadListener.onCompleted(downloadTask);
		}
	}

	@Override
	public void onFailed(DownloadTask downloadTask, int i, String s) {
		Loge(TAG, "onFailed: i:" + i + ",s:" + s);
		if (mApkDownloadListener != null) {
			mApkDownloadListener.onFailed(downloadTask, i, s);
		}
	}


	class ApkTask extends AsyncTask<File, Void, Boolean> {

		@Override
		protected Boolean doInBackground(File... files) {
			File file = files[0];
			boolean isSuccess = setChannelInfo(file);
			return isSuccess;
		}


		@Override
		protected void onPostExecute(Boolean aBoolean) {
			super.onPostExecute(aBoolean);
			Logd(TAG, "onPostExecute: " + aBoolean);
			if (aBoolean) {
				DownloadTask strategyTask = Beta.getStrategyTask();
				File saveFile = strategyTask.getSaveFile();
				String fileName = getFileName(saveFile);
				File apk = new File(saveFile.getParentFile(), fileName);
				Logd(TAG, "onPostExecute: apk:" + apk.getAbsolutePath());
				installApk(apk);
			}
		}
	}

	/**
	 * copy一份apk文件，并写入渠道信息
	 *
	 * @param file
	 * @return
	 */
	private boolean setChannelInfo(@NonNull File file) {
		String newName = getFileName(file);
		Logd(TAG, "setChannelInfo: newName:" + newName);
		FileInputStream inputStream = null;
		FileOutputStream outputStream = null;
		File channelFile = new File(file.getParentFile(), newName);
		boolean isSuccess = false;
		try {
			inputStream = new FileInputStream(file);
			outputStream = new FileOutputStream(channelFile);
			byte[] bytes = new byte[1024];
			int len = 0;
			while ((len = inputStream.read(bytes)) > 0) {
				outputStream.write(bytes, 0, len);
			}
			isSuccess = true;
		} catch (FileNotFoundException e) {
			isSuccess = false;
			e.printStackTrace();
		} catch (IOException e) {
			isSuccess = false;
			e.printStackTrace();
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
		if (isSuccess) {
			Logd(TAG, "setChannelInfo: path:" + channelFile.getAbsolutePath());
			ChannelUtil.setChannelInfo(channelFile, ChannelUtil.getChannelInfo(MyApplication.getInstance().getApplication().getApplicationContext()), true);
		}
		return isSuccess;
	}

	/**
	 * 得到渠道apk文件名
	 *
	 * @param file 源文件
	 * @return
	 */
	@NonNull
	private String getFileName(@NonNull File file) {
		String name = file.getName();
		String newName = null;
		if (name.endsWith(".apk")) {
			newName = name.substring(0, name.length() - 4) + APK_NAME;
		} else {
			newName = name + APK_NAME;
		}
		return newName;
	}

	/**
	 * 安装apk
	 *
	 * @param apk
	 */
	private void installApk(File apk) {
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			Uri uri = getUri(apk, MyApplication.getInstance().getApplication().getApplicationContext());
			Log.d(TAG, "installApk: uri:" + uri);
			intent.setDataAndType(uri, "application/vnd.android.package-archive");
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getApplication().getApplicationContext().startActivity(intent);
		} catch (Exception e) {
			Log.d(TAG, "installApk: exception");
			e.printStackTrace();
		}
	}

	/**
	 * @param cameraFile
	 * @param context
	 * @return
	 */
	public static Uri getUri(File cameraFile, Context context) {
		Uri imageUri;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			//通过FileProvider创建一个content类型的Uri
			Context applicationContext = context.getApplicationContext();
			imageUri = FileProvider.getUriForFile(applicationContext, applicationContext.getPackageName() + ".fileProvider", cameraFile);
		} else {
			imageUri = Uri.fromFile(cameraFile);
		}
		return imageUri;
	}

	public void registerApkDownloadListener(ApkDownloadListener listener) {
		mApkDownloadListener = listener;
	}

	public void unregisterApkDownloadListener() {
		mApkDownloadListener = null;
	}

	public interface ApkDownloadListener {
		void onReceive(com.tencent.bugly.beta.download.DownloadTask downloadTask);

		void onCompleted(com.tencent.bugly.beta.download.DownloadTask downloadTask);

		void onFailed(com.tencent.bugly.beta.download.DownloadTask downloadTask, int i, java.lang.String s);
	}

}
