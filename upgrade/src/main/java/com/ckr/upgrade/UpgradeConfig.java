package com.ckr.upgrade;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ckr on 2018/11/27.
 */

public class UpgradeConfig {
    private static final int CAPACITY = 8;
    static int smallIconId = -1;
    public static boolean isDebug = false;//是否是debug模式
    public static boolean isAutoInstall = true;//是否自动安装
    public static boolean enableNotification = true;//是否发送通知
    public static boolean enableWriteChannelInfo = true;//是否写入渠道
    public static List<Class<? extends Activity>> canShowUpgradeActs = new ArrayList<>(CAPACITY);


    String buglyId = null;//buglyId
    String appVersion = null;//app版本
    String appChannel = null;//app渠道
    int notificationIconId = -1;//通知栏小图标id

    public UpgradeConfig(String buglyId, String appVersion, String appChannel, int notificationIconId) {
        this.buglyId = buglyId;
        this.appVersion = appVersion;
        this.appChannel = appChannel;
        this.notificationIconId = notificationIconId;
    }

}
