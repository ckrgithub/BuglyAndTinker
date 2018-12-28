package com.ckr.bugly;

import android.app.Application;

import com.ckr.upgrade.UpgradeConfig;
import com.ckr.upgrade.UpgradeManager;
import com.ckr.upgrade.util.UpgradeLog;
import com.ckr.walle.ChannelUtil;

/**
 * Created by ckr on 2018/11/10.
 */
public class UpgradeApplication extends Application {
    private static final String BUGLY_ID = "83ffe4ff10";

    @Override
    public void onCreate() {
        super.onCreate();
        UpgradeLog.debug(BuildConfig.IS_DEBUG);
        UpgradeConfig.isDebug = BuildConfig.IS_DEBUG;
        UpgradeConfig.isAutoInstall = true;
        UpgradeConfig.enableNotification = true;
        UpgradeConfig.enableWriteChannelInfo = true;
        UpgradeConfig.pauseDownloadWhenClickNotify = true;
        UpgradeConfig.canShowUpgradeActs.add(MainActivity.class);
        UpgradeConfig upgradeConfig = new UpgradeConfig(BUGLY_ID, BuildConfig.VERSION_NAME, ChannelUtil.getChannelInfo(this.getApplicationContext()), R.mipmap.ic_launcher);
        //升级功能配置
        UpgradeManager.init(this, upgradeConfig, false);
    }


}
