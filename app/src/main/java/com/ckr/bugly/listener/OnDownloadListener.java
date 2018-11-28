package com.ckr.bugly.listener;

import java.io.IOException;

/**
 * Created by ckr on 2018/11/13.
 */

public interface OnDownloadListener {

    void onReceive(long contentLen, long downloadLen, int progress);

    void onCompleted(String path);

    void onFailed(IOException e);

    void onPaused();
}
