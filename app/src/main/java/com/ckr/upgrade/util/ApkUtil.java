package com.ckr.upgrade.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import com.ckr.upgrade.MyApplication;

import java.io.File;

import static com.ckr.upgrade.util.UpgradeLog.Logd;

/**
 * Created by ckr on 2018/11/12.
 */

public class ApkUtil {
    private static final String TAG = "ApkUtil";

    /**
     * 获取apk名
     *
     * @param apkUrl apk链接
     * @return
     */
    @NonNull
    public static String getApkName(String apkUrl) {
        if (TextUtils.isEmpty(apkUrl)) {
            return null;
        }
        int beginIndex = apkUrl.lastIndexOf("/") + 1;
        return apkUrl.substring(beginIndex, apkUrl.length());
    }

    @NonNull
    public static String getApkPath(String apkUrl) {
        String apkName = getApkName(apkUrl);
        if (TextUtils.isEmpty(apkName)) {
            return null;
        }
        return MyApplication.getInstance().getApplication().getExternalFilesDir(null).getAbsolutePath() + File.separator + apkName;
    }

    /**
     * 安装apk
     *
     * @param path apk路径
     */
    public static void installApk(String path) {
        File apkFile = new File(path);
        if (!apkFile.exists()) {
            return;
        }
        try {
            Context context = MyApplication.getInstance().getApplication().getApplicationContext();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = parUri(apkFile, context);
            Logd(TAG, "installApk: uri:" + uri);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Logd(TAG, "installApk: exception");
            e.printStackTrace();
        }

    }

    /**
     * 获取文件Uri
     *
     * @param file
     * @param context
     * @return
     */
    public static Uri parUri(File file, Context context) {
        Uri imageUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Context applicationContext = context.getApplicationContext();
            imageUri = FileProvider.getUriForFile(applicationContext, applicationContext.getPackageName() + ".fileProvider", file);
        } else {
            imageUri = Uri.fromFile(file);
        }
        return imageUri;
    }
}
