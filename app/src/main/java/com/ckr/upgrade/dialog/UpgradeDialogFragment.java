package com.ckr.upgrade.dialog;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ckr.upgrade.R;
import com.ckr.upgrade.UpgradeInfo;
import com.ckr.upgrade.listener.DownloadListener;
import com.ckr.upgrade.util.ApkUtil;
import com.ckr.upgrade.util.DownloadManager;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import static com.ckr.upgrade.util.DownloadManager.COMPLETE;
import static com.ckr.upgrade.util.DownloadManager.FAILED;
import static com.ckr.upgrade.util.DownloadManager.INIT;
import static com.ckr.upgrade.util.DownloadManager.PAUSED;
import static com.ckr.upgrade.util.UpgradeLog.Logd;
import static com.ckr.upgrade.util.UpgradeLog.Loge;

/**
 * Created by ckr on 2018/11/11.
 */

public class UpgradeDialogFragment extends BaseDialogFragment implements DownloadListener {
	private static final String TAG = "BaseDialogFragment";
	private static final long MB = 1024 * 1024;
	private static final int SCALE_VALUE = 2;

	private static final String TYPE_CANCELABLE = "cancelableType";
	// 建议升级
	private static final int STRATEGY_OPTIONAL = 1;
	// 强制升级
	private static final int STRATEGY_FORCE = 2;
	// 手工升级
	private static final int STRATEGY_MANUAL = 3;

	private TextView btnOK;
	private static OnDialogClickListener onDialogClickListener;

	public static UpgradeDialogFragment newInstance(@CancelableType int cancelableType, OnDialogClickListener onDialogClickListener) {
		Bundle args = new Bundle();
		args.putInt(TYPE_CANCELABLE, cancelableType);
		UpgradeDialogFragment fragment = new UpgradeDialogFragment();
		fragment.setArguments(args);
		UpgradeDialogFragment.onDialogClickListener = onDialogClickListener;
		return fragment;
	}

	public void show(@NonNull Activity activity) {
		if (activity instanceof FragmentActivity) {
			//当手机屏幕锁屏后，收到升级通知显示dialog时会报错:
			//java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
			try {
				show(((FragmentActivity) activity).getSupportFragmentManager(), UpgradeDialogFragment.class.getSimpleName());
			} catch (Exception e) {
				e.printStackTrace();
				Loge(TAG, "show: e:" + e.getMessage());
			}
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.dialog_upgrade;
	}

	@Override
	protected void init(Bundle savedInstanceState) {
		DownloadManager.with(this.getContext().getApplicationContext()).registerDownloadListener(this);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		TextView btnCancel = view.findViewById(R.id.btnCancel);
		btnOK = view.findViewById(R.id.btnOK);
		View verticalLine = view.findViewById(R.id.verticalLine);
		TextView titleView = view.findViewById(R.id.titleView);
		TextView msgView = view.findViewById(R.id.msgView);
		TextView versionView = view.findViewById(R.id.versionView);
		TextView sizeView = view.findViewById(R.id.sizeView);
		btnOK.setOnClickListener(this);
		btnCancel.setOnClickListener(this);

		UpgradeInfo upgradeInfo = DownloadManager.with(getContext()).getUpgradeInfo();
		if (upgradeInfo == null) {
			return;
		}
		String title = upgradeInfo.title;
		String versionName = upgradeInfo.versionName;
		String newFeature = upgradeInfo.newFeature;
		int upgradeType = upgradeInfo.upgradeType;
		double fileSize = upgradeInfo.fileSize / (double) MB;
		Logd(TAG, "onViewCreated: fileSize:" + upgradeInfo.fileSize);
		BigDecimal decimal = new BigDecimal(fileSize);
		String size = decimal.setScale(SCALE_VALUE, BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros().toPlainString();

		titleView.setText(title);
		msgView.setText(newFeature);
		versionView.setText(getString(R.string.upgrade_version_info) + versionName);
		sizeView.setText(String.format(getString(R.string.upgrade_apk_size), size));
		versionView.setVisibility(View.GONE);
		sizeView.setVisibility(View.INVISIBLE);

		int cancelableType = DEFAULT;
		Bundle bundle = getArguments();
		if (bundle != null) {
			cancelableType = bundle.getInt(TYPE_CANCELABLE, DEFAULT);
		}
		if (upgradeType == STRATEGY_FORCE) {
			setCancelableType(NO_CANCELED);
			forceStrategyLayout(btnCancel, verticalLine);
		} else {
			setCancelableType(cancelableType);
		}
		updateBtn(getText());
	}

	protected void forceStrategyLayout(TextView btnCancel, View verticalLine) {
		btnCancel.setVisibility(View.GONE);
		verticalLine.setVisibility(View.GONE);
		ViewGroup.LayoutParams params = btnOK.getLayoutParams();
		if (params instanceof ConstraintLayout.LayoutParams) {
			ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) params;
			layoutParams.leftToLeft = R.id.constraintLayout;
			btnOK.setLayoutParams(layoutParams);
		}
		btnOK.setBackgroundResource(R.drawable.selector_dialog_upgrade_button);
	}

	private String getText() {
		int mDownloadStatus = getDownloadStatus();
		String positive = null;
		switch (mDownloadStatus) {
			case INIT:
				positive = getString(R.string.upgrade_status_init);
				break;
			case COMPLETE:
				positive = getString(R.string.upgrade_status_complete);
				break;
			case PAUSED:
				positive = getString(R.string.upgrade_status_pause);
				break;
			case FAILED:
				positive = getString(R.string.upgrade_status_failed);
				break;
		}
		return positive;
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);
		switch (v.getId()) {
			case R.id.btnOK:
				UpgradeInfo upgradeInfo = DownloadManager.with(getContext()).getUpgradeInfo();
				if (upgradeInfo == null) {
					dismiss();
					return;
				}
				if (upgradeInfo.upgradeType == STRATEGY_FORCE) {
				} else {
					dismiss();
				}
				DownloadManager downloadManager = DownloadManager.with(this.getContext().getApplicationContext());
				int downloadStatus = getDownloadStatus();
				Logd(TAG, "onClick: downloadStatus:" + downloadStatus);
				if (downloadStatus == COMPLETE) {
					String apkUrl = upgradeInfo.apkUrl;
					Context context = getContext();
					ApkUtil.installApk(ApkUtil.getApkPath(apkUrl, context), context);
				} else if (downloadStatus == PAUSED) {
					downloadManager.resumeDownload();
				} else if (downloadStatus == FAILED || downloadStatus == INIT) {
					updateBtn(getString(R.string.upgrade_status_download));
					if (onDialogClickListener != null) {
						onDialogClickListener.onPositiveClick();
					} else {
						downloadManager.startDownload();
					}
				}
				break;
			case R.id.btnCancel:
				dismiss();
				break;
		}
	}

