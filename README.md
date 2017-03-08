此文章结合 [Github AnalyseRxJava](https://github.com/zhgqthomas/AnalyseRxJava) 项目，给 Android 开发者带来 RxJava 详细的解说。参考自 [RxJava Essential](https://rxjava.yuxingxin.com/) 及书中的例子

关于 RxJava 的由来及简介，这里就不在重复了，感兴趣的请阅读 `RxJava Essential`。

## App 讲解
在此 App 中将检索安装的应用列表并填充 RecycleView 的 item 来展示它们。通过下拉刷新的功能和一个进度条来告知用户当前任务正在执行。

首先创建一个 `Observable` 来检索安装的应用程序列表并把它提供给我们的观察者。我们一个接一个的发射这些应用程序数据，将它们分组到一个单独的列表中，以此来展示响应式方法的灵活性。

```Java
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
```
与 `RxJava Essiential` 书中，使用 `Obervable.create` 方式不同，这里使用的是 `Observable.from`，在使用 RxJava 中应该尽量的避免编写着自定义操作符, 具体原因请阅读[实现操作符时的一些陷阱](实现操作符时的一些陷阱)等系列文，通过阅读该系列，我发现很难写出正确的操作符。所以尽量避免编写自定义操作符。

AppInfo对象如下：

```Java
@Data
@Accessors(prefix = "m")
public class AppInfo implements Comparable<AppInfo> {

    long mLastUpdateTime;

    String mName;

    String mIcon;

    public AppInfo(String name, String icon, long lastUpdateTime) {
        this.mLastUpdateTime = lastUpdateTime;
        this.mName = name;
        this.mIcon = icon;
    }

    @Override
    public int compareTo(@NonNull AppInfo appInfo) {
        // 实现改方法为之后使用 Observable.toSortedList
        return getName().compareTo(appInfo.getName());
    }
}
```
我们使用 `refreshAppList` 下拉刷新方法，因此列表数据可以来自初始化加载，或由用户触发的一个刷新动作。针对这两个场景，我们用同样的行为，因此我们把我们的观察者放在一个易被复用的函数里面。下面是我们的观察者，定义了成功、失败、完成要做的事情：

```Java
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
                        mAppInfos.addAll(appInfos);
                        mAppsRecyclerView.setVisibility(View.VISIBLE);
                        mAppsAdapter.setData(appInfos);
                        mAppsRefreshLayout.setRefreshing(false);
                    }
                });
    }
```
以上就来检索安装的应用程序列表并把它提供给我们的观察者。我们一个接一个的发射这些应用程序数据，将它们分组到一个单独的列表中。

## 基本操作符
在这一节中，我们将基于 RxJava 的 `just()`,`repeat()`,`defer()`,`range()`,`interval()` 方法展示一些例子。

### just()
此方法的示意图为
![](http://upload-images.jianshu.io/upload_images/854027-2105b126d7c978e1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

该方法可以接受 1 ~ 10 个元素作为参数，然后将这些元素依次的进行发射。可以将一个函数作为参数传给 `just()` 方法，你将会得到一个已存在代码的原始 Observable 版本。在一个新的响应式架构的基础上迁移已存在的代码，这个方法可能是一个有用的开始点。

```Java
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
```
检索列表并提取出三个元素, 依次进行订阅发射

### repeat()
此方法的示意图为
![](http://upload-images.jianshu.io/upload_images/854027-3f40e6e4163a9617.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
假如你想对一个 Observable 重复发射三次数据。例如，我们用 `just()` 例子中的 Observable：

```Java
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
```
正如你看到的，我们在 `just()` 创建 Observable 后追加了 `repeat(3)` ，它将会创建 9 个元素的序列，每一个都单独发射。

### defer()
此函数的示意图:
![](http://upload-images.jianshu.io/upload_images/854027-b88a8ae6def6965f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

`defer()` 用以确保Observable代码在被订阅后才执行（而不是创建后立即执行）。`just()`，`from()` 等这类能够[创建 Observable 的操作符](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables)。在创建之初，就已经存储了对象的值，而不被订阅的时候。这种情况，显然不是预期表现，我们想要的是无论什么时候请求，都能够表现为当前值。`defer()` 就能满足这种需求，**使用 `defer()` 操作符的唯一缺点就是，每次订阅都会创建一个新的 Observable 对象。`create()` 操作符则为每一个订阅者都使用同一个函数，所以，后者效率更高。**

```Java
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
```
以上代码，虽然 `List` 数据的反转是在 `Observable.just()` 之后，但是因为 `defer()` 函数的作用，还是会得到最新的反转之后的数据

### range()
此函数的示意图:
![](http://upload-images.jianshu.io/upload_images/854027-a6f6ca5688de5aad.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

`range()` 函数用两个数字作为参数：第一个是起始点，第二个是我们想发射数字的个数。

```Java
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
```

### interval()
此函数的示意图:
![](http://upload-images.jianshu.io/upload_images/854027-23980c74038ae4d1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

`interval()`函数在你需要创建一个轮询程序时非常好用。 `interval()` 函数的两个参数：一个指定两次发射的时间间隔，另一个是用到的时间单位。

```Java
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
```
### timer()
`timer()` 已作废，请参考 `interval()`





