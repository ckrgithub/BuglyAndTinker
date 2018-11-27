package com.ckr.upgrade;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.text.TextUtils;

import com.ckr.upgrade.lifecycle.AppTracker;
import com.ckr.upgrade.listener.MyUpgradeListener;
import com.ckr.upgrade.util.BuglyConfig;
import com.ckr.upgrade.util.UpgradeLog;
import com.ckr.walle.ChannelUtil;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.tinker.anno.DefaultLifeCycle;
import com.tencent.tinker.entry.DefaultApplicationLike;
import com.tencent.tinker.loader.shareutil.ShareConstants;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static com.ckr.upgrade.util.UpgradeLog.Logd;

/**
 * Created by ckr on 2018/11/27.
 * 配置bugly升级功能及异常上报功能
 */

public class UpgradeApplication extends Application {
    private static UpgradeApplication instance;

    public static UpgradeApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        BuglyConfig.init(this);
    }

}
