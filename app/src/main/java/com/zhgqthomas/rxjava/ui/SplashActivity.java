package com.zhgqthomas.rxjava.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.zhgqthomas.rxjava.ui.base.BaseActivity;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivity(MainActivity.getStartIntent(this));
        finish();
    }
}
