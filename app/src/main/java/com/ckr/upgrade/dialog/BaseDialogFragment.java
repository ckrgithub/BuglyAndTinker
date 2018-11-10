package com.ckr.upgrade.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ckr.upgrade.R;

/**
 * Created by ckr on 2018/11/10.
 */

public class BaseDialogFragment extends DialogFragment implements View.OnClickListener {
    private static final String KEY_TITLE = "title";
    private static final String KEY_MSG = "msg";
    private static final String KEY_POSITIVE = "positive";
    private static final String KEY_NEGATIVE = "negative";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Base_Dialog_Style);
    }

    public void show(@NonNull Activity activity, String title, String msg, String positive, String negative) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TITLE, title);
        bundle.putString(KEY_MSG, msg);
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
        return inflater.inflate(R.layout.dialog_base, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView btnCancel = view.findViewById(R.id.btnCancel);
        TextView btnOK = view.findViewById(R.id.btnOK);
        View container = view.findViewById(R.id.container);
        container.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        btnOK.setOnClickListener(this);
        TextView titleView = view.findViewById(R.id.titleView);
        TextView msgView = view.findViewById(R.id.msgView);

        Bundle bundle = getArguments();
        if (bundle != null) {
            String negative = bundle.getString(KEY_NEGATIVE);
            String positive = bundle.getString(KEY_POSITIVE);
            String title = bundle.getString(KEY_TITLE);
            String msg = bundle.getString(KEY_MSG);
            titleView.setText(title);
            msgView.setText(msg);
            btnCancel.setText(negative);
            btnOK.setText(positive);
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
            dismiss();
        } else if (id == R.id.btnCancel) {
            dismiss();
        }else if (id == R.id.container) {
            dismiss();
        }
    }
}
