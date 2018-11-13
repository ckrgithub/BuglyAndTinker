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

import com.ckr.upgrade.listener.DownloadListener;
import com.ckr.upgrade.util.ApkUtil;
import com.ckr.upgrade.util.DownloadManager;
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

import static com.ckr.upgrade.DownloadReceiver.DOWNLOAD_RECEIVER;
import static com.ckr.upgrade.util.UpgradeLog.Logd;

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
        downloadManager.startDownload();
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
