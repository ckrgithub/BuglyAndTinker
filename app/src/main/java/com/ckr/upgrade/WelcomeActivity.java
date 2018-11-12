package com.ckr.upgrade;

import android.Manifest;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.ckr.upgrade.permission.PermissionManager;
import com.tencent.bugly.beta.Beta;

public class WelcomeActivity extends BaseActivity {
    public static final int TYPE_BUGLY = 0;
    public static final int TYPE_OFFICIAL = 1;
    private int upgradeType = TYPE_BUGLY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        findViewById(R.id.welcome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(WelcomeActivity.this, "客官，莫急", Toast.LENGTH_SHORT).show();
//                MainActivity.start(WelcomeActivity.this);
            }
        });
        PermissionManager.requestPermission(this, PermissionManager.REQUEST_CODE_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
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
            Beta.checkUpgrade(false, false);
        } else if (upgradeType == TYPE_OFFICIAL) {

        }
    }
}
