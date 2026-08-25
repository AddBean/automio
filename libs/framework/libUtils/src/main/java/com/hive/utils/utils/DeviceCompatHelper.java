// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import android.content.res.Configuration;

import com.hive.utils.GlobalApp;
import com.hive.utils.global.CommonUtilsWrapper;

public class DeviceCompatHelper {

    private static DeviceCompatHelper sInstance;

    public static DeviceCompatHelper getInstance() {
        if (sInstance == null) sInstance = new DeviceCompatHelper();
        return sInstance;
    }

    public static boolean isDarkMode() {
        int currentMode = GlobalApp.getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentMode == Configuration.UI_MODE_NIGHT_YES;
    }

    public static boolean isLandscape() {
        return GlobalApp.isLandscape();
    }


    public static boolean isVertical() {
        return !isLandscape();
    }

    public static boolean isPad() {
        return CommonUtilsWrapper.isPadDevice(GlobalApp.getContext());
    }


}
