package com.ckr.upgrade;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.tencent.bugly.beta.Beta;
import com.tencent.tinker.anno.DefaultLifeCycle;
import com.tencent.tinker.entry.DefaultApplicationLike;
import com.tencent.tinker.loader.shareutil.ShareConstants;

/**
 * Created by ckr on 2018/11/27.
 * 配置热更新功能
 */

@SuppressWarnings("unused")
@DefaultLifeCycle(application = "com.ckr.upgrade.CkrApplication",
        flags = ShareConstants.TINKER_ENABLE_ALL,
        loadVerifyFlag = false)
public class HotfixApplication extends DefaultApplicationLike {
    private static HotfixApplication instance;

    public static HotfixApplication getInstance() {
        return instance;
    }

    public HotfixApplication(Application application, int tinkerFlags, boolean tinkerLoadVerifyFlag, long applicationStartElapsedTime, long applicationStartMillisTime, Intent tinkerResultIntent) {
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
        instance = this;
    }

}
