// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.framework;

import android.app.Activity;
import android.app.Application;
import android.content.ClipboardManager;
import android.os.Bundle;

import com.hive.utils.GlobalApp;

public class CommonActivityCallbacks implements Application.ActivityLifecycleCallbacks, ClipboardManager.OnPrimaryClipChangedListener {
    public static Activity sCurrentActivity;


    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        sCurrentActivity = activity;
    }

    @Override
    public void onActivityStarted(Activity activity) {
        GlobalApp.sActivityCount++;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        GlobalApp.sTopActivity = activity;
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        GlobalApp.sActivityCount--;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        sCurrentActivity = null;
    }

    @Override
    public void onPrimaryClipChanged() {

    }
}
