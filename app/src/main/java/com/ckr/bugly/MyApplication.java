package com.ckr.bugly;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.ckr.upgrade.util.UpgradeConfig;
import com.tencent.bugly.beta.Beta;
import com.tencent.tinker.anno.DefaultLifeCycle;
import com.tencent.tinker.entry.DefaultApplicationLike;
import com.tencent.tinker.loader.shareutil.ShareConstants;

/**
 * Created by ckr on 2018/11/10.
 */
@SuppressWarnings("unused")
@DefaultLifeCycle(application = "com.ckr.bugly.CkrApplication",
        flags = ShareConstants.TINKER_ENABLE_ALL,
        loadVerifyFlag = false)
public class MyApplication extends DefaultApplicationLike {
    private static MyApplication instance;

    public static MyApplication getInstance() {
        return instance;
    }

    public MyApplication(Application application, int tinkerFlags, boolean tinkerLoadVerifyFlag, long applicationStartElapsedTime, long applicationStartMillisTime, Intent tinkerResultIntent) {
        super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent);
    }

    @Override
    public void onBaseContextAttached(Context base) {
        super.onBaseContextAttached(base);
        //热更新配置
        Beta.installTinker(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        //升级功能配置
        UpgradeConfig.init(getApplication(), R.mipmap.ic_launcher, BuildConfig.VERSION_NAME, BuildConfig.IS_DEBUG);
    }


}
