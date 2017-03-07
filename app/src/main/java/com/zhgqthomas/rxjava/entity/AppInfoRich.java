package com.zhgqthomas.rxjava.entity;


import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.util.Locale;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class AppInfoRich implements Comparable<AppInfoRich> {

    @Setter
    String mName = null;
    @Getter
    ComponentName mComponentName;
    @Getter
    ResolveInfo mResolveInfo;
    @Getter
    PackageInfo mPackageInfo;

    private Context mContext;
    private Drawable mIcon;

    public AppInfoRich(Context context, ResolveInfo info) {
        mContext = context;
        mResolveInfo = info;

        mComponentName = new ComponentName(
                info.activityInfo.applicationInfo.packageName, info.activityInfo.name);

        try {
            mPackageInfo = context.getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    public String getName() {
        try {
            return null != mName ? mName : getNameFromResolveInfo();
        } catch (PackageManager.NameNotFoundException e) {
            return getPackageName();
        }
    }

    public String getComponentInfo() {
        return null != getComponentName() ? getComponentName().toString() : "";
    }

    public String getVersionName() {
        return null != getPackageInfo() ? getPackageInfo().versionName : "";
    }

    public int getVersionCode() {
        return null != getPackageInfo() ? getPackageInfo().versionCode : 0;
    }

    public Drawable getIcon() {
        return null == mIcon ? getResolveInfo().loadIcon(mContext.getPackageManager()) : mIcon;
    }

    public long getFirstInstallTime() {
        return null != getPackageInfo() ? getPackageInfo().firstInstallTime : 0;
    }

    public long getLastUpdateTime() {
        return null != getPackageInfo() ? getPackageInfo().lastUpdateTime : 0;
    }

    private String getNameFromResolveInfo() throws PackageManager.NameNotFoundException {
        String name = mResolveInfo.resolvePackageName;
        if (null != mResolveInfo.activityInfo) {
            Resources res = mContext.getPackageManager()
                    .getResourcesForApplication(mResolveInfo.activityInfo.applicationInfo);
            Resources engRes = getEnglishResources(res);

            if (0 != mResolveInfo.activityInfo.labelRes) {
                name = engRes.getString(mResolveInfo.activityInfo.labelRes);

                if (TextUtils.isEmpty(name)) {
                    name = res.getString(mResolveInfo.activityInfo.labelRes);
                }
            } else {
                name = mResolveInfo.activityInfo
                        .applicationInfo.loadLabel(mContext.getPackageManager()).toString();
            }
        }

        return name;
    }

    private Resources getEnglishResources(Resources res) {
        AssetManager assets = res.getAssets();
        DisplayMetrics metrics = res.getDisplayMetrics();
        Configuration config = new Configuration(res.getConfiguration());
        config.locale = Locale.US;
        return new Resources(assets, metrics, config);
    }

    public String getPackageName() {
        return mResolveInfo.activityInfo.packageName;
    }

    @Override
    public int compareTo(@NonNull AppInfoRich appInfoRich) {
        return getName().compareTo(appInfoRich.getName());
    }
}
