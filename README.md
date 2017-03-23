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

### filter()
此函数示意图为:

![](http://upload-images.jianshu.io/upload_images/854027-3c7814736a8da128.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

`filter()` 函数接受一个 `Func1` 对象，即只有一个参数的函数。`Func1` 有一个 Object 对象来作为它的参数类型并且返回 Boolean 对象。只要条件符合 `filter()` 函数就会返回 true。此时，值会发射出去并且所有的观察者都会接收到。

```Java
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
```
以上函数，遍历已安装的应用列表，只展示以字母 C 开头的已安装的应用

### take()
此函数的示意图为:

![](http://upload-images.jianshu.io/upload_images/854027-29d62624c96fa6ce.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

`take()` 函数用整数 N 来作为一个参数，从原始的序列中发射前 N 个元素
```Java
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
```

### takeLast()
此函数的示意图为:

![](http://upload-images.jianshu.io/upload_images/854027-2151fcd5de327c27.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

`takeLast()` 函数用整数 N 来作为一个参数，从原始的序列中发射后 N 个元素

```Java
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
```

### distinct()
此函数的示意图为:

![](http://upload-images.jianshu.io/upload_images/854027-8f8a4188b273e33a.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

如果我们想对一个指定的值仅处理一次该怎么办？我们可以对我们的序列使用 `distinct()` 函数去掉重复的。就像 `takeLast()` 一样，`distinct()` 作用于一个完整的序列，然后得到重复的过滤项，它需要记录每一个发射的值。如果你在处理一大堆序列或者大的数据记得关注内存使用情况。

```Java
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
```

### distinctUntilChanged()
此函数示意图为:

![](http://upload-images.jianshu.io/upload_images/854027-f0352e58f4c71a39.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

`ditinctUntilChanged()` 过滤函数能做到这一点。它能轻易的忽略掉所有的重复并且只发射出新的值。

```Java

```
### first()
此函数示意图为:

![](http://upload-images.jianshu.io/upload_images/854027-9dddfe0cad6237de.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

`first()` 方法, 它们从 Observable 中只发射第一个元素。传 Func1 作为参数，：一个可以确定我们感兴趣的第一个符合约束条件的元素。

```Java

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
```

### last()
此函数示意图为:

![](http://upload-images.jianshu.io/upload_images/854027-294d5eb56fab0a3e.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

`last()` 方法, 它们从 Observable 中只发射最后一个元素。传Func1作为参数，：一个可以确定我们感兴趣的最后一个的符合约束条件的元素。

```Java
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
```

### skip() & skipLast()
`skip()` 此函数的示意图为：

![](http://upload-images.jianshu.io/upload_images/854027-fb49bf3fd49e27c4.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

`skipLast()` 此函数的示意图为：

![](http://upload-images.jianshu.io/upload_images/854027-922e8efb8da278fe.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


`skip()` 和 `skipLast()` 函数与 `take()` 和 `takeLast()` 相对应。它们用整数 N 作参数，从本质上来说，它们不让 Observable 发射前 N 个或者后 N 个值。如果我们知道一个序列以没有太多用的“可控”元素开头或结尾时我们可以使用它。

```Java
// skip()

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
```

```Java
// skipLast()

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
```

### elementAt()
此函数的示意图：

![](http://upload-images.jianshu.io/upload_images/854027-78645ec37d3c2312.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

如果我们只想要可观测序列发射的第五个元素该怎么办？elementAt() 函数仅从一个序列中发射第 n 个元素然后就完成了。
如果我们想查找第五个元素但是可观测序列只有三个元素可供发射时该怎么办？我们可以使用 `elementAtOrDefault()` 。下图展示了如何通过使用 elementAt(2) 从一个序列中选择第三个元素以及如何创建一个只发射指定元素的新的 Observable。

```Java
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
```

### sample()
此函数的示意图：

![](http://upload-images.jianshu.io/upload_images/854027-3687df179fcc1fe3.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

如果我们想让 App 列表美妙发射一个元素，但是只是每 3 秒才显示当前发射的元素，更恰当的例子是温度传感器。它每秒都会发射当前室内的温度。说实话，我们并不认为温度会变化这么快，我们可以使用一个小的发射间隔。
这时候就可以用到 `sample()`

```Java
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
```

### timeout()
此函数的示意图为：

![](http://upload-images.jianshu.io/upload_images/854027-08cb1213f740d923.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

假设我们工作的是一个时效性的环境，我们温度传感器每秒都在发射一个温度值。我们想让它每隔两秒至少发射一个，我们可以使用 `timeout()` 函数来监听源可观测序列,就是在我们设定的时间间隔内如果没有得到一个值则发射一个错误。我们可以认为 `timeout()` 为一个 Observable 的限时的副本。如果在指定的时间间隔内 Observable 不发射值的话，它监听的原始的 Observable 时就会触发 `onError()` 函数。类似的还可以用于网络请求超时的异常处理

```Java
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
```

### debounce()
此函数的示意图:

![](http://upload-images.jianshu.io/upload_images/854027-2aede4f9f55f72bb.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

`debounce()` 函数过滤掉由 Observable 发射的速率过快的数据；如果在一个指定的时间间隔过去了仍旧没有发射一个，那么它将发射**最后的那个**。
就像 `sample()` 和 `timeout()` 函数一样，`debounce()` 使用 TimeUnit 对象指定时间间隔。
下图展示了多久从 Observable 发射一次新的数据，`debounce()` 函数开启一个内部定时器，如果在这个时间间隔内没有新的数据发射，则新的 Observable 发射出最后一个数据：

> debounce 是一个非常有用的函数，在 [RxBinding](https://github.com/JakeWharton/RxBinding) 中，就使用 debounce 来解决多次点击按钮等问题

```Java
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
```