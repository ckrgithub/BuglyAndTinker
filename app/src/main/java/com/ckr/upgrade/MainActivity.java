package com.ckr.upgrade;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;
import com.tencent.bugly.beta.tinker.TinkerManager;

import static com.ckr.upgrade.UpgradeLog.Logd;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";

	private TextView versionView;
	private TextView tinkerIdView;

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
}
