package com.zhgqthomas.rxjava;

import android.app.Application;

import com.zhgqthomas.rxjava.util.LogUtils;

public class App extends Application {

    private static App mInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        // 开启 Log debug 模式
        LogUtils.syncIsDebug(getApplicationContext());

    }

    public static App getInstance() {
        return mInstance;
    }
}
