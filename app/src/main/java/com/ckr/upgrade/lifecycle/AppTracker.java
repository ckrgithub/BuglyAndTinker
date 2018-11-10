package com.ckr.upgrade.lifecycle;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/**
 * Created by ckr on 2018/11/10.
 */

public class AppTracker implements Application.ActivityLifecycleCallbacks {
    private static final String TAG="AppTracker";
    
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
