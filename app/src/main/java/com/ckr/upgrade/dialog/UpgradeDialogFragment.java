package com.ckr.upgrade.dialog;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.ckr.upgrade.DownLoadService;
import com.ckr.upgrade.R;
import com.ckr.upgrade.listener.MyDownloadListener;
import com.ckr.upgrade.util.ApkUtil;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;
import com.tencent.bugly.beta.download.DownloadTask;

import java.io.File;
import java.math.BigDecimal;

import static com.ckr.upgrade.util.UpgradeLog.Logd;

/**
 * Created by ckr on 2018/11/11.
 */

public class UpgradeDialogFragment extends BaseDialogFragment{
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
    private String positive = "立即更新";
    private static OnDialogClickListener onDialogClickListener;

    public static UpgradeDialogFragment newInstance(String positive, String negative, @CancelableType int cancelableType, OnDialogClickListener onDialogClickListener) {
        Bundle args = new Bundle();
        args.putString(KEY_POSITIVE, positive);
        args.putString(KEY_NEGATIVE, negative);
        args.putInt(TYPE_CANCELABLE, cancelableType);
        UpgradeDialogFragment fragment = new UpgradeDialogFragment();
        fragment.setArguments(args);
        UpgradeDialogFragment.onDialogClickListener = onDialogClickListener;
        return fragment;
    }

    public void show(@NonNull Activity activity) {
        if (activity instanceof FragmentActivity) {
            show(((FragmentActivity) activity).getSupportFragmentManager(), UpgradeDialogFragment.class.getSimpleName());
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_upgrade;
    }

    @Override
    protected void init(Bundle savedInstanceState) {
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

        UpgradeInfo upgradeInfo = Beta.getUpgradeInfo();
        String title = upgradeInfo.title;
        String versionName = upgradeInfo.versionName;
        String newFeature = upgradeInfo.newFeature;
        int upgradeType = upgradeInfo.upgradeType;
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

        Bundle bundle = getArguments();
        if (bundle != null) {
            String negative = bundle.getString(KEY_NEGATIVE);
            positive = bundle.getString(KEY_POSITIVE);
            int cancelableType = bundle.getInt(TYPE_CANCELABLE, DEFAULT);
            if (upgradeType == STRATEGY_FORCE) {
                Logd(TAG, "onViewCreated: ");
                setCancelableType(NO_CANCELED);
                btnCancel.setVisibility(View.GONE);
                verticalLine.setVisibility(View.GONE);
                ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) btnOK.getLayoutParams();
                layoutParams.leftToLeft = R.id.constraintLayout;
                btnOK.setLayoutParams(layoutParams);
                btnOK.setBackgroundResource(R.drawable.selector_dialog_upgrade_button);
            } else {
                setCancelableType(cancelableType);
                btnCancel.setText(negative);
            }
        }
        int apkDownloadStatus = getApkDownloadStatus();
        switch (apkDownloadStatus){
            case DownLoadService.INIT:
                break;
            case DownLoadService.COMPLETE:
                positive="立即安装";
                break;
            case DownLoadService.PAUSED:
                positive="继续下载";
                break;
            case DownLoadService.FAILED:
                break;
        }
        btnOK.setText(positive);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.btnOK:
//                dismiss();
                if (onDialogClickListener != null) {
                    if (getApkDownloadStatus() == DownLoadService.COMPLETE) return;
                    onDialogClickListener.onPositiveClick();
                }
                break;
            case R.id.btnCancel:
                dismiss();
                break;
        }
    }

    /**
     * apk是否已经下载
     *
     * @return
     */
    private int getApkDownloadStatus() {
        UpgradeInfo upgradeInfo = Beta.getUpgradeInfo();
        if (upgradeInfo != null) {
            String apkUrl = upgradeInfo.apkUrl;
            long fileSize = upgradeInfo.fileSize;
            String apkPath = ApkUtil.getApkPath(apkUrl);
            if (!TextUtils.isEmpty(apkPath)) {
                File file = new File(apkPath);
                Logd(TAG, "getApkDownloadStatus: len:" + file.length() + ",fileSize:" + fileSize);
                if (file.exists()) {
                    if (file.length() == fileSize) {
                        return DownLoadService.COMPLETE;
                    } else {
                        return DownLoadService.DELETED;
                    }
                }
            }
        }
        return DownLoadService.INIT;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public static class Builder {
        private String negative;
        private String positive;
        private int cancelableType = DEFAULT;
        private OnDialogClickListener onDialogClickListener;

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

        public Builder setOnDialogClickListener(OnDialogClickListener onDialogClickListener) {
            this.onDialogClickListener = onDialogClickListener;
            return this;
        }

        public UpgradeDialogFragment build() {
            UpgradeDialogFragment dialogFragment = UpgradeDialogFragment.newInstance(positive, negative, cancelableType, onDialogClickListener);
            return dialogFragment;
        }
    }

    public interface OnDialogClickListener {
        void onPositiveClick();
    }
}
