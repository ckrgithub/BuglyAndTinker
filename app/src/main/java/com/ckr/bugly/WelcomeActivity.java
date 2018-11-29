package com.ckr.bugly;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import com.ckr.bugly.permission.PermissionManager;
import com.ckr.bugly.util.UpgradeUtil;
import com.tencent.bugly.beta.Beta;

public class WelcomeActivity extends BaseActivity {
    private static final boolean noRequestPermission = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
//        Toast.makeText(this, "这个是热修复后的App", Toast.LENGTH_LONG).show();
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
        UpgradeUtil.checkUpgrade("http://www.baidu.com");
    }
}
