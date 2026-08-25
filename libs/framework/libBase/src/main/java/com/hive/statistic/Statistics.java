// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.statistic;

import com.hive.utils.debug.DLog;

import java.util.Map;

public class Statistics {
    private static class SingleHolder {
        static Statistics instance = new Statistics();
    }

    public static Statistics getInstance() {
        return SingleHolder.instance;
    }

    public void onEvent(String key, Map<String, String> map) {
        if (DLog.isDebug())
            DLog.d("onEvent " + key, map);
    }

}
