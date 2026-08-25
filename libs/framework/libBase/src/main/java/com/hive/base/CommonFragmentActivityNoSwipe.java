// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RelativeLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.hive.utils.utils.IntentUtils;
import com.hive.views.IBackListener;

public class CommonFragmentActivityNoSwipe extends BaseFragmentActivity {
    public static final String FRAGMENT_CLAZZ_NAME = "fragment_clazz_name";
    private ViewHolder mViewHolder;
    private String mFragmentClazzName = null;
    private Fragment fragment;

    static class ViewHolder {
        RelativeLayout mLayoutContent;

        ViewHolder(CommonFragmentActivityNoSwipe view) {
            mLayoutContent = view.findViewById(R.id.layout_content);
        }
    }

    @Override
    protected void doOnCreate(Bundle savedState) {
        mViewHolder = new ViewHolder(this);
        mFragmentClazzName = getIntent().getStringExtra(FRAGMENT_CLAZZ_NAME);
        if (TextUtils.isEmpty(mFragmentClazzName)) {
            return;
        }
        loadHostFragment();
    }

    private void loadHostFragment() {
        try {
            fragment = (Fragment) Class.forName(mFragmentClazzName).newInstance();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            fragment.setArguments(getIntent().getExtras());
            transaction.replace(R.id.layout_content, fragment);
            transaction.commitAllowingStateLoss();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.common_fragment_activity;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (fragment instanceof IBackListener) {
            if (((IBackListener) fragment).onBackPressed()) {
                return;
            }
        }
        super.onBackPressed();
    }

    public static <T extends Fragment> void start(Context context, Class<T> clazz, Bundle extras) {
        if (extras == null) extras = new Bundle();
        Intent intent = new Intent(context, CommonFragmentActivityNoSwipe.class);
        intent.putExtras(extras);
        intent.putExtra(FRAGMENT_CLAZZ_NAME, clazz.getName());
        IntentUtils.safeStartActivity(context, intent);
    }

    public static <T extends Fragment> void startForResult(Context context, Class<T> clazz, Bundle extras, int requestCode) {
        if (extras == null) extras = new Bundle();
        Intent intent = new Intent(context, CommonFragmentActivityNoSwipe.class);
        intent.putExtras(extras);
        intent.putExtra(FRAGMENT_CLAZZ_NAME, clazz.getName());
        IntentUtils.safeStartActivityForResult(context, intent, requestCode);
    }

    public static <T extends Fragment> void start(Context context, Class<T> clazz) {
        start(context, clazz, new Bundle());
    }
}
