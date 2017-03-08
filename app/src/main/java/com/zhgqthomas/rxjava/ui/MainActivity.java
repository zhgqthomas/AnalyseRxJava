package com.zhgqthomas.rxjava.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;

import com.zhgqthomas.rxjava.R;
import com.zhgqthomas.rxjava.ui.apps.AppListActivity;
import com.zhgqthomas.rxjava.ui.base.BaseActivity;

import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    public static Intent getStartIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.tool_bar));
    }

    @OnClick(R.id.operator)
    void operatorClicked() {
        startActivity(AppListActivity.getStartIntent(this));
    }
}
