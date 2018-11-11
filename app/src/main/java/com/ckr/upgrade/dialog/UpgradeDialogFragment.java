package com.ckr.upgrade.dialog;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ckr.upgrade.R;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;
import com.tencent.bugly.beta.download.DownloadTask;

import java.math.BigDecimal;

import static com.ckr.upgrade.UpgradeLog.Logd;

/**
 * Created by ckr on 2018/11/11.
 */

public class UpgradeDialogFragment extends BaseDialogFragment {
	private static final String TAG = "BaseDialogFragment";

	private static final String KEY_POSITIVE = "positive";
	private static final String KEY_NEGATIVE = "negative";
	private static final String TYPE_CANCELABLE = "cancelable";
	// 建议升级
	private static final int STRATEGY_OPTIONAL = 1;
	// 强制升级
	private static final int STRATEGY_FORCE = 2;
	// 手工升级
	private static final int STRATEGY_MANUAL = 3;

	private TextView btnOK;

	public void show(@NonNull Activity activity) {
		if (activity instanceof FragmentActivity) {
			show(((FragmentActivity) activity).getSupportFragmentManager(), UpgradeDialogFragment.class.getSimpleName());
		} else {
			show(getFragmentManager(), UpgradeDialogFragment.class.getSimpleName());
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.dialog_upgrade;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		TextView btnCancel = view.findViewById(R.id.btnCancel);
		btnOK = view.findViewById(R.id.btnOK);
		btnOK.setOnClickListener(this);
		btnCancel.setOnClickListener(this);
		TextView titleView = view.findViewById(R.id.titleView);
		TextView msgView = view.findViewById(R.id.msgView);
		TextView versionView = view.findViewById(R.id.versionView);
		TextView sizeView = view.findViewById(R.id.sizeView);

		Bundle bundle = getArguments();
		if (bundle != null) {
			String negative = bundle.getString(KEY_NEGATIVE);
			String positive = bundle.getString(KEY_POSITIVE);
			int cancelableType = bundle.getInt(TYPE_CANCELABLE, DEFAULT);
			setCancelableType(cancelableType);
			btnCancel.setText(negative);
			btnOK.setText(positive);
		}

		UpgradeInfo upgradeInfo = Beta.getUpgradeInfo();
		String title = upgradeInfo.title;
		String versionName = upgradeInfo.versionName;
		String newFeature = upgradeInfo.newFeature;
		int upgradeType = upgradeInfo.upgradeType;
		if (upgradeType == STRATEGY_FORCE) {
			setCancelableType(NO_CANCELED);
		}
		double fileSize = upgradeInfo.fileSize / 1000D / 1000;
		Logd(TAG, "onViewCreated: fileSize:" + upgradeInfo.fileSize);
		BigDecimal decimal = new BigDecimal(fileSize);
		String size = decimal.setScale(2, BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros().toPlainString();

		titleView.setText(title);
		msgView.setText(newFeature);
		versionView.setText("版本：" + versionName);
		sizeView.setText("包大小：" + size + "M");
		versionView.setVisibility(View.GONE);
		sizeView.setVisibility(View.INVISIBLE);

		updateBtn(Beta.getStrategyTask());
	}

	private void updateBtn(DownloadTask strategyTask) {
		int status = strategyTask.getStatus();
		switch (status) {
			case DownloadTask.INIT:
			case DownloadTask.DELETED:
			case DownloadTask.FAILED: {
				btnOK.setText("升级");
			}
			break;
			case DownloadTask.COMPLETE: {
				btnOK.setText("立即安装");
			}
			break;
			case DownloadTask.DOWNLOADING: {
			}
			break;
			case DownloadTask.PAUSED: {
				btnOK.setText("继续下载");
			}
			break;
		}
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);
		switch (v.getId()) {
			case R.id.btnOK:
				DownloadTask downloadTask = Beta.startDownload();
				updateBtn(downloadTask);
				if (Beta.getUpgradeInfo().upgradeType != STRATEGY_FORCE && downloadTask.getStatus() == DownloadTask.DOWNLOADING) {
					Toast.makeText(getContext(), "开始下载", Toast.LENGTH_SHORT).show();
					dismiss();
				}
				break;
			case R.id.btnCancel:
				dismiss();
				break;
		}
	}


	public static class Builder {
		private String negative;
		private String positive;
		private int cancelableType = DEFAULT;

		public Builder() {
		}

		public Builder setNegativeText(String negative) {
			this.negative = negative;
			return this;
		}

		public Builder setPositiveText(String positive) {
			this.positive = positive;
			return this;
		}

		public Builder setCancelableType(@CancelableType int cancelableType) {
			this.cancelableType = cancelableType;
			return this;
		}

		public UpgradeDialogFragment build() {
			UpgradeDialogFragment dialogFragment = new UpgradeDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putString(KEY_POSITIVE, positive);
			bundle.putString(KEY_NEGATIVE, negative);
			bundle.putInt(TYPE_CANCELABLE, cancelableType);
			dialogFragment.setArguments(bundle);
			return dialogFragment;
		}
	}
}
