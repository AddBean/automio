// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Admin on 2017/7/25.
 */

public abstract class BaseFragment extends Fragment {
    protected View mView;
    protected int DP = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DP = dp2Px(getContext(), 1);
        if (mView == null) {
            mView = inflater.inflate(getLayoutId(), container, false);
        }
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    public View getCurrentView() {
        return this.mView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public Context getContext() {
        return getActivity();
    }


    protected abstract void initView();

    protected abstract int getLayoutId();

    public void onShow() {
    }

    public void onHidden() {
    }

    public static int dp2Px(Context context, int dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        int px = (int) ((float) dp * scale + 0.5F);
        return px;
    }

    protected String getStr(int id){
        return getContext().getString(id);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // RefWatcher removed for compatibility
    }
}
