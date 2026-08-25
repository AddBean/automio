// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.hive.views.widgets.AbsStatefulLayout;

public class StatefulLayout extends AbsStatefulLayout implements View.OnClickListener {
    private OnLoadingListener mOnLoadingListener;

    public StatefulLayout(Context context) {
        super(context);
    }

    public StatefulLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StatefulLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSetState(State state) {
    }

    public void showOffline(final OnLoadingListener onLoadingListener) {
        super.showOffline();
        mOnLoadingListener = onLoadingListener;
        View view = findViewById(R.id.text_retry);
        if (view == null) return;
        view.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.text_retry) {
            if (mOnLoadingListener != null)
                mOnLoadingListener.onRetry(v);
        }
    }

    public interface OnLoadingListener {
        void onRetry(View v);
    }
}
