package com.ckr.bugly.util;

import android.text.TextUtils;

import com.ckr.upgrade.UpgradeInfo;
import com.ckr.upgrade.util.OkHttpFactory;
import com.tencent.bugly.beta.Beta;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

import static com.ckr.upgrade.util.UpgradeLog.Logd;

/**
 * Created by ckr on 2018/11/29.
 */

public class UpgradeUtil {
    private static final String TAG = "UpgradeUtil";

    public static final int TYPE_BUGLY = 0;
    public static final int TYPE_OFFICIAL = 1;
    private static int upgradeType = TYPE_BUGLY;

    public static void checkUpgrade(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        //模拟网络数据访问
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = OkHttpFactory.createOkHttp().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logd(TAG, "onFailure: ");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Logd(TAG, "onResponse: response:" + response);
                if (response.isSuccessful()) {
                    if (upgradeType == TYPE_BUGLY) {
                        Logd(TAG, "onResponse: 走bugly升级");
                        //isManual  用户手动点击检查，非用户点击操作请传false
                        //isSilence 是否显示弹窗等交互，[true:没有弹窗和toast] [false:有弹窗或toast]
                        Beta.checkUpgrade(false, true);
                    } else if (upgradeType == TYPE_OFFICIAL) {
                        Logd(TAG, "onResponse: 走服务器升级");
                        UpgradeInfo info = new UpgradeInfo();
                        info.title = "App升级";
                        info.newFeature = "修复bug\n快来看看";
                        info.upgradeType = 1;
                        info.versionCode = 120;
                        info.versionName = "1.2.0";
                        info.apkUrl = "https://github.com/ckrgithub/BuglyAndTinker/blob/master/apk/Upgrade-debug-v1.0.1.101.apk";
                        info.fileSize = 2706 * 1024;
                        info.popTimes = 0;
                        info.popInterval = 0;
                    }
                }
            }
        });
    }
}
