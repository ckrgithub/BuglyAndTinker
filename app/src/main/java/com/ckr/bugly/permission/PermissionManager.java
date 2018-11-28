package com.ckr.bugly.permission;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

import com.ckr.bugly.BaseContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2018/10/27
 *
 * @author ckr
 */

public class PermissionManager {
    private static final String TAG = "PermissionManager";
    public static final int REQUEST_CODE_CALENDAR = 100;
    public static final int REQUEST_CODE_CAMERA = 101;
    public static final int REQUEST_CODE_CONTACTS = 102;
    public static final int REQUEST_CODE_LOCATION = 103;
    public static final int REQUEST_CODE_RECORD = 104;
    public static final int REQUEST_CODE_PHONE = 105;
    public static final int REQUEST_CODE_STORAGE = 106;

    /**
     * 是否有已授权权限
     *
     * @param context
     * @param group
     * @return
     */
    public static boolean hasPermissionGranted(@NonNull final BaseContext context, @NonNull String... group) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        boolean isGranted = true;
        for (String s : group) {
            if (ContextCompat.checkSelfPermission(context.getContext(), s) != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;
                break;
            }
        }
        return isGranted;
    }

    /**
     * 请求权限
     *
     * @param context
     * @param requestCode
     * @param group
     */
    public static void requestPermission(@NonNull final BaseContext context, int requestCode, @NonNull String... group) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            context.onPermissionGranted(requestCode);
            return;
        }
        boolean isGranted = true;
        for (String s : group) {
            if (ContextCompat.checkSelfPermission(context.getContext(), s) != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;
                break;
            }
        }
        if (!isGranted) {
            executePermissionRequest(context, requestCode, group);
        } else {
            context.onPermissionGranted(requestCode);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static void executePermissionRequest(@NonNull BaseContext context, int requestCode, @NonNull String[] group) {
        if (context instanceof Activity) {
            ActivityCompat.requestPermissions((Activity) context, group, requestCode);
        } else if (context instanceof Fragment) {
            ((Fragment) context).requestPermissions(group, requestCode);
        } else if (context instanceof android.app.Fragment) {
            ((android.app.Fragment) context).requestPermissions(group, requestCode);
        }
    }

    /**
     * 处理请求权限结果
     *
     * @param context
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public static void handleRequestPermissionResult(@NonNull BaseContext context, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        List<String> denied = null;
        if (grantResults.length > 0) {
            denied = new ArrayList<>(grantResults.length);
            for (int i = 0; i < grantResults.length; i++) {
                String perm = permissions[i];
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    denied.add(perm);
                }
            }
        }
        if (denied != null && !denied.isEmpty()) {
            boolean b = hasPermissionPermanentlyDenied(context, denied.toArray(new String[denied.size()]));
            if (b) {
                context.onPermissionPermanentlyDenied(requestCode);
            } else {
                context.onPermissionDenied(requestCode);
            }
        } else {
            context.onPermissionGranted(requestCode);
        }
    }

    /**
     * 所有的权限被拒接，并不再提示
     *
     * @param context
     * @param perms
     * @return
     */
    @TargetApi(Build.VERSION_CODES.M)
    private static boolean hasPermissionPermanentlyDenied(@NonNull BaseContext context, @NonNull String[] perms) {
        boolean isPermanentlyDenied = true;
        if (context instanceof Activity) {
            for (String perm : perms) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, perm)) {
                    isPermanentlyDenied = false;
                    break;
                }
            }
        } else if (context instanceof Fragment) {
            for (String perm : perms) {
                if (((Fragment) context).shouldShowRequestPermissionRationale(perm)) {
                    isPermanentlyDenied = false;
                    break;
                }
            }
        } else if (context instanceof android.app.Fragment) {
            for (String perm : perms) {
                if (((android.app.Fragment) context).shouldShowRequestPermissionRationale(perm)) {
                    isPermanentlyDenied = false;
                    break;
                }
            }
        }
        return isPermanentlyDenied;
    }
}
