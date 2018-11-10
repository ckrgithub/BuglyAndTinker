package com.ckr.upgrade.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ckr.upgrade.R;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;
import com.tencent.bugly.beta.download.DownloadTask;

import java.math.BigDecimal;

import static com.ckr.upgrade.UpgradeLog.Logd;

/**
 * Created by ckr on 2018/11/10.
 */

public class BaseDialogFragment extends DialogFragment implements View.OnClickListener, DialogInterface.OnKeyListener {
    private static final String TAG = "BaseDialogFragment";

    private static final String KEY_POSITIVE = "positive";
    private static final String KEY_NEGATIVE = "negative";
    // 建议
    private static final int STRATEGY_OPTIONAL = 1;
    // 强制
    private static final int STRATEGY_FORCE = 2;
    // 手工
    private static final int STRATEGY_MANUAL = 3;

    private TextView btnOK;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Base_Dialog_Style);
    }

    public void show(@NonNull Activity activity, String positive, String negative) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_POSITIVE, positive);
        bundle.putString(KEY_NEGATIVE, negative);
        setArguments(bundle);
        if (activity instanceof FragmentActivity) {
            show(((FragmentActivity) activity).getSupportFragmentManager(), BaseDialogFragment.class.getSimpleName());
        } else {
            show(getFragmentManager(), BaseDialogFragment.class.getSimpleName());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().setOnKeyListener(this);
        return inflater.inflate(R.layout.dialog_base, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView btnCancel = view.findViewById(R.id.btnCancel);
        btnOK = view.findViewById(R.id.btnOK);
        View container = view.findViewById(R.id.container);
        container.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        btnOK.setOnClickListener(this);
        TextView titleView = view.findViewById(R.id.titleView);
        TextView msgView = view.findViewById(R.id.msgView);
        TextView versionView = view.findViewById(R.id.versionView);
        TextView sizeView = view.findViewById(R.id.sizeView);

        Bundle bundle = getArguments();
        if (bundle != null) {
            String negative = bundle.getString(KEY_NEGATIVE);
            String positive = bundle.getString(KEY_POSITIVE);
            btnCancel.setText(negative);
            btnOK.setText(positive);
        }

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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnOK) {
            DownloadTask downloadTask = Beta.startDownload();
            updateBtn(downloadTask);
            if (Beta.getUpgradeInfo().upgradeType != STRATEGY_FORCE && downloadTask.getStatus() == DownloadTask.DOWNLOADING) {
                Toast.makeText(getContext(), "开始下载", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        } else if (id == R.id.btnCancel) {
            dismiss();
        } else if (id == R.id.container) {
//            dismiss();
        }
    }


    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return false;
    }
}
