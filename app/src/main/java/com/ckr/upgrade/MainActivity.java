package com.ckr.upgrade;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;
import com.tencent.bugly.beta.tinker.TinkerManager;

import static com.ckr.upgrade.util.UpgradeLog.Logd;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private TextView versionView;
    private TextView tinkerIdView;
    private View checkUpgrade;
    private DownloadReceiver downloadReceiver;

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
        findViewById(R.id.btnService).setOnClickListener(this);
        findViewById(R.id.btnReceiver).setOnClickListener(this);

        UpgradeInfo upgradeInfo = Beta.getUpgradeInfo();
        String versionName = null;
        if (upgradeInfo != null) {
            versionName = upgradeInfo.versionName;
            versionView.append(versionName);
        }
        String tinkerId = TinkerManager.getTinkerId();
        Logd(TAG, "onCreate: versionName:" + versionName + ",tinkerId:" + tinkerId);
        tinkerIdView.append(tinkerId);
//        register();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloadReceiver != null) {
            unregisterReceiver(downloadReceiver);
            downloadReceiver = null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.checkUpgrade:
                Logd(TAG, "onClick: 检查更新");
                Beta.checkUpgrade(false, false);
                break;
            case R.id.btnService:
                startService(new Intent(this, DownLoadService.class));
                break;
            case R.id.btnReceiver:
                register();
                break;
        }
    }

    private void register() {
        downloadReceiver = new DownloadReceiver(this);
        IntentFilter filter = new IntentFilter(DownloadReceiver.DOWNLOAD_RECEIVER);
        registerReceiver(downloadReceiver, filter);
    }
}
