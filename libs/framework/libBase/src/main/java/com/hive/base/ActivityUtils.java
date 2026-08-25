// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base;

import android.app.Activity;

import java.util.HashSet;

/**
 * Created by Admin on 2017/7/25.
 */

public class ActivityUtils {
    public static HashSet<Activity> sActivitys = new HashSet<>();

    public static void put(Activity activity) {
        if (!sActivitys.contains(activity))
            sActivitys.add(activity);
    }

    public static void remove(Activity activity) {
        if (sActivitys.contains(activity))
            sActivitys.remove(activity);
    }

    public static void killAll() {
        if (sActivitys == null || sActivitys.size() == 0) return;
        for (Activity activity : sActivitys) {
            if (activity != null)
                activity.finish();
        }
    }

    public static String getCurrentFragmentName(BaseFragment baseFragment) {
        return baseFragment.getClass().getSimpleName();
    }
}
