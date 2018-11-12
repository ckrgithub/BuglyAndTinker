package com.ckr.upgrade;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.tencent.bugly.beta.Beta;

public class WelcomeActivity extends AppCompatActivity {
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
