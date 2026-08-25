// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hive.anim.AnimUtils;
import com.hive.utils.system.CommonUtils;
import com.hive.views.R;

/**
 * Created by Admin on 2016/1/25.
 */
public abstract class CustomBaseDialog extends Dialog {
    public ContentView mContentView;
    public IOnBtnClickListener mOnBtnClickListener;
    private String mTitle;
    protected Context mContext;
    protected int DP=1;

    public CustomBaseDialog(Context context, String title, boolean isTouch) {
        super(context, R.style.base_dialog_distouch);
        DP = CommonUtils.dipToPx(context, 1);
        this.mContext = context;
        setStyle();
        this.mTitle = title;
        mContentView = new ContentView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        this.setContentView(mContentView, lp);
        setTitle(title);
        mContentView.mSubmitText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimUtils.scaleAnim(v);
                if (mOnBtnClickListener != null)
                    mOnBtnClickListener.onSubmit();
            }
        });
        mContentView.mCancelText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimUtils.scaleAnim(v);
                if (mOnBtnClickListener != null)
                    mOnBtnClickListener.onCancel();
            }
        });
        initView();
    }

    public CustomBaseDialog(Context context, String title) {
        super(context, R.style.base_dialog);
        this.mContext = context;
        setStyle();
        this.mTitle = title;
        mContentView = new ContentView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        this.setContentView(mContentView, lp);
        setTitle(title);
        mContentView.mSubmitText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimUtils.scaleAnim(v);
                if (mOnBtnClickListener != null)
                    mOnBtnClickListener.onSubmit();
            }
        });
        mContentView.mCancelText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimUtils.scaleAnim(v);
                if (mOnBtnClickListener != null)
                    mOnBtnClickListener.onCancel();
            }
        });
        initView();
    }

    protected abstract void initView();

    private void setStyle() {
        Window window = getWindow();
        WindowManager.LayoutParams wl = window.getAttributes();
        wl.gravity = Gravity.CENTER;
        this.onWindowAttributesChanged(wl);
        getWindow().setAttributes(wl);
    }

    /**
     * 设置确认键文字
     *
     * @param text
     */
    public void setSubmitText(String text) {
        mContentView.mSubmitText.setText(text);
    }

    /**
     * 设置取消键文件
     *
     * @param text
     */
    public void setCancelText(String text) {
        mContentView.mCancelText.setText(text);
    }

    /**
     * 设置title;
     *
     * @param title
     */
    public void setTitle(String title) {
        mContentView.mTitleText.setText(title);
    }


    /**
     * 设置点击按钮监听；
     */
    public void setOnBtnClickListener(IOnBtnClickListener onBtnClickListener) {
        this.mOnBtnClickListener = onBtnClickListener;

    }


    /**
     * 内容视图；
     */
    public class ContentView extends LinearLayout {
        public LayoutInflater mInflater;
        public View mView;
        private Context mContext;
        public TextView mTitleText;
        public LinearLayout mMsgLayout;
        public TextView mSubmitText;
        public TextView mCancelText;
        private int DP = 1;

        public ContentView(Context context) {
            super(context);
            this.mContext = context;
            DP = CommonUtils.dipToPx(mContext, 1);
            initView();
        }


        /**
         * 初始化视图；
         */
        private void initView() {
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mView = mInflater.inflate(R.layout.custom_base_dialog, null);
            mTitleText = (TextView) mView.findViewById(R.id.dialog_title);
            mMsgLayout = (LinearLayout) mView.findViewById(R.id.dialog_content);
            mSubmitText = (TextView) mView.findViewById(R.id.dialog_btn_submit);
            mCancelText = (TextView) mView.findViewById(R.id.dialog_btn_cancel);
            mMsgLayout.setGravity(Gravity.CENTER_VERTICAL);
            mMsgLayout.setPadding(0, 0, 0, 0);
            mMsgLayout.addView(mInflater.inflate(getChildResId(), null));
            this.addView(mView);

        }


    }

    protected abstract int getChildResId();

    /**
     * 监听点击接口；
     */
    public interface IOnBtnClickListener {
        public void onSubmit();

        public void onCancel();
    }
}