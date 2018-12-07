package com.ckr.upgrade.util;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import static com.ckr.upgrade.util.UpgradeLog.Logd;

/**
 * Created by ckr on 2018/12/7.
 */

public class ClearApkService extends IntentService {
    private static final String TAG = "ClearApkService";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public ClearApkService() {
        super("ClearApkService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Logd(TAG, "onHandleIntent: ");
        ApkUtil.clearApk(this);
    }
}
