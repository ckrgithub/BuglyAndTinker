package com.ckr.upgrade;

/**
 * Created by ckr on 2018/11/17.
 */

public class UpgradeInfo {
    public String id;//唯一标识
    public String title;//升级提示标题
    public String newFeature;//升级说明
    public long publishTime;//升级发布时间,ms
    public int publishType;//发布类型 0测试 1正式
    public int upgradeType = 1;//升级策略 1建议 2强制 3手工
    public int popTimes;//提醒次数
    public long popInterval;//提醒间隔
    public int versionCode;
    public String versionName;
    public String apkMd5;//包md5值
    public String apkUrl;//APK下载地址
    public long fileSize;//APK文件的大小
    public String imageUrl;// 图片url

}
