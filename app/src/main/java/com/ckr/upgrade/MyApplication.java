package com.ckr.upgrade;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;

import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.tencent.tinker.anno.DefaultLifeCycle;
import com.tencent.tinker.entry.DefaultApplicationLike;
import com.tencent.tinker.loader.shareutil.ShareConstants;

/**
 * Created by ckr on 2018/11/10.
 */

@SuppressWarnings("unused")
@DefaultLifeCycle(application = "com.ckr.upgrade.CkrApplication",
        flags = ShareConstants.TINKER_ENABLE_ALL,
        loadVerifyFlag = false)
public class MyApplication extends DefaultApplicationLike {
    private static final String BUGLY_ID = "83ffe4ff10";

    public MyApplication(Application application, int tinkerFlags, boolean tinkerLoadVerifyFlag, long applicationStartElapsedTime, long applicationStartMillisTime, Intent tinkerResultIntent) {
        super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent);
    }

    @Override
    public void onBaseContextAttached(Context base) {
        super.onBaseContextAttached(base);
        Beta.installTinker(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplication().getApplicationContext();
        //Beta类作为Bugly的初始化扩展，通过Beta类可以修改升级的检测时机，界面元素以及自定义升级行为
        Beta.autoInit = true;//是否自动初始化升级模块
//        Beta.init(context,BuildConfig.IS_DEBUG);//手动初始化升级模块
//        Beta.upgradeCheckPeriod = 60 * 1000;//设置升级检查周期为60s,60s内sdk不重复向后台请求策略
        Beta.initDelay = 3 * 1000;//设置启动延时为3s，app启动1s后初始化sdk,避免影响app启动速度
        Beta.largeIconId = R.mipmap.ic_launcher;//设置通知栏大图标
        Beta.smallIconId = R.mipmap.ic_launcher;//设置通知栏小图标
//        Beta.defaultBannerId=R.mipmap.ic_launcher;//设置更新弹框默认展示的banner
        Beta.storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);//设置sd卡的Download为更新资源存储目录
//        Beta.showInterruptedStrategy = true;//设置点击过确认的弹窗 在app下次启动自动检查更新时会再次显示。
//        Beta.canShowUpgradeActs.add(MainActivity.class);//添加可显示弹窗的Activity
        Beta.enableNotification = true;//设置是否显示消息通知
        Beta.autoDownloadOnWifi=false;//设置wifi下自动下载
        Beta.enableHotfix=true;//设置开启热更新

        Bugly.init(context, BUGLY_ID, BuildConfig.IS_DEBUG);
    }

}
