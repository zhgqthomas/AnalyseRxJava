package com.zhgqthomas.rxjava.ui.apps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.zhgqthomas.rxjava.R;
import com.zhgqthomas.rxjava.entity.AppInfo;
import com.zhgqthomas.rxjava.ui.base.BaseRecyclerAdapter;
import com.zhgqthomas.rxjava.ui.base.BaseRecyclerViewHolder;

import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class AppListAdapter extends BaseRecyclerAdapter<AppInfo> {

    public AppListAdapter(Context context, List<AppInfo> data) {
        super(context, data);
    }

    @Override
    public int getItemLayoutId(int viewType) {
        return R.layout.item_app_info;
    }

    @Override
    public void bindData(final BaseRecyclerViewHolder holder, int position, AppInfo appInfo) {
        Observable.just(appInfo.getIcon())
                .map(new Func1<String, Bitmap>() {
                    @Override
                    public Bitmap call(String iconPath) {
                        return BitmapFactory.decodeFile(iconPath);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Bitmap>() {
                    @Override
                    public void call(Bitmap bitmap) {
                        holder.getImageView(R.id.app_icon).setImageBitmap(bitmap);
                    }
                });

        holder.getTextView(R.id.app_name).setText(appInfo.getName());
    }
}