	/**
	 * 得到下载状态
	 *
	 * @return
	 */
	private int getDownloadStatus() {
		UpgradeInfo upgradeInfo = DownloadManager.with(getContext()).getUpgradeInfo();
		if (upgradeInfo != null) {
			String apkUrl = upgradeInfo.apkUrl;
			long fileSize = upgradeInfo.fileSize;
			String apkPath = ApkUtil.getApkPath(apkUrl, getContext());
			if (!TextUtils.isEmpty(apkPath)) {
				File file = new File(apkPath);
				Logd(TAG, "getDownloadStatus: len:" + file.length() + ",fileSize:" + fileSize);
				if (file.exists()) {
					if (file.length() == fileSize) {
						return COMPLETE;
					}
				}
			}
		}
		return DownloadManager.with(this.getContext().getApplicationContext()).getDownloadStatus();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		DownloadManager.with(this.getContext().getApplicationContext()).unregisterDownloadListener(this);
	}

	@Override
	public void onReceive(long contentLen, long downloadLen, int progress) {
		updateBtn(progress + getString(R.string.upgrade_progress_symbol));
	}

	@Override
	public void onCompleted(String path) {
		updateBtn(getString(R.string.upgrade_status_complete));
	}

	@Override
	public void onFailed(IOException e) {
		updateBtn(getString(R.string.upgrade_status_failed));
	}

	@Override
	public void onPaused() {
		updateBtn(getString(R.string.upgrade_status_pause));
	}

	private void updateBtn(String text) {
		if (btnOK != null) {
			btnOK.setText(text);
		}
	}

	public static class Builder {
		private int cancelableType = DEFAULT;
		private OnDialogClickListener onDialogClickListener;

		public Builder() {
		}

		public Builder setCancelableType(@CancelableType int cancelableType) {
			this.cancelableType = cancelableType;
			return this;
		}

		public Builder setOnDialogClickListener(OnDialogClickListener onDialogClickListener) {
			this.onDialogClickListener = onDialogClickListener;
			return this;
		}

		public UpgradeDialogFragment build() {
			UpgradeDialogFragment dialogFragment = UpgradeDialogFragment.newInstance(cancelableType, onDialogClickListener);
			return dialogFragment;
		}
	}

	public interface OnDialogClickListener {
		void onPositiveClick();
	}
}
