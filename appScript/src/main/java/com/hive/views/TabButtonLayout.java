// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.hive.app.script.R;


import androidx.annotation.Nullable;

import com.hive.base.BaseLayout;
import com.hive.net.NetHelper;
import com.hive.net.image.ImageLoadCallBack;
import com.hive.net.image.ImageLoader;
import com.hive.utils.StatisticsHelper;

public class TabButtonLayout extends BaseLayout implements View.OnClickListener {
    private ViewHolder mViewHolder;
    private int mColorChecked;
    private int mColorUnchecked;
    private String mNameChecked;
    private String mNameUnchecked;
    private int mDrawableChecked;
    private int mDrawableUnchecked;
    private boolean mChecked = false;
    private String mImageUrl;
    private String mPluginViewClassName;
    private BitmapDrawable mNetDrawable;
    private String mPluginName;

    public static int defaultColor = -9999999;

    static class ViewHolder {
        ImageView mTabIcon;
        TextView mTabTitle;

        ViewHolder(View view) {
            mTabIcon = view.findViewById(R.id.tab_icon);
            mTabTitle = view.findViewById(R.id.tab_title);
        }
    }

    public TabButtonLayout(Context context) {
        super(context);
    }

    public TabButtonLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TabButtonLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void initAttrs(AttributeSet attrs) {
        if (attrs == null) return;
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.TabButtonLayout);
        mChecked = typedArray.getBoolean(R.styleable.TabButtonLayout_checked, false);
        mColorChecked = typedArray.getColor(R.styleable.TabButtonLayout_color_checked, defaultColor);
        mColorUnchecked = typedArray.getColor(R.styleable.TabButtonLayout_color_unchecked, defaultColor);
        mDrawableChecked = typedArray.getResourceId(R.styleable.TabButtonLayout_res_checked, com.hive.i8n.R.drawable.logo);
        mDrawableUnchecked = typedArray.getResourceId(R.styleable.TabButtonLayout_res_unchecked, com.hive.i8n.R.drawable.logo);
        typedArray.recycle();
    }

    @Override
    protected void initView(View view) {
        mViewHolder = new ViewHolder(view);
        this.setOnClickListener(this);
        setSelected(mChecked);
    }

    public ImageView getIconView(){
        return mViewHolder.mTabIcon;
    }

    private void setCheckedStatus(boolean checked) {
        if (checked) {
            if (mDrawableChecked != com.hive.i8n.R.drawable.logo) {
                mViewHolder.mTabIcon.setImageResource(mDrawableChecked);
            }
            if (mNetDrawable != null) {
                mViewHolder.mTabIcon.setImageDrawable(mNetDrawable);
            }
            if (mColorChecked != defaultColor) {
                mViewHolder.mTabIcon.setColorFilter(mColorChecked);
                mViewHolder.mTabTitle.setTextColor(mColorChecked);
            }
            if (!TextUtils.isEmpty(mNameChecked)) {
                mViewHolder.mTabTitle.setText(mNameChecked);
            }
        } else {
            if (mDrawableUnchecked != com.hive.i8n.R.drawable.logo) {
                mViewHolder.mTabIcon.setImageResource(mDrawableUnchecked);
            }
            if (mNetDrawable != null) {
                mViewHolder.mTabIcon.setImageDrawable(mNetDrawable);
            }
            if (mColorUnchecked != defaultColor) {
                mViewHolder.mTabIcon.setColorFilter(mColorUnchecked);
                mViewHolder.mTabTitle.setTextColor(mColorUnchecked);
            }
            if (!TextUtils.isEmpty(mNameUnchecked)) {
                mViewHolder.mTabTitle.setText(mNameUnchecked);
            }
        }
    }

    @Override
    public void onClick(View v) {
        StatisticsHelper.INSTANCE.reportHomeTabEvent(mNameUnchecked);
        if (mOnTabSelectedListener != null) {
            if (mOnTabSelectedListener.onTabSelected(this)) return;
        }
        setSelected(true);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        mChecked = isSelected();
        setCheckedStatus(mChecked);
    }

    public void setColorChecked(int mColorChecked) {
        this.mColorChecked = mColorChecked;
    }

    public void setColorUnchecked(int mColorUnchecked) {
        this.mColorUnchecked = mColorUnchecked;
    }

    public void setNameChecked(String mNameChecked) {
        this.mNameChecked = mNameChecked;
    }

    public void setNameUnchecked(String mNameUnchecked) {
        this.mNameUnchecked = mNameUnchecked;
    }

    public void setDrawableChecked(int mDrawableChecked) {
        this.mDrawableChecked = mDrawableChecked;
    }

    public void setDrawableUnchecked(int mDrawableUnchecked) {
        this.mDrawableUnchecked = mDrawableUnchecked;
    }

    public void setNetDrawable(String imageUrl) {
        this.mImageUrl = NetHelper.covertRes(imageUrl);
        ImageLoader.getInstance().loadImageAsync(getContext(), mImageUrl, new ImageLoadCallBack() {
            @Override
            public void onImageLoadFinish(@Nullable Bitmap bitmap) {
                super.onImageLoadFinish(bitmap);
                mNetDrawable = new BitmapDrawable(bitmap);
                mViewHolder.mTabIcon.setImageDrawable(mNetDrawable);
            }
        });

    }

    public String getPluginName() {
        return mPluginName;
    }

    public void setPluginName(String mPluginName) {
        this.mPluginName = mPluginName;
    }


    public void setChecked(boolean mChecked) {
        this.mChecked = mChecked;
    }

    public void update() {
        setCheckedStatus(mChecked);
    }

    @Override
    public int getLayoutId() {
        return R.layout.tab_button_layout;
    }


    public OnTabSelectedListener mOnTabSelectedListener;

    public void setOnTabSelectedListener(OnTabSelectedListener mOnTabSelectedListener) {
        this.mOnTabSelectedListener = mOnTabSelectedListener;
    }

    public interface OnTabSelectedListener {
        boolean onTabSelected(TabButtonLayout tabButtonLayout);
    }

    public void setPluginViewClassName(String pluginView) {
        mPluginViewClassName = pluginView;
    }

    public String getPluginViewClassName() {
        return mPluginViewClassName;
    }


}
