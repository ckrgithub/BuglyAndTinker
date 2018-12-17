package com.ckr.upgrade;

import android.app.Application;
import android.content.Context;
import android.os.Process;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.ckr.upgrade.listener.ApkUpgradeListener;
import com.ckr.upgrade.util.AppTracker;
import com.ckr.upgrade.util.UpgradeLog;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by ckr on 2018/11/29.
 */

public class UpgradeManager {
    private static final String TAG = "UpgradeManager";
    private static AppTracker appTracker;

    public static AppTracker getAppTracker() {
        return appTracker;
    }

    public static void release() {
        appTracker = null;
    }

    /**
     * 应用升级和异常上报
     *
     * @param application
     * @param config
     * @param enableHotfix
     */
    public static void init(@NonNull Application application, @NonNull UpgradeConfig config, boolean enableHotfix) {
        long startTime = System.currentTimeMillis();
        UpgradeLog.Logd(TAG, "init: startTime:" + startTime);
        UpgradeConfig.smallIconId = config.notificationIconId;
        appTracker = new AppTracker();
        application.registerActivityLifecycleCallbacks(appTracker);

        Context context = application.getApplicationContext();
        //<editor-fold desc="异常上报">
        String packageName = context.getPackageName();//当前包名
        String processName = getProcessName(Process.myPid());//当前进程名
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(context);
        strategy.setUploadProcess(processName == null || processName.equals(packageName));//设置上报进程
        strategy.setAppChannel(config.appChannel);//设置渠道信息
        strategy.setAppVersion(config.appVersion);//app版本
        strategy.setAppPackageName(packageName);//app包名
        strategy.setAppReportDelay(10 * 1000);//Bugly在启动10s后联网同步数据
//      </editor-fold>

        //<editor-fold desc="应用升级-->Beta类作为Bugly的初始化扩展，通过Beta类可以修改升级的检测时机，界面元素以及自定义升级行为">
        Beta.autoInit = true;//是否自动初始化升级模块
        Beta.autoCheckUpgrade = false;//是否自动检查更新
//		Beta.checkUpgrade(false,false);//isManual用户手动点击检查，非用户点击操作请传false;isSilence是否显示弹窗等交互，[true:没有弹窗和toast;false:有弹窗或toast]
        Beta.upgradeCheckPeriod = 60 * 1000;//设置升级检查周期为60s,60s内sdk不重复向后台请求策略
        Beta.initDelay = 3 * 1000;//设置启动延时为3s，app启动1s后初始化sdk,避免影响app启动速度
        Beta.enableNotification = true;//设置是否显示消息通知
        Beta.autoDownloadOnWifi = false;//设置wifi下自动下载
        Beta.enableHotfix = enableHotfix;//设置开启热更新
//      </editor-fold>

        Beta.upgradeListener = new ApkUpgradeListener(application.getApplicationContext());//app更新策略监听
        //初始化统一接口:会自动检测更新，不需要手动调用Beta.checkUpgrade(),如需增加自动检查时机可以使用Beta.checkUpgrade(false,false);
        Bugly.init(context, config.buglyId, UpgradeConfig.isDebug, strategy);
        UpgradeLog.Logd(TAG, "init: usedTime:" + (System.currentTimeMillis() - startTime));
    }

    /**
     * 获取进程号对应的进程名
     *
     * @param pid 进程号
     * @return 进程名
     */
    private static String getProcessName(int pid) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
            String processName = reader.readLine();
            if (!TextUtils.isEmpty(processName)) {
                processName = processName.trim();
            }
            return processName;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }
}
