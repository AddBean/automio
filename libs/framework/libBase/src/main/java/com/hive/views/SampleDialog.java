// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views;

import android.app.Dialog;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.hive.base.R;

public class SampleDialog extends Dialog implements View.OnClickListener {
    public ViewHolder mViewHolder;
    private View mView;

    public static class ViewHolder {
        public TextView mTvTitle;
        public TextView mTvContent;
        public TextView mTvBtnCancel;
        public TextView mTvBtnSubmit;
        public View mViewLines;
        public FrameLayout mLayoutHolder;

        ViewHolder(View view) {
            mTvTitle = view.findViewById(R.id.tv_title);
            mTvContent = view.findViewById(R.id.tv_content);
            mTvBtnCancel = view.findViewById(R.id.tv_btn_cancel);
            mTvBtnSubmit = view.findViewById(R.id.tv_btn_submit);
            mViewLines = view.findViewById(R.id.view_lines);
            mLayoutHolder = view.findViewById(R.id.layout_holder);

        }
    }

    public SampleDialog(@NonNull Context context) {
        this(context, com.hive.views.R.style.base_dialog);
    }

    public SampleDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        initView();
    }

    protected SampleDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        initView();
    }


    protected void initView() {
        mView = LayoutInflater.from(getContext()).inflate(getLayoutId(), null);
        setContentView(mView);
        mViewHolder = new ViewHolder(mView);
        mViewHolder.mTvBtnCancel.setOnClickListener(this);
        mViewHolder.mTvBtnSubmit.setOnClickListener(this);
    }

    protected int getLayoutId() {
        return R.layout.sample_dialog;
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_btn_cancel) {
            if (mOnDialogListener != null) mOnDialogListener.onItemClick(false);
        }
        if (v.getId() == R.id.tv_btn_submit) {
            if (mOnDialogListener != null) mOnDialogListener.onItemClick(true);
        }
    }


    public SampleDialog setLeftText(CharSequence text) {
        mViewHolder.mTvBtnCancel.setText(text);
        return this;
    }

    public SampleDialog setRightText(CharSequence text) {
        mViewHolder.mTvBtnSubmit.setText(text);
        return this;
    }

    public SampleDialog setDialogTitle(CharSequence title) {
        mViewHolder.mTvTitle.setText(title);
        return this;
    }

    public SampleDialog setDialogContent(CharSequence content) {
        mViewHolder.mTvContent.setText(content);
        return this;
    }


    public ViewHolder getViewHolder() {
        return mViewHolder;
    }

    private OnDialogListener mOnDialogListener;

    public SampleDialog setOnDialogListener(OnDialogListener mOnDialogListener) {
        this.mOnDialogListener = mOnDialogListener;
        return this;
    }

    public interface OnDialogListener {
        void onItemClick(boolean isRight);
    }
}
