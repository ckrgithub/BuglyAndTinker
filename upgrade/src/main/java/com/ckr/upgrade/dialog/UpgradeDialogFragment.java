package com.ckr.upgrade.dialog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ckr.upgrade.DownloadManager;
import com.ckr.upgrade.R;
import com.ckr.upgrade.UpgradeInfo;
import com.ckr.upgrade.listener.OnDownloadListener;
import com.ckr.upgrade.listener.OnInstallApkListener;
import com.ckr.upgrade.util.ApkUtil;
import com.ckr.upgrade.util.UpgradeLog;

import java.io.File;
import java.io.IOException;

import static com.ckr.upgrade.DownloadManager.COMPLETE;
import static com.ckr.upgrade.DownloadManager.FAILED;
import static com.ckr.upgrade.DownloadManager.INIT;
import static com.ckr.upgrade.DownloadManager.PAUSED;

/**
 * Created by ckr on 2018/11/11.
 */

public class UpgradeDialogFragment extends BaseDialogFragment implements OnDownloadListener {
    private static final String TAG = "BaseDialogFragment";
    protected static final String TYPE_CANCELABLE = "cancelableType";
    // 强制升级
    protected static final int STRATEGY_FORCE = 2;

    private TextView btnPositive;
    private static OnDialogClickListener onDialogClickListener;
    private boolean mIsStatedSaved = false;

    public static UpgradeDialogFragment newInstance(@CancelableType int cancelableType, OnDialogClickListener onDialogClickListener) {
        Bundle args = new Bundle();
        args.putInt(TYPE_CANCELABLE, cancelableType);
        UpgradeDialogFragment fragment = new UpgradeDialogFragment();
        fragment.setArguments(args);
        UpgradeDialogFragment.onDialogClickListener = onDialogClickListener;
        return fragment;
    }

    public void showAllowingStateLoss(@NonNull FragmentManager manager, String tag) {
        //当手机屏幕锁屏后，收到升级通知显示dialog时会报错:
        //java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
        if (manager.isStateSaved()) {
            return;
        }
        if (mIsStatedSaved) {
            return;
        }
        show(manager, tag);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mIsStatedSaved = true;
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnInstallApkListener) {
            DownloadManager.with(this.getContext().getApplicationContext()).setOnInstallerListener((OnInstallApkListener) context);
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_upgrade;
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        DownloadManager.with(this.getContext().getApplicationContext()).registerDownloadListener(this);
        TextView btnNegative = rootView.findViewById(R.id.btnNegative);
        btnPositive = rootView.findViewById(R.id.btnPositive);
        View verticalLine = rootView.findViewById(R.id.verticalLine);
        TextView titleView = rootView.findViewById(R.id.titleView);
        TextView msgView = rootView.findViewById(R.id.msgView);
        btnPositive.setOnClickListener(this);
        btnNegative.setOnClickListener(this);
        UpgradeInfo upgradeInfo = DownloadManager.with(getContext()).getUpgradeInfo();
        if (upgradeInfo == null) {
            return;
        }
        String title = upgradeInfo.title;
        String newFeature = upgradeInfo.newFeature;
        UpgradeLog.Logd(TAG, "onViewCreated: title:" + title + ",newFeature:" + newFeature);
        titleView.setText(title);
        msgView.setText(newFeature);

        int upgradeType = upgradeInfo.upgradeType;
        int cancelableType = DEFAULT;
        Bundle bundle = getArguments();
        if (bundle != null) {
            cancelableType = bundle.getInt(TYPE_CANCELABLE, DEFAULT);
        }
        if (upgradeType == STRATEGY_FORCE) {
            setCancelableType(NO_CANCELED);
            forceStrategyLayout(btnNegative, verticalLine);
        } else {
            setCancelableType(cancelableType);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateBtn(getText());
    }

    protected void forceStrategyLayout(TextView btnNegative, View verticalLine) {
        btnNegative.setVisibility(View.GONE);
        verticalLine.setVisibility(View.GONE);
        ViewGroup.LayoutParams params = btnPositive.getLayoutParams();
        if (params instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) params;
            layoutParams.leftToLeft = R.id.constraintLayout;
            btnPositive.setLayoutParams(layoutParams);
        }
        btnPositive.setBackgroundResource(R.drawable.selector_dialog_positive_button2);
    }

    private String getText() {
        String positive = null;

        int mDownloadStatus = getDownloadStatus();
        switch (mDownloadStatus) {
            case INIT:
                positive = getString(R.string.download_status_init);
                break;
            case COMPLETE:
                positive = getString(R.string.download_status_complete);
                break;
            case PAUSED:
                positive = getString(R.string.download_status_pause);
                break;
            case FAILED:
                positive = getString(R.string.download_status_failed);
                break;
        }
        return positive;
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        if (v.getId() == R.id.btnPositive) {
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
            UpgradeLog.Logd(TAG, "onClick: downloadStatus:" + downloadStatus);
            if (downloadStatus == COMPLETE) {
                String apkUrl = upgradeInfo.apkUrl;
                installApk(apkUrl, true);
            } else if (downloadStatus == PAUSED) {
                downloadManager.resumeDownload();
            } else if (downloadStatus == FAILED || downloadStatus == INIT) {
                updateBtn(getString(R.string.download_status_downloading));
                if (onDialogClickListener != null) {
                    onDialogClickListener.onPositiveClick();
                } else {
                    downloadManager.startDownload();
                }
            }
        } else if (v.getId() == R.id.btnNegative) {
            dismiss();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ApkUtil.REQUEST_CODE_INSTALL) {
            Context context = getContext();
            boolean canInstall = ApkUtil.hasInstallPermission(context);
            if (canInstall) {
                DownloadManager with = DownloadManager.with(context);
                UpgradeInfo upgradeInfo = with.getUpgradeInfo();
                if (upgradeInfo != null) {
                    int downloadStatus = with.getDownloadStatus();
                    if (downloadStatus == COMPLETE) {
                        installApk(upgradeInfo.apkUrl, false);
                    }
                }
            }
        }
    }

    /**
     * 适配android O apk安装
     *
     * @param apkUrl
     * @return
     */
    private void installApk(String apkUrl, boolean isOpenSetting) {
        boolean canInstall = ApkUtil.hasInstallPermission(getContext());
        Context context = getContext();
        if (canInstall) {
            ApkUtil.installApk(ApkUtil.getApkPath(apkUrl, context), context, true);
        } else {
            Toast.makeText(context, context.getString(R.string.tips_install_permission), Toast.LENGTH_LONG).show();
            if (isOpenSetting) {
                ApkUtil.openUnknownAppSourcesActivity(this);
            }
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
                UpgradeLog.Logd(TAG, "getDownloadStatus: len:" + file.length() + ",fileSize:" + fileSize);
                if (file.length() >= fileSize) {
                    return COMPLETE;
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
        updateBtn(getString(R.string.download_status_complete));
    }

    @Override
    public void onFailed(IOException e) {
        updateBtn(getString(R.string.download_status_failed));
    }

    @Override
    public void onPaused() {
        updateBtn(getString(R.string.download_status_pause));
    }

    private void updateBtn(String text) {
        if (btnPositive != null) {
            btnPositive.setText(text);
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
