package com.zhgqthomas.rxjava.ui.apps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.zhgqthomas.rxjava.util.LogUtils;
import com.zhgqthomas.rxjava.util.ToastUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;

public class TransformObservablesActivity extends BaseActivity {

    private static final String TAG = LogUtils.makeLogTag(TransformObservablesActivity.class);

    @BindView(R.id.apps_recycler_view)
    RecyclerView mAppsRecycler;
    @BindView(R.id.apps_swipe_refresh)
    SwipeRefreshLayout mSwipeLayout;

    private AppListAdapter mAppAdapter;
    private List<AppInfo> mApps = new ArrayList<>();

    public static Intent getStartIntent(Context context) {
        return new Intent(context, TransformObservablesActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        setSupportActionBar((Toolbar) findViewById(R.id.tool_bar));
        if (null != getSupportActionBar()) {
            getSupportActionBar().setTitle(R.string.transform_observable);
        }

        mAppAdapter = new AppListAdapter(this, new ArrayList<AppInfo>());
        mAppsRecycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mAppsRecycler.setAdapter(mAppAdapter);
        mAppsRecycler.setHasFixedSize(true);

        mSwipeLayout.setColorSchemeColors(
                ResourcesCompat.getColor(getResources(), R.color.colorAccent, null));
        mSwipeLayout.setProgressViewOffset(false, 0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24,
                        getResources().getDisplayMetrics()));
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshAppList();
            }
        });
        mSwipeLayout.setRefreshing(true);
        mAppsRecycler.setVisibility(View.GONE);

        refreshAppList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.transform, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.map:
                performMap();
                return true;
            case R.id.flat_map:
                performFlatMap();
                return true;

            case R.id.concat_map:
                performConcatMap();
                return true;

            case R.id.switch_map:
                performSwitchMap();
                return true;

            case R.id.scan:
                performScan();
                return true;

            case R.id.group_by:
                performGroupBy();
                return true;

            case R.id.buffer:
                performBuffer();
                return true;

            case R.id.window:
                performWindow();
                return true;
            default:
                return false;
        }
    }

    private void performWindow() {
        mAppAdapter.clear();

        getApps().take(8)
                .doOnNext(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppAdapter.add(appInfo);
                    }
                })
                .flatMap(new Func1<AppInfo, Observable<String>>() {
                    @Override
                    public Observable<String> call(AppInfo appInfo) {
                        long time = (long) (Math.random() * 3 + 0.5);
                        LogUtils.d(TAG, "app: " + appInfo.getName() + " time: " + time);
                        return Observable.just(appInfo.getName())
                                .delay(time, TimeUnit.SECONDS);
                    }
                })
                .window(2, TimeUnit.SECONDS)
                .subscribe(new Action1<Observable<String>>() {
                    @Override
                    public void call(Observable<String> result) { // 与 buffer 的不同在于返回的是个 Observable
                        result.toList()
                            .subscribe(new Action1<List<String>>() {
                                @Override
                                public void call(List<String> strings) {
                                    LogUtils.d(TAG, "values: " + Arrays.toString(strings.toArray()));
                                }
                            });
                    }
                });
    }

    private void performBuffer() {
        mAppAdapter.clear();

        getApps().take(8)
                .doOnNext(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppAdapter.add(appInfo);
                    }
                })
                .flatMap(new Func1<AppInfo, Observable<String>>() {
                    @Override
                    public Observable<String> call(AppInfo appInfo) {
                        long time = (long) (Math.random() * 3 + 0.5);
                        LogUtils.d(TAG, "app: " + appInfo.getName() + " time: " + time);
                        return Observable.just(appInfo.getName())
                                .delay(time, TimeUnit.SECONDS);
                    }
                })
                .buffer(2, TimeUnit.SECONDS)
                .subscribe(new Action1<List<String>>() {
                    @Override
                    public void call(List<String> strings) {
                        LogUtils.d(TAG, "values: " + Arrays.toString(strings.toArray()));
                    }
                });
    }

    private void performGroupBy() {
        mAppAdapter.clear();

        getApps()
                .groupBy(new Func1<AppInfo, String>() {
                    @Override
                    public String call(AppInfo appInfo) { // 根据第一次安装时间进行分组
                        SimpleDateFormat formatter = new SimpleDateFormat("MM/yyyy", Locale.CHINA);
                        return formatter.format(new Date(appInfo.getFirstInstallTime()));
                    }
                })
                .subscribe(new Action1<GroupedObservable<String, AppInfo>>() {
                    @Override
                    public void call(final GroupedObservable<String, AppInfo> result) {
                        // 只显示 key 为 09/2016 的 Apps
                        if (!result.getKey().equalsIgnoreCase("09/2016")) {
                            return;
                        }

                        result.toList()
                                .flatMap(new Func1<List<AppInfo>, Observable<AppInfo>>() {
                                    @Override
                                    public Observable<AppInfo> call(List<AppInfo> appInfos) {
                                        return Observable.from(appInfos);
                                    }
                                })
                                .subscribe(new Action1<AppInfo>() {
                                    @Override
                                    public void call(AppInfo appInfo) {
                                        appInfo.setName(appInfo.getName() + " " + result.getKey());
                                        mAppAdapter.add(appInfo);
                                    }
                                });
                    }
                });
    }

    private void performScan() {
        mAppAdapter.clear();

        getApps()
                .scan(new Func2<AppInfo, AppInfo, AppInfo>() {
                    @Override
                    public AppInfo call(AppInfo appInfo, AppInfo appInfo2) {
                        if (appInfo.getName().length() > appInfo2.getName().length()) {
                            return appInfo;
                        } else {
                            return appInfo2;
                        }
                    }
                })
                .distinct()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppAdapter.add(appInfo);
                    }
                });
    }

    private void performSwitchMap() {
        mAppAdapter.clear();

        getApps().take(3)
                .switchMap(new Func1<AppInfo, Observable<AppInfo>>() {
                    @Override
                    public Observable<AppInfo> call(AppInfo appInfo) {
                        String name = appInfo.getName();
                        appInfo.setName(name + " switchMap");
                        LogUtils.d(TAG, "switchMap -- 1 -- " + appInfo.getName());
                        return Observable.just(appInfo)
                                .delay(1, TimeUnit.SECONDS);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppAdapter.add(appInfo);
                        LogUtils.d(TAG, "switchMap -- 2 -- " + appInfo.getName());
                    }
                });
    }

    private void performConcatMap() {
        mAppAdapter.clear();

        getApps().take(3)
                .concatMap(new Func1<AppInfo, Observable<AppInfo>>() {
                    @Override
                    public Observable<AppInfo> call(AppInfo appInfo) {
                        String name = appInfo.getName();
                        appInfo.setName(name + " concatMap");
                        LogUtils.d(TAG, "concatMap -- 1 -- " + appInfo.getName());
                        return Observable.just(appInfo)
                                .delay((long) (Math.random() * 2 + 0.5), TimeUnit.SECONDS);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppAdapter.add(appInfo);
                        LogUtils.d(TAG, "concatMap -- 2 -- " + appInfo.getName());
                    }
                });
    }

    private void performMap() {
        mAppAdapter.clear();

        Observable.from(mApps)
                .take(3)
                .map(new Func1<AppInfo, AppInfo>() {
                    @Override
                    public AppInfo call(AppInfo appInfo) {
                        String name = appInfo.getName();
                        appInfo.setName(name + " map");
                        LogUtils.d(TAG, "map -- 1 -- " + appInfo.getName());
                        return appInfo;
                    }
                })
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppAdapter.add(appInfo);
                        LogUtils.d(TAG, "map -- 2 -- " + appInfo.getName());
                    }
                });
    }

    private void performFlatMap() {
        mAppAdapter.clear();

        getApps().take(3)
                .flatMap(new Func1<AppInfo, Observable<AppInfo>>() {
                    @Override
                    public Observable<AppInfo> call(AppInfo appInfo) {
                        String name = appInfo.getName();
                        appInfo.setName(name + " flatMap");
                        LogUtils.d(TAG, "flatMap -- 1 -- " + appInfo.getName());
                        return Observable.just(appInfo)
                                .delay((long) (Math.random() * 2 + 0.5), TimeUnit.SECONDS);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppAdapter.add(appInfo);
                        LogUtils.d(TAG, "flatMap -- 2 -- " + appInfo.getName());
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
                        mSwipeLayout.setRefreshing(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.SHORT.show(getBaseContext(), "Something went wrong!");
                        mSwipeLayout.setRefreshing(false);
                    }

                    @Override
                    public void onNext(List<AppInfo> appInfos) {
                        mApps.addAll(appInfos);
                        mAppAdapter.setData(appInfos);
                        mAppsRecycler.setVisibility(View.VISIBLE);
                    }
                });
    }

    private Observable<AppInfo> getApps() {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        return Observable.from(this.getPackageManager().queryIntentActivities(mainIntent, 0))
                .map(new Func1<ResolveInfo, AppInfo>() { // 将源数据源 ResolveInfo 元素转换为 AppInfo
                    @Override
                    public AppInfo call(ResolveInfo info) {
                        AppInfoRich appInfoRich = new AppInfoRich(TransformObservablesActivity.this, info);
                        Bitmap icon = BitmapUtils.drawableToBitmap(appInfoRich.getIcon());
                        String name = appInfoRich.getName();
                        String iconPath = App.getInstance().getFilesDir() + "/" + name;
                        BitmapUtils.storeBitmap(App.getInstance(), icon, name);
                        return new AppInfo(name, iconPath,
                                appInfoRich.getLastUpdateTime(), appInfoRich.getFirstInstallTime());
                    }
                });
    }
}
