package com.ckr.upgrade;

import android.app.Application;
import android.content.Intent;

import com.ckr.upgrade.util.BuglyConfig;

/**
 * Created by ckr on 2018/11/10.
 */

public class MyApplication extends HotfixApplication {

    public MyApplication(Application application, int tinkerFlags, boolean tinkerLoadVerifyFlag, long applicationStartElapsedTime, long applicationStartMillisTime, Intent tinkerResultIntent) {
        super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BuglyConfig.init(getApplication());
    }


}
