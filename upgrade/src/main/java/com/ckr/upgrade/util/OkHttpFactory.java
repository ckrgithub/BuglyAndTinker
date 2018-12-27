package com.ckr.upgrade.util;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Created by ckr on 2018/11/12.
 */

public class OkHttpFactory {
    private static OkHttpClient client;

    public static OkHttpClient createOkHttp() {
        if (client == null) {
            synchronized (OkHttpFactory.class) {
                if (client == null) {
                    OkHttpClient.Builder builder = new OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(15, TimeUnit.SECONDS)
                            .writeTimeout(15, TimeUnit.SECONDS)
                            .sslSocketFactory(SSLSocketClient.getSSLSocketFactory())
                            .hostnameVerifier(SSLSocketClient.getHostnameVerifier())
                            .retryOnConnectionFailure(true);
                    client = builder.build();
                }
            }
        }
        return client;
    }
}
