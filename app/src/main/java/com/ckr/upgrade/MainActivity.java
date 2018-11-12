package com.ckr.upgrade;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;
import com.tencent.bugly.beta.tinker.TinkerManager;

import static com.ckr.upgrade.util.UpgradeLog.Logd;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private TextView versionView;
    private TextView tinkerIdView;
    private View checkUpgrade;

    public static void start(Context context) {
        Intent starter = new Intent(context, MainActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        versionView = findViewById(R.id.version);
        tinkerIdView = findViewById(R.id.tinkerId);
        checkUpgrade = findViewById(R.id.checkUpgrade);
        checkUpgrade.setOnClickListener(this);

        UpgradeInfo upgradeInfo = Beta.getUpgradeInfo();
        String versionName = null;
        if (upgradeInfo != null) {
            versionName = upgradeInfo.versionName;
            versionView.append(versionName);
        }
        String tinkerId = TinkerManager.getTinkerId();
        Logd(TAG, "onCreate: versionName:" + versionName + ",tinkerId:" + tinkerId);
        tinkerIdView.append(tinkerId);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.checkUpgrade:
                Logd(TAG, "onClick: 检查更新");
                Beta.checkUpgrade(false, false);
                break;
        }
    }
}
