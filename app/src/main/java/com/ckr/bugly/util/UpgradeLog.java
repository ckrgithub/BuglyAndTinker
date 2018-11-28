package com.ckr.bugly.util;

import android.util.Log;

/**
 * Created by ckr on 2018/11/10.
 */

public class UpgradeLog {
    private static final String TAG = "UpgradeLog";
    private static boolean isDebug = false;

    public static void debug(boolean isDebug) {
        UpgradeLog.isDebug = isDebug;
    }

    public static void Logd(String msg) {
        Logd("", msg);
    }

    public static void Logd(String tag, String msg) {
        if (isDebug) {
            Log.d(TAG, tag + "--->" + msg);
        }
    }

    public static void Logi(String msg) {
        Logi("", msg);
    }

    public static void Logi(String tag, String msg) {
        if (isDebug) {
            Log.i(TAG, tag + "--->" + msg);
        }
    }

    public static void Logw(String msg) {
        Logw("", msg);
    }

    public static void Logw(String tag, String msg) {
        if (isDebug) {
            Log.w(TAG, tag + "--->" + msg);
        }
    }

    public static void Loge(String msg) {
        Loge("", msg);
    }

    public static void Loge(String tag, String msg) {
        if (isDebug) {
            Log.e(TAG, tag + "--->" + msg);
        }
    }
}
