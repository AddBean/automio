// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.engineer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.hive.net.engineer.EngineerConfig;
import com.hive.net.engineer.EngineerObservable;
import com.hive.views.R;


public class EngineerEnterLayout extends RelativeLayout implements View.OnClickListener, EngineerObservable.ConfigObserver {

    private View mView;

    public EngineerEnterLayout(Context context) {
        super(context);
        initView();
    }

    public EngineerEnterLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public EngineerEnterLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mView = LayoutInflater.from(getContext()).inflate(R.layout.engineer_enter_layout, this);
        mView.setOnClickListener(this);
        setVisibility(EngineerConfig.read().engineerOn ? VISIBLE : GONE);
        EngineerObservable.registerObserver(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EngineerObservable.unregisterObserver(this);
    }

    @Override
    public void onClick(View v) {
        EngineerActivity.start(v.getContext());
    }


    @Override
    public void applyConfig(EngineerConfig config) {
        setVisibility(EngineerConfig.read().engineerOn ? VISIBLE : GONE);
    }
}
