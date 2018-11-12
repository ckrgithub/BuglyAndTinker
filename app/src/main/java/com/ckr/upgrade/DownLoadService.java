package com.ckr.upgrade;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.ckr.upgrade.util.ApkUtil;
import com.ckr.upgrade.util.OkHttpFactory;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

import static com.ckr.upgrade.util.UpgradeLog.Logd;

/**
 * Created on 2018/11/11
 *
 * @author ckr
 */
public class DownLoadService extends Service {
    private static final String TAG = "DownLoadService";
    private static final String DOWNLOAD_RECEIVER = "apk_download_receiver";
    public static final String DOWNLOAD_PROGRESS = "download_progress";
    public static final String APK_URL = "apk_url";
    private static final String CHANNEL_ID = "ckr";
    private static final String CHANNEL_NAME = "upgrade";
    public final static int INIT = 0;
    public final static int COMPLETE = 1;
    public final static int DOWNLOADING = 2;
    public final static int PAUSED = 3;
    public final static int DELETED = 4;
    public final static int FAILED = 5;

    private final static int NOTIFY_ID = 1111;
    private DownLoadBinder binder;
    private long downloadLen = 0;
    private long contentLen = 0;
    private DownloadListener mDownloadListener;
    private int mDownloadStatus = INIT;
    private MyHandler myHandler;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private Call call;

    @Override
    public void onCreate() {
        super.onCreate();
        myHandler = new MyHandler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logd(TAG, "onStartCommand: flags:" + flags + ",startId:" + startId);
        mDownloadStatus = INIT;
        sendNotification();
        downloadApk();
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

    /**
     * apk下载
     */
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
                Logd(TAG, "onStartCommand: downloadLen:" + downloadLen + ",contentLen:" + contentLen + ",exist:" + apkFile.exists());
                final Request request = new Request.Builder()
                        .addHeader("RANGE", "bytes=" + startLen + "-" + contentLen)
                        .url(apkUrl)
                        .build();
                call = OkHttpFactory.createOkHttp().newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Logd(TAG, "onFailure: ");
                        if (myHandler != null) {
                            Message message = myHandler.obtainMessage();
                            message.what = FAILED;
                            message.obj = e;
                            myHandler.sendMessage(message);
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.d(TAG, "onResponse: ");
                        InputStream inputStream = null;
                        FileOutputStream outputStream = null;
                        try {
                            inputStream = response.body().byteStream();
                            outputStream = new FileOutputStream(apkFile, true);
                            byte[] buffer = new byte[1024];
                            int len = 0;
                            while ((len = inputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, len);
                                downloadLen += len;
                                if (myHandler != null) {
                                    Message message = myHandler.obtainMessage();
                                    message.what = DOWNLOADING;
                                    message.obj = downloadLen;
                                    myHandler.sendMessage(message);
                                }
                            }
                            outputStream.flush();
                            if (myHandler != null) {
                                Message message = myHandler.obtainMessage();
                                message.what = COMPLETE;
                                message.obj = path;
                                myHandler.sendMessage(message);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            if (myHandler != null) {
                                Message message = myHandler.obtainMessage();
                                message.what = FAILED;
                                message.obj = e;
                                myHandler.sendMessage(message);
                            }
                        } finally {
                            if (inputStream != null) {
                                inputStream.close();
                            }
                            if (outputStream != null) {
                                outputStream.close();
                            }
                        }
                    }
                });
            }
        }
    }

    public void cancel(String url) {
        if (call != null) {
            if (!call.isCanceled()) {
                call.cancel();
            }
        }
    }

    /**
     * 通知
     */
    private void sendNotification() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // 8.0适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        // 设置参数
        builder = new NotificationCompat.Builder(this, CHANNEL_ID);
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
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_SOUND;
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
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
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
                        if (mDownloadListener != null) {
                            mDownloadListener.onReceive(contentLen, downloadLen, progress);
                        }
                    }
                    break;
                case FAILED:
                    mDownloadStatus = FAILED;
                    obj = msg.obj;
                    if (obj instanceof IOException) {
                        if (mDownloadListener != null) {
                            mDownloadListener.onFailed((IOException) obj);
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
                        if (mDownloadListener != null) {
                            mDownloadListener.onCompleted(obj.toString());
                        }
                        ApkUtil.installApk(obj.toString());
                    }
                    break;
            }
        }
    }

    public interface DownloadListener {

        void onReceive(long contentLen, long downloadLen, int progress);

        void onCompleted(String path);

        void onFailed(IOException e);

    }
}
