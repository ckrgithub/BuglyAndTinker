package com.ckr.bugly;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.IdRes;
import android.view.View;
import android.widget.TextView;

import com.ckr.upgrade.DownloadManager;
import com.ckr.upgrade.UpgradeInfo;
import com.ckr.upgrade.listener.OnInstallApkListener;
import com.ckr.upgrade.util.ApkUtil;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.tinker.TinkerManager;

import java.io.File;

import static com.ckr.upgrade.DownloadManager.COMPLETE;
import static com.ckr.upgrade.util.ApkUtil.REQUEST_CODE_INSTALL;
import static com.ckr.upgrade.util.UpgradeLog.Logd;

public class MainActivity extends BaseActivity implements View.OnClickListener, OnInstallApkListener {
    private static final String TAG = "MainActivity";
    private TextView versionView;
    private TextView tinkerIdView;
    private View checkUpgrade;
    private Intent intent;

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
        setOnClickListener(R.id.btnInstaller);
        setOnClickListener(R.id.btnNotification);
        setOnClickListener(R.id.btnDownload);
        setOnClickListener(R.id.btnPause);

        versionView.append(BuildConfig.VERSION_NAME + "-" + BuildConfig.VERSION_CODE);
        String tinkerId = TinkerManager.getTinkerId();
        Logd(TAG, "onCreate: tinkerId:" + tinkerId);
        tinkerIdView.append(tinkerId);
    }

    private void setOnClickListener(@IdRes int viewId) {
        findViewById(viewId).setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logd(TAG, "onActivityResult: requestCode:" + requestCode + ",resultCode:" + resultCode);
        if (requestCode == REQUEST_CODE_INSTALL) {
            Context context = getContext();
            boolean canInstall = ApkUtil.hasInstallPermission(context);
            if (canInstall) {
                DownloadManager with = DownloadManager.with(context);
                UpgradeInfo upgradeInfo = with.getUpgradeInfo();
                if (upgradeInfo != null) {
                    int downloadStatus = with.getDownloadStatus();
                    if (downloadStatus == COMPLETE) {
                        ApkUtil.installApk(ApkUtil.getApkPath(upgradeInfo.apkUrl, context), context);
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (intent != null) {
            stopService(intent);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.checkUpgrade:
                Logd(TAG, "onClick: 检查更新");
                Beta.checkUpgrade(true, true);
                break;
            case R.id.btnService:
                intent = new Intent(this, DownLoadService.class);
                startService(intent);
                break;
            case R.id.btnInstaller:
                boolean canInstall = true;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    canInstall = getPackageManager().canRequestPackageInstalls();
                }
                if (!canInstall) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, REQUEST_CODE_INSTALL);
                } else {
                    DownloadManager downloadManager = DownloadManager.with(this);
                    UpgradeInfo upgradeInfo = downloadManager.getUpgradeInfo();
                    if (upgradeInfo != null) {
                        String apkUrl = upgradeInfo.apkUrl;
                        String path = ApkUtil.getApkPath(apkUrl, this);
                        int downloadStatus = downloadManager.getDownloadStatus();
                        boolean isComplete = downloadStatus == COMPLETE;
                        if (!isComplete) {
                            long fileSize = upgradeInfo.fileSize;
                            File file = new File(path);
                            if (file.length() >= fileSize) {
                                isComplete = true;
                            }
                        }
                        if (isComplete) {
                            ApkUtil.installApk(path, this);
                        }
                    }
                }
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
                    if (with.getUpgradeInfo() == null) {
                        with.startDownload();
                    }
                }
                break;
            case R.id.btnPause:
                DownloadManager.with(this.getApplicationContext()).pauseDownload();
                break;
        }
    }

    @Override
    public void requestInstallPermission() {
        ApkUtil.openUnknownAppSourcesActivity(this);
    }
}
