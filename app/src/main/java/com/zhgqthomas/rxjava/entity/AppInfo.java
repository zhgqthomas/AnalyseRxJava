package com.zhgqthomas.rxjava.entity;


import android.support.annotation.NonNull;

import lombok.Data;
import lombok.experimental.Accessors;

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
