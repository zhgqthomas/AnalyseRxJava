package com.zhgqthomas.rxjava.ui.apps;

import android.content.Context;
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


public class FilterObservablesActivity extends BaseActivity {

    private static final String TAG = LogUtils.makeLogTag(FilterObservablesActivity.class);

    public static Intent getStartIntent(Context context) {
        return new Intent(context, FilterObservablesActivity.class);
    }

    @BindView(R.id.apps_recycler_view)
    RecyclerView mAppsRecyclerView;
    @BindView(R.id.apps_swipe_refresh)
    SwipeRefreshLayout mAppsRefreshLayout;

    private AppListAdapter mAppsAdapter;
    private List<AppInfo> mAppInfos = new ArrayList<>();
    private Subscription mInterval;
    private Subscription mDistinctInterval;
    private Subscription mSampleInterval;
    private Subscription mTimeoutInterval;
    private Subscription mDebounceInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        setSupportActionBar((Toolbar) findViewById(R.id.tool_bar));
        if (null != getSupportActionBar()) {
            getSupportActionBar().setTitle(R.string.filter_observable);
        }

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
    protected void onDestroy() {
        super.onDestroy();
        unsubscribe();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filter, menu);
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
            case R.id.filter:
                performFilter();
                return true;
            case R.id.take:
                performTake();
                return true;
            case R.id.take_last:
                performTakeLast();
                return true;
            case R.id.distinct:
                performDistinct();
                return true;
            case R.id.distinct_until_changed:
                performDistinctUntilChanged();
                return true;
            case R.id.first:
                performFirst();
                return true;
            case R.id.last:
                performLast();
                return true;
            case R.id.skip:
                performSkip();
                return true;
            case R.id.skip_last:
                performSkipLast();
                return true;
            case R.id.element_at:
                performElementAt();
                return true;
            case R.id.sample:
                performSample();
                return true;
            case R.id.timeout:
                performTimeout();
                return true;
            case R.id.debounce:
                performDebounce();
                return true;
            default:
                return false;
        }
    }

    private void performDebounce() {
        mAppsAdapter.clear();
        mDebounceInterval = Observable.interval(2, TimeUnit.SECONDS)
                .map(new Func1<Long, AppInfo>() {
                    @Override
                    public AppInfo call(Long aLong) {
                        if (aLong.intValue() == mAppInfos.size() - 1) {
                            if (!mDebounceInterval.isUnsubscribed()) {
                                mDebounceInterval.unsubscribe();
                            }
                        }
                        return mAppInfos.get(aLong.intValue());
                    }
                })
                .debounce(1, TimeUnit.SECONDS) // 通过修改数值来显示不同的结果
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performTimeout() {
        mAppsAdapter.clear();
        mTimeoutInterval = Observable.interval(1, 3, TimeUnit.SECONDS)  // 第 1 秒时发射一个 AppInfo, 之后每隔 3 秒发射一个 AppInfo
                .map(new Func1<Long, AppInfo>() {
                    @Override
                    public AppInfo call(Long aLong) {
                        if (aLong.intValue() == mAppInfos.size() - 3) {
                            if (!mTimeoutInterval.isUnsubscribed()) {
                                mTimeoutInterval.unsubscribe();
                            }
                        }
                        return mAppInfos.get(aLong.intValue());
                    }
                })
                .timeout(2, TimeUnit.SECONDS) // 超时时间设置为 2 秒
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<AppInfo>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.SHORT.show(getBaseContext(), "Timeout!!!!");
                    }

                    @Override
                    public void onNext(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performSample() { // 每秒发出一个 AppInfo，但是每隔三秒的时候才显示出来
        mAppsAdapter.clear();
        mSampleInterval = Observable.interval(1, TimeUnit.SECONDS)
                .map(new Func1<Long, AppInfo>() {
                    @Override
                    public AppInfo call(Long aLong) {
                        if (aLong.intValue() == mAppInfos.size() - 1) {
                            if (!mSampleInterval.isUnsubscribed()) {
                                mSampleInterval.unsubscribe();
                            }
                        }
                        return null;
                    }
                })
                .sample(3, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performElementAt() { // 发射序列中的第 4 个元素，如果没有默认发射序列中第一个元素
        mAppsAdapter.clear();
        Observable.from(mAppInfos)
                .elementAtOrDefault(3, mAppInfos.get(0))
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performSkipLast() { // 跳过最后两条 AppInfo
        mAppsAdapter.clear();
        Observable.from(mAppInfos)
                .skipLast(2)
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performSkip() { // 跳过头两条 AppInfo
        mAppsAdapter.clear();
        Observable.from(mAppInfos)
                .skip(2)
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performLast() { // 过滤出序列中最后一个以 C 开头的 AppInfo
        mAppsAdapter.clear();
        Observable.from(mAppInfos)
                .last(new Func1<AppInfo, Boolean>() {
                    @Override
                    public Boolean call(AppInfo appInfo) {
                        return appInfo.getName().startsWith("C");
                    }
                })
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performFirst() { // 过滤出序列中第一个以 C 开头的 AppInfo
        mAppsAdapter.clear();
        Observable.from(mAppInfos)
                .first(new Func1<AppInfo, Boolean>() {
                    @Override
                    public Boolean call(AppInfo appInfo) {
                        return appInfo.getName().startsWith("C");
                    }
                })
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performDistinctUntilChanged() {
        //
        mAppsAdapter.clear();
        mDistinctInterval = Observable.interval(1, TimeUnit.SECONDS)
                .map(new Func1<Long, AppInfo>() {
                    @Override
                    public AppInfo call(Long aLong) {

                        if (aLong.intValue() == mAppInfos.size() - 1) {
                            if (!mDistinctInterval.isUnsubscribed()) {
                                mDistinctInterval.unsubscribe();
                            }
                        }

                        if (aLong.intValue() % 3 == 0) {
                            return mAppInfos.get(aLong.intValue());
                        }

                        return mAppInfos.get(3);
                    }
                })
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performDistinct() { // 获取序列的头三条数据，然后重复三次，最后将重复去掉
        mAppsAdapter.clear();
        Observable.from(mAppInfos)
                .take(3)
                .repeat(3)
                .distinct()
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performTakeLast() {
        mAppsAdapter.clear();
        Observable.from(mAppInfos)
                .takeLast(5) // 显示序列后 5 个 AppInfo
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performTake() {
        mAppsAdapter.clear();
        Observable.from(mAppInfos)
                .take(5) // 显示序列头 5 个 AppInfo
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performFilter() {
        mAppsAdapter.clear();
        Observable.from(mAppInfos)
                .filter(new Func1<AppInfo, Boolean>() {
                    @Override
                    public Boolean call(AppInfo appInfo) { // 过滤出以 C 开头的 AppInfo
                        return appInfo.getName().startsWith("C");
                    }
                })
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performInterval() { // 每 2 秒显示一个 AppInfo
        mAppsAdapter.clear();
        mInterval = Observable.interval(2, TimeUnit.SECONDS) // interval 默认是在 work 线程
                .map(new Func1<Long, AppInfo>() {
                    @Override
                    public AppInfo call(Long index) {
                        if (index.intValue() < 5) {
                            if (!mInterval.isUnsubscribed()) {
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
        mAppsAdapter.clear();
        Observable.just(mAppInfos.get(0), mAppInfos.get(1), mAppInfos.get(2))
                .repeat(3)
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void performJust() {
        mAppsAdapter.clear();
        Observable.just(mAppInfos.get(0), mAppInfos.get(1), mAppInfos.get(2))
                .subscribe(new Action1<AppInfo>() {
                    @Override
                    public void call(AppInfo appInfo) {
                        mAppsAdapter.add(appInfo);
                    }
                });
    }

    private void refreshAppList() {
        unsubscribe();

        getApps().toSortedList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<AppInfo>>() {
                    @Override
                    public void onCompleted() {
                        mAppsRefreshLayout.setRefreshing(false);
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
                        AppInfoRich appInfoRich = new AppInfoRich(FilterObservablesActivity.this, info);
                        Bitmap icon = BitmapUtils.drawableToBitmap(appInfoRich.getIcon());
                        String name = appInfoRich.getName();
                        String iconPath = App.getInstance().getFilesDir() + "/" + name;
                        BitmapUtils.storeBitmap(App.getInstance(), icon, name);
                        return new AppInfo(name, iconPath,
                                appInfoRich.getLastUpdateTime(), appInfoRich.getFirstInstallTime());
                    }
                });
    }

    private void unsubscribe() {
        if (null != mInterval && !mInterval.isUnsubscribed()) {
            mInterval.unsubscribe();
        }

        if (null != mDistinctInterval && !mDistinctInterval.isUnsubscribed()) {
            mDistinctInterval.unsubscribe();
        }

        if (null != mSampleInterval && !mSampleInterval.isUnsubscribed()) {
            mSampleInterval.unsubscribe();
        }

        if (null != mTimeoutInterval && !mTimeoutInterval.isUnsubscribed()) {
            mTimeoutInterval.unsubscribe();
        }

        if (null != mDebounceInterval && !mDebounceInterval.isUnsubscribed()) {
            mDebounceInterval.unsubscribe();
        }
    }
}
