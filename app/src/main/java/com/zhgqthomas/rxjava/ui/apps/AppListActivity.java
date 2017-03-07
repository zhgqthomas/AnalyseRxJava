package com.zhgqthomas.rxjava.ui.apps;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

import com.zhgqthomas.rxjava.App;
import com.zhgqthomas.rxjava.R;
import com.zhgqthomas.rxjava.entity.AppInfo;
import com.zhgqthomas.rxjava.entity.AppInfoRich;
import com.zhgqthomas.rxjava.ui.base.BaseActivity;
import com.zhgqthomas.rxjava.util.BitmapUtils;
import com.zhgqthomas.rxjava.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


public class AppListActivity extends BaseActivity {

    @BindView(R.id.apps_recycler_view)
    RecyclerView mAppsRecyclerView;
    @BindView(R.id.apps_swipe_refresh)
    SwipeRefreshLayout mAppsRefreshLayout;

    private AppListAdapter mAppsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        mAppsAdapter = new AppListAdapter(this, new ArrayList<AppInfo>());
        mAppsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mAppsRecyclerView.setHasFixedSize(true);
        mAppsRecyclerView.setAdapter(mAppsAdapter);

        mAppsRefreshLayout.setColorSchemeColors(
                ResourcesCompat.getColor(getResources(), R.color.colorAccent, null));
        mAppsRefreshLayout.setProgressViewOffset(false, 0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24,
                        getResources().getDisplayMetrics()));
        mAppsRefreshLayout.setEnabled(false);
        mAppsRefreshLayout.setRefreshing(true);
        mAppsRecyclerView.setVisibility(View.GONE);

        refreshAppList();
    }

    private void refreshAppList() {
        getApps().toSortedList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<AppInfo>>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.SHORT.show(getBaseContext(), "Something went wrong!");
                        mAppsRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onNext(List<AppInfo> appInfos) {
                        mAppsRecyclerView.setVisibility(View.VISIBLE);
                        mAppsAdapter.setData(appInfos);
                        mAppsRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private Observable<AppInfo> getApps() {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

         return Observable.from(this.getPackageManager().queryIntentActivities(mainIntent, 0))
                .map(new Func1<ResolveInfo, AppInfoRich>() {
                    @Override
                    public AppInfoRich call(ResolveInfo info) {
                        return new AppInfoRich(AppListActivity.this, info);
                    }
                })
                .map(new Func1<AppInfoRich, AppInfo>() {
                    @Override
                    public AppInfo call(AppInfoRich appInfoRich) {
                        Bitmap icon = BitmapUtils.drawableToBitmap(appInfoRich.getIcon());
                        String name = appInfoRich.getName();
                        String iconPath = App.getInstance().getFilesDir() + "/" + name;
                        BitmapUtils.storeBitmap(App.getInstance(), icon, name);
                        return new AppInfo(name, iconPath, appInfoRich.getLastUpdateTime());
                    }
                });
    }
}
