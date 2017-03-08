package com.zhgqthomas.rxjava.ui.apps;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.zhgqthomas.rxjava.App;
import com.zhgqthomas.rxjava.R;
import com.zhgqthomas.rxjava.entity.AppInfo;
import com.zhgqthomas.rxjava.entity.AppInfoRich;
import com.zhgqthomas.rxjava.ui.base.BaseActivity;
import com.zhgqthomas.rxjava.util.BitmapUtils;
import com.zhgqthomas.rxjava.util.CollectionUtils;
import com.zhgqthomas.rxjava.util.LogUtils;
import com.zhgqthomas.rxjava.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


public class AppListActivity extends BaseActivity {

    private static final String TAG = LogUtils.makeLogTag(AppListActivity.class);

    @BindView(R.id.apps_recycler_view)
    RecyclerView mAppsRecyclerView;
    @BindView(R.id.apps_swipe_refresh)
    SwipeRefreshLayout mAppsRefreshLayout;

    private AppListAdapter mAppsAdapter;
    private List<AppInfo> mAppInfos = new ArrayList<>();
    private Subscription mInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        setSupportActionBar((Toolbar) findViewById(R.id.tool_bar));

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
        mAppsRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshAppList();
            }
        });
        mAppsRefreshLayout.setRefreshing(true);
        mAppsRecyclerView.setVisibility(View.GONE);

        refreshAppList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.just:
                performJust();
                return true;
            case R.id.repeat:
                performRepeat();
                return true;
            case R.id.defer:
                performDefer();
                return true;
            case R.id.range:
                performRange();
                return true;
            case R.id.interval:
                performInterval();
                return true;
            default:
                return false;
        }
    }

    private void performInterval() { // 每 2 秒显示一个 AppInfo
        mAppsAdapter.clear();
        mInterval = Observable.interval(2, TimeUnit.SECONDS) // interval 默认是在 work 线程
                .map(new Func1<Long, AppInfo>() {
                    @Override
                    public AppInfo call(Long index) {
                        if (index.intValue() < 5) {
                            if (mInterval.isUnsubscribed()) {
                                mInterval.unsubscribe();
                            }
                        }
                        return mAppInfos.get(index.intValue());
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performRange() { // 获取指定范围的 AppInfo
        mAppsAdapter.clear();
        Observable.range(4, 4)
                .map(new Func1<Integer, AppInfo>() {
                    @Override
                    public AppInfo call(Integer integer) {
                        return mAppInfos.get(integer);
                    }
                })
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performDefer() {
        Observable<AppInfo> observable = Observable.defer(new Func0<Observable<AppInfo>>() {
            @Override
            public Observable<AppInfo> call() {
                return Observable.just(mAppInfos.get(0), mAppInfos.get(1), mAppInfos.get(2));
            }
        });

        // 改变 List 中的顺序
        CollectionUtils.reverseList(mAppInfos);
        mAppsAdapter.clear();

        // defer 操作符只有在被订阅的时候才会执行 List.get 操作
        observable.subscribe(new Action1<AppInfo>() {
            @Override
            public void call(AppInfo appInfo) {
                mAppsAdapter.add(appInfo);
            }
        });
    }

    private void performRepeat() {
        Observable.just(mAppInfos.get(0), mAppInfos.get(1), mAppInfos.get(2))
                .repeat(3)
                .toSortedList()
                .subscribe(new Action1<List<AppInfo>>() {
                    @Override
                    public void call(List<AppInfo> appInfos) {
                        mAppsAdapter.clear();
                        mAppsAdapter.setData(appInfos);
                    }
                });
    }

    private void performJust() {
        Observable.just(mAppInfos.get(0), mAppInfos.get(1), mAppInfos.get(2))
                .toSortedList()
                .subscribe(new Action1<List<AppInfo>>() {
                    @Override
                    public void call(List<AppInfo> appInfos) {
                        mAppsAdapter.clear();
                        mAppsAdapter.setData(appInfos);
                    }
                });
    }

    private void refreshAppList() {
        getApps().toSortedList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<AppInfo>>() {
                    @Override
                    public void onCompleted() {
                    }

                    public void onError(Throwable e) {
                        ToastUtils.SHORT.show(getBaseContext(), "Something went wrong!");
                        mAppsRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onNext(List<AppInfo> appInfos) {
                        mAppInfos.addAll(appInfos);
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
                .map(new Func1<ResolveInfo, AppInfo>() {
                    @Override
                    public AppInfo call(ResolveInfo info) {
                        AppInfoRich appInfoRich = new AppInfoRich(AppListActivity.this, info);
                        Bitmap icon = BitmapUtils.drawableToBitmap(appInfoRich.getIcon());
                        String name = appInfoRich.getName();
                        String iconPath = App.getInstance().getFilesDir() + "/" + name;
                        BitmapUtils.storeBitmap(App.getInstance(), icon, name);
                        return new AppInfo(name, iconPath, appInfoRich.getLastUpdateTime());
                    }
                });
    }
}
