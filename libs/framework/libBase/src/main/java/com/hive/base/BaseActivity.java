// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import android.os.PersistableBundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.hive.config.BuildConfigHelper;
import com.hive.utils.CommonBaseResources;
import com.hive.utils.GlobalApp;
import com.hive.utils.LanguageManager;
import com.hive.utils.ResultActivityAdaptor;
import com.hive.utils.debug.DLog;
import com.hive.utils.utils.DensityUtil;
import com.hive.utils.utils.ViewUtilsWrapper;

import com.hive.permissions.PermissionsChecker;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatDelegate;


/**
 * Created by Admin on 2016/5/16.
 */
public abstract class BaseActivity extends AppCompatActivity {
    protected int DP = 1;

    protected Resources resources;

    protected CommonLayoutInflaterFactory factory = new CommonLayoutInflaterFactory(this);

    protected ResultActivityAdaptor mResultActivityAdaptor = new ResultActivityAdaptor(this);
    public List<PermissionsChecker> mPermissionsCheckerList = new ArrayList<>();

    public List<ActivityResultCallback> mResultCallbackList = new ArrayList<>();

    public void registerResultCallback(ActivityResultCallback checker) {
        if (!mResultCallbackList.contains(checker))
            mResultCallbackList.add(checker);
    }

    public void unregisterResultCallback(ActivityResultCallback checker) {
        if (mResultCallbackList.contains(checker))
            mResultCallbackList.remove(checker);
    }


    public void registerPermissionsChecker(PermissionsChecker checker) {
        if (!mPermissionsCheckerList.contains(checker))
            mPermissionsCheckerList.add(checker);
    }

    public void unregisterPermissionsChecker(PermissionsChecker checker) {
        if (mPermissionsCheckerList.contains(checker))
            mPermissionsCheckerList.remove(checker);
    }

    public void startActivityWithCallback(Intent intent, ResultActivityAdaptor.ResultActivityListener listener) {
        mResultActivityAdaptor.startActivityForResult(intent, listener);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < mPermissionsCheckerList.size(); i++) {
            mPermissionsCheckerList.get(i).onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        for (int i = 0; i < mResultCallbackList.size(); i++) {
            mResultCallbackList.get(i).onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mResultActivityAdaptor.onResult(requestCode, resultCode, data);
        for (int i = 0; i < mPermissionsCheckerList.size(); i++) {
            mPermissionsCheckerList.get(i).onActivityResult(requestCode, resultCode, data);
        }
        for (int i = 0; i < mResultCallbackList.size(); i++) {
            mResultCallbackList.get(i).onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        if (supportEncodeString()) {
            getLayoutInflater().setFactory2(factory);
        }
        LanguageManager.INSTANCE.loadLanguage(this);
        if (BuildConfigHelper.getMapBoolean("isSupportScreenShot") == false) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        ActivityUtils.put(this);
        initSystemBar(this);
        onCreateBefore();
        super.onCreate(savedInstanceState);
        DLog.e("onCreate");
        DP = DensityUtil.dip2px(1);
        setContentView(getLayoutId());
        doOnCreate();
    }

    protected abstract void doOnCreate();


    protected void onCreateBefore() {

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityUtils.remove(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    protected abstract int getLayoutId();

    public void initSystemBar(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            int systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            window.getDecorView().setSystemUiVisibility(systemUiVisibility);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(context, com.hive.i8n.R.color.colorPrimary));
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        if (isSupportStatusBarCompat()) {
            ViewUtilsWrapper.setStatusBarBgColor(true, this, 0);
        }
        adjustNavigationBar();
    }

    protected boolean supportEncodeString() {
        return true;
    }

    @Override
    public Resources getResources() {
        if (supportEncodeString()) {
            if (resources == null) {
                resources = new CommonBaseResources(super.getResources());
            }
            return resources;
        }
        return super.getResources();
    }


    protected void adjustNavigationBar() {

    }

    public boolean isSupportStatusBarCompat() {
        return GlobalApp.isSupportStatusBar;
    }
}
