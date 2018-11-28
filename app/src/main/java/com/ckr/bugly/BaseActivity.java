package com.ckr.bugly;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.ckr.bugly.dialog.BaseDialogFragment;
import com.ckr.bugly.dialog.PermissionDialogFragment;
import com.ckr.bugly.permission.PermissionManager;


/**
 * Created by ckr on 2018/9/25.
 */

public abstract class BaseActivity extends AppCompatActivity implements BaseContext {
    private static final String TAG = "BaseActivity";
    private PermissionDialogFragment settingDialog;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.handleRequestPermissionResult(this, requestCode, permissions, grantResults);
    }

    @Nullable
    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onPermissionGranted(int requestCode) {

    }

    @Override
    public void onPermissionPermanentlyDenied(final int requestCode) {
        if (settingDialog == null) {
            settingDialog = new PermissionDialogFragment.Builder()
                    .setTitle("提示")
                    .setMessage("请打开设置开启应用权限")
                    .setPositiveText("设置")
                    .setCancelableType(BaseDialogFragment.NO_CANCELED)
                    .setOnDialogClickListener(new PermissionDialogFragment.OnDialogClickListener() {

                        @Override
                        public void onPositiveClick() {
                            startForResult(requestCode);
                        }

                        @Override
                        public void onNegativeClick() {
                            Toast.makeText(BaseActivity.this, "请在应用管理开启权限", Toast.LENGTH_SHORT).show();
                        }
                    }).build();
        }
        settingDialog.show(this);
    }

    private void startForResult(int requestCode) {
        this.startActivityForResult(
                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", getContext().getPackageName(), null)),
                requestCode);
    }

    @Override
    public void onPermissionDenied(int requestCode) {

    }
}
