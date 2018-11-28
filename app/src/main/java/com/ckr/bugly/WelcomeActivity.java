package com.ckr.bugly;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import com.ckr.bugly.permission.PermissionManager;
import com.ckr.upgrade.util.UpgradeLog;
import com.tencent.bugly.beta.Beta;

public class WelcomeActivity extends BaseActivity {
    public static final int TYPE_BUGLY = 0;
    public static final int TYPE_OFFICIAL = 1;
    private int upgradeType = TYPE_BUGLY;
    private static final boolean noRequestPermission = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        UpgradeLog.debug(BuildConfig.IS_DEBUG);
        findViewById(R.id.welcome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(WelcomeActivity.this, "客官，莫急", Toast.LENGTH_SHORT).show();
            }
        });
        if (noRequestPermission) {
            onPermissionGranted(-1);
        } else {
            PermissionManager.requestPermission(this, PermissionManager.REQUEST_CODE_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionManager.REQUEST_CODE_STORAGE) {
            PermissionManager.requestPermission(this, PermissionManager.REQUEST_CODE_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onPermissionGranted(int requestCode) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MainActivity.start(WelcomeActivity.this);
                finish();
            }
        }, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (upgradeType == TYPE_BUGLY) {
            //isManual  用户手动点击检查，非用户点击操作请传false
            //isSilence 是否显示弹窗等交互，[true:没有弹窗和toast] [false:有弹窗或toast]
            Beta.checkUpgrade(false, false);
        } else if (upgradeType == TYPE_OFFICIAL) {

        }
    }
}
