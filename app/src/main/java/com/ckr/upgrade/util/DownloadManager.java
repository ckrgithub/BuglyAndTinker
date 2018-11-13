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
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.ckr.upgrade.DownloadReceiver.DOWNLOAD_RECEIVER;
import static com.ckr.upgrade.util.UpgradeLog.Logd;

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
    public final static int DELETED = 4;
    public final static int FAILED = 5;
    private final static int NOTIFY_ID = 1111;

    private static DownloadManager INSTANCE;
    private final Context mContext;
    private final LinkedList<DownloadListener> mListeners;
    private long downloadLen = 0;
    private long contentLen = 0;
    private int mDownloadStatus = INIT;
    private MyHandler mHandler;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private Call call;
    private ExecutorService mExecutor;
    private Future<?> mFuture;


    private DownloadManager(Context context) {
        mContext = context;
        mListeners = new LinkedList<>();
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

    public void clear() {
        mListeners.clear();
    }

    /**
     * 停止下载
     */
    public void pauseDownload() {
        mDownloadStatus = PAUSED;
        if (call != null) {
            if (!call.isCanceled()) {
                call.cancel();
                call = null;
            }
        }
    }

    /**
     * 继续下载
     */
    public void resumeDownload() {
        submit();
    }

    /**
     * 开始下载
     */
    public void startDownload() {
        if (mDownloadStatus == DownloadManager.DOWNLOADING) {
            return;
        }
        mDownloadStatus = INIT;
        if (mHandler == null) {
            mHandler = new MyHandler();
        }
        sendNotification();
        submit();
    }

    /**
     * 发送通知
     */
    public void sendNotification() {
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
        builder.setDefaults(Notification.DEFAULT_LIGHTS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setTicker("app更新中")
                .setLights(Color.BLUE, 5000, 500)
                .setAutoCancel(true)
                .setContentTitle("bugly")
                .setContentText("下载中")
                .setContentInfo("0%")
                .setWhen(System.currentTimeMillis())
                .setContentIntent(getPendingIntent(INIT))
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher);

        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_LIGHTS;
        notification.defaults = Notification.DEFAULT_VIBRATE;
        notification.defaults = Notification.DEFAULT_SOUND;
        notification.flags = Notification.FLAG_ONGOING_EVENT;

        notificationManager.notify(NOTIFY_ID, notification);
    }

    private PendingIntent getPendingIntent(int downloadStatus) {
        Intent intent = new Intent();
        intent.setAction(DOWNLOAD_RECEIVER);
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
            Logd(TAG, "download: cancel:" + cancel);
        }
        mFuture = mExecutor.submit(this);
    }

    @Override
    public void run() {
        downloadApk();
    }

    private void downloadApk() {
        UpgradeInfo upgradeInfo = Beta.getUpgradeInfo();
        if (upgradeInfo != null) {
            String apkUrl = upgradeInfo.apkUrl;
            String apkName = ApkUtil.getApkName(apkUrl);
            Logd(TAG, "onStartCommand: apkName:" + apkName);
            if (!TextUtils.isEmpty(apkName)) {
                final String path = ApkUtil.getApkPath(apkUrl);
                final File apkFile = new File(path);
                long startLen = apkFile.length();
                contentLen = upgradeInfo.fileSize;
                downloadLen = (int) startLen;
                if (contentLen == downloadLen) {
                    sendCompleteMsg(path);
                    return;
                }
                Logd(TAG, "onStartCommand: downloadLen:" + downloadLen + ",contentLen:" + contentLen + ",exist:" + apkFile.exists());
                final Request request = new Request.Builder()
                        .addHeader("RANGE", "bytes=" + startLen + "-" + contentLen)
                        .url(apkUrl)
                        .build();
                call = OkHttpFactory.createOkHttp().newCall(request);
                try {
                    Response response = call.execute();
                    writeApk(response, apkFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    sendFailureMsg(e);
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
        if (mHandler != null) {
            Message message = mHandler.obtainMessage();
            message.what = FAILED;
            message.obj = e;
            mHandler.sendMessage(message);
        }
    }

    /**
     * 发送下载进度消息
     */
    private void sendProgressMsg() {
        if (mHandler != null) {
            Message message = mHandler.obtainMessage();
            message.what = DOWNLOADING;
            message.obj = downloadLen;
            mHandler.sendMessage(message);
        }
    }

    /**
     * 写入apk文件
     *
     * @param response
     * @param apkFile
     */
    private void writeApk(Response response, File apkFile) {
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
                sendProgressMsg();
            }
            outputStream.flush();
            sendCompleteMsg(apkFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            sendFailureMsg(e);
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
            switch (msg.what) {
                case DOWNLOADING:
                    int mStatus = mDownloadStatus;
                    mDownloadStatus = DOWNLOADING;
                    Object obj = msg.obj;
                    if (obj instanceof Long) {
                        Long downloadLen = (Long) obj;
                        int progress = (int) (downloadLen * 100 / contentLen);
                        if (progress < 100) {
                            builder.setProgress(100, progress, false);
                            builder.setContentInfo(progress + "%");
                            if (mStatus != DOWNLOADING) {
                                builder.setContentIntent(getPendingIntent(DOWNLOADING));
                            }
                            Notification notification = builder.build();
                            notificationManager.notify(NOTIFY_ID, notification);
                        }
                        for (DownloadListener mListener : mListeners) {
                            if (mListener != null) {
                                mListener.onReceive(contentLen, downloadLen, progress);
                            }
                        }
                    }
                    break;
                case FAILED:
                    mDownloadStatus = FAILED;
                    obj = msg.obj;
                    if (obj instanceof IOException) {
                        for (DownloadListener mListener : mListeners) {
                            if (mListener != null) {
                                mListener.onFailed((IOException) obj);
                            }
                        }
                    }
                    break;
                case COMPLETE:
                    mDownloadStatus = COMPLETE;
                    Notification notification = builder.build();
                    builder.setContentIntent(getPendingIntent(COMPLETE));
//                            notification.flags = Notification.FLAG_AUTO_CANCEL;
//                                stopSelf();
                    notificationManager.notify(NOTIFY_ID, notification);
//                    if (notificationManager != null) {
//                        notificationManager.cancel(NOTIFY_ID);
//                    }
                    obj = msg.obj;
                    if (obj != null) {
                        for (DownloadListener mListener : mListeners) {
                            if (mListener != null) {
                                mListener.onCompleted(obj.toString());
                            }
                        }
                        ApkUtil.installApk(obj.toString());
                    }
                    break;
            }
        }
    }
}
