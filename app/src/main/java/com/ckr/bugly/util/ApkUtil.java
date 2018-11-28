package com.ckr.bugly.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import com.ckr.bugly.listener.OnInstallApkListener;

import java.io.File;

import static com.ckr.bugly.util.UpgradeLog.Logd;

/**
 * Created by ckr on 2018/11/12.
 */

public class ApkUtil {
    private static final String TAG = "ApkUtil";
    public static final int REQUEST_CODE_INSTALL = 1129;

    /**
     * 获取apk名
     *
     * @param apkUrl apk下载链接
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

    /**
     * 获取apk存储路径
     *
     * @param apkUrl  apk下载路径
     * @param context
     * @return
     */
    @NonNull
    public static String getApkPath(String apkUrl, @NonNull Context context) {
        String apkName = getApkName(apkUrl);
        if (TextUtils.isEmpty(apkName)) {
            return null;
        }
        return context.getExternalFilesDir(null).getAbsolutePath() + File.separator + apkName;
    }

    /**
     * 安装apk
     *
     * @param path apk路径
     */
    public static boolean installApk(String path, @NonNull Context context) {
        Logd(TAG, "installApk: path:" + path);
        File apkFile = new File(path);
        if (!apkFile.exists()) {
            return false;
        }
        boolean canInstall = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            canInstall = context.getPackageManager().canRequestPackageInstalls();
        }
        if (!canInstall) {
            OnInstallApkListener onInstallerListener = DownloadManager.with(context).getOnInstallerListener();
            if (onInstallerListener != null) {
                onInstallerListener.requestInstallPermission();
            }
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = parUri(apkFile, context);
            Logd(TAG, "installApk: uri:" + uri);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Logd(TAG, "installApk: exception");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取文件Uri
     *
     * @param file
     * @param context
     * @return
     */
    public static Uri parUri(File file, @NonNull Context context) {
        Uri imageUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            imageUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", file);
        } else {
            imageUri = Uri.fromFile(file);
        }
        return imageUri;
    }

    /**
     * 是否有安装未知来源的应用程序权限
     *
     * @return
     */
    public static boolean hasInstallPermission(@NonNull Context context) {
        boolean canInstall = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            canInstall = context.getPackageManager().canRequestPackageInstalls();
        }
        return canInstall;
    }


    /**
     * 打开设置页
     */
    public static void openUnknownAppSourcesActivity(@NonNull Object obj) {
        if (obj instanceof Activity) {
            Activity activity = ((Activity) obj);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivityForResult(intent, REQUEST_CODE_INSTALL);
            }
        } else if (obj instanceof Fragment) {
            Fragment fragment = ((Fragment) obj);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                Uri uri = Uri.fromParts("package", fragment.getContext().getPackageName(), null);
                intent.setData(uri);
                fragment.startActivityForResult(intent, REQUEST_CODE_INSTALL);
            }
        } else if (obj instanceof android.app.Fragment) {
            android.app.Fragment fragment = ((android.app.Fragment) obj);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                Uri uri = Uri.fromParts("package", fragment.getContext().getPackageName(), null);
                intent.setData(uri);
                fragment.startActivityForResult(intent, REQUEST_CODE_INSTALL);
            }
        }
    }
}
