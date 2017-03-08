package com.zhgqthomas.rxjava.ui.base;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRecyclerAdapter<T> extends RecyclerView.Adapter<BaseRecyclerViewHolder> {

    public static final int TYPE_HEADER = 1;
    public static final int TYPE_ITEM = 2;
    public static final int TYPE_FOOTER = 3;

    protected List<T> mData;
    protected Context mContext;
    protected boolean mUseAnimation;
    protected LayoutInflater mInflater;
    protected OnItemClickListener mClickListener;
    protected OnItemLongClickListener mLongClickListener;
    protected boolean mShowFooter;
    private RecyclerView.LayoutManager mLayoutManager;
    private int mLastPosition = -1;
    private int mFooterViewRes = -1;


    public BaseRecyclerAdapter(Context context, List<T> data) {
        this(context, data, true);
    }

    public BaseRecyclerAdapter(Context context, List<T> data, boolean useAnimation) {
        this(context, data, useAnimation, null);
    }

    public BaseRecyclerAdapter(Context context, List<T> data, boolean useAnimation, RecyclerView.LayoutManager layoutManager) {
        mContext = context;
        mUseAnimation = useAnimation;
        mLayoutManager = layoutManager;
        mData = data == null ? new ArrayList<T>() : data;
        mInflater = LayoutInflater.from(context);
    }


    @Override
    public BaseRecyclerViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        if (viewType == TYPE_FOOTER) {
            if (mFooterViewRes == -1) {
                throw new Resources.NotFoundException("footer view resource not found.");
            }

            return new BaseRecyclerViewHolder(mContext, mInflater.inflate(mFooterViewRes, parent, false));
        } else {
            final BaseRecyclerViewHolder holder = new BaseRecyclerViewHolder(mContext,
                    mInflater.inflate(getItemLayoutId(viewType), parent, false));

            if (null != mClickListener) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mClickListener.onItemClick(v, holder.getLayoutPosition());
                    }
                });
            }

            if (null != mLongClickListener) {
                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        return mLongClickListener.onItemLongClick(view, holder.getLayoutPosition());
                    }
                });
            }

            return holder;
        }
    }

    @Override
    public void onBindViewHolder(BaseRecyclerViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_FOOTER) {
            if (null != mLayoutManager) {
                if (mLayoutManager instanceof StaggeredGridLayoutManager) {
                    if (((StaggeredGridLayoutManager) mLayoutManager).getSpanCount() != 1) {
                        StaggeredGridLayoutManager.LayoutParams params = (StaggeredGridLayoutManager.LayoutParams) holder.itemView
                                .getLayoutParams();
                        params.setFullSpan(true);
                    }
                } else if (mLayoutManager instanceof GridLayoutManager) {
                    if (((GridLayoutManager) mLayoutManager)
                            .getSpanCount() != 1 && ((GridLayoutManager) mLayoutManager)
                            .getSpanSizeLookup() instanceof GridLayoutManager.DefaultSpanSizeLookup) {
                        throw new RuntimeException("网格布局列数大于1时应该继承SpanSizeLookup时处理底部加载时布局占满一行。");
                    }
                }
            }
        } else {
            bindData(holder, position, mData.get(position));
            if (mUseAnimation) {
                setAnimation(holder.itemView, position);
            }
        }
    }

    protected void setAnimation(View viewToAnimate, int position) {
        if (position > mLastPosition) {
//            Animation animation = AnimationUtils
//                    .loadAnimation(viewToAnimate.getContext(), R.anim.item_bottom_in);
//            viewToAnimate.startAnimation(animation);
            mLastPosition = position;
        }
    }


    @Override
    public void onViewDetachedFromWindow(BaseRecyclerViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (mUseAnimation && holder.itemView.getAnimation() != null && holder.itemView
                .getAnimation().hasStarted()) {
            holder.itemView.clearAnimation();
        }
    }

    public void add(T item) {
        mData.add(item);
        notifyDataSetChanged();
    }

    public void add(int pos, T item) {
        mData.add(pos, item);
        notifyItemInserted(pos);
    }

    public void delete(int pos) {
        mData.remove(pos);
        notifyItemRemoved(pos);
    }

    public void clear() {
        mData.clear();
        notifyDataSetChanged();
    }

    public void addMoreData(List<T> data) {
        int startPos = mData.size();
        mData.addAll(data);
        notifyItemRangeInserted(startPos, data.size());
    }

    public List<T> getData() {
        return mData;
    }

    public void setData(List<T> data) {
        mData = data;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (mShowFooter && getItemCount() - 1 == position) {
            return TYPE_FOOTER;
        }
        return bindViewType(position);
    }

    @Override
    public int getItemCount() {
        int i = mShowFooter ? 1 : 0;
        return mData != null ? mData.size() + i : 0;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mLongClickListener = listener;
    }

    public abstract int getItemLayoutId(int viewType);

    public abstract void bindData(BaseRecyclerViewHolder holder, int position, T item);

    protected int bindViewType(int position) {
        return 0;
    }

    public void setFootView(int viewRes) {
        mFooterViewRes = viewRes;
    }

    public void showFooter() {
        notifyItemInserted(getItemCount());
        mShowFooter = true;
    }

    public void hideFooter() {
        notifyItemRemoved(getItemCount() - 1);
        mShowFooter = false;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View view, int position);
    }
}
