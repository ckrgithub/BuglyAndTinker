package com.ckr.walle;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import android.util.Log;

import com.ckr.walle.meituan.ChannelInfo;
import com.ckr.walle.meituan.ChannelReader;
import com.ckr.walle.meituan.ChannelWriter;
import com.ckr.walle.meituan.SignatureNotFoundException;

import java.io.File;
import java.io.IOException;

/**
 * Created by ckr on 2018/10/8.
 */

public class ChannelUtil {
    private static final String TAG = "ChannelUtil";
    private static final String DEFAULT_CHANNEL = "dinghuo123";

    /**
     * 获取当前apk的路径
     *
     * @param context
     * @return
     */
    private static String getSourceDir(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        if (applicationInfo == null) {
            return null;
        }
        return applicationInfo.sourceDir;
    }


    /**
     * 获取当前app渠道信息
     *
     * @param context
     * @param
     * @return
     */
    public static String getChannelInfo(Context context) {
        return getChannelInfo(getSourceDir(context));
    }

    /**
     * 获取apk渠道信息
     *
     * @param apkPath apk路径
     * @return
     */
    public static String getChannelInfo(String apkPath) {
        Log.d(TAG, "getChannelInfo: apkPath:" + apkPath);
        String channel = DEFAULT_CHANNEL;
        if (TextUtils.isEmpty(apkPath)) {
            return channel;
        }
        ChannelInfo info = ChannelReader.get(new File(apkPath));
        if (info != null) {
            channel = info.getChannel();
        }
        return channel;
    }

    /**
     * 写入渠道信息
     *
     * @param apkPath     apk路径
     * @param channelInfo 渠道信息
     * @param isReplace   是否覆盖渠道信息
     * @return
     */
    public static boolean setChannelInfo(String apkPath, String channelInfo, boolean isReplace) {
        Log.d(TAG, "setChannelInfo: apkPath:" + apkPath + ",channelInfo:" + channelInfo + ",isReplace:" + isReplace);
        if (TextUtils.isEmpty(apkPath)) {
            return false;
        }
        return setChannelInfo(new File(apkPath), channelInfo, isReplace);
    }

    /**
     * 写入渠道信息
     *
     * @param apkFile     文件
     * @param channelInfo 渠道信息
     * @param isReplace   是否覆盖渠道信息
     * @return
     */
    public static boolean setChannelInfo(File apkFile, String channelInfo, boolean isReplace) {
        Log.d(TAG, "setChannelInfo ,channelInfo:" + channelInfo + ",isReplace:" + isReplace);
        if (apkFile == null) {
            return false;
        }
        try {
            if (isReplace) {
                ChannelWriter.remove(apkFile);
            }
            ChannelWriter.put(apkFile, channelInfo);
            return true;
        } catch (IOException e) {
            Log.d(TAG, "setChannelInfo IOException: ");
            e.printStackTrace();
        } catch (SignatureNotFoundException e) {
            Log.d(TAG, "setChannelInfo SignatureNotFoundException: ");
            e.printStackTrace();
        }
        return false;
    }
}
