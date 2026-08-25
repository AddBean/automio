// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.engineer;

import com.hive.utils.utils.CollectionUtil;

import java.util.ArrayList;
import java.util.List;

public class EngineerObservable {
    private static List<ConfigObserver> sObserverList = new ArrayList<>();

    public interface ConfigObserver {
        void applyConfig(EngineerConfig config);
    }

    public static void notifyApplyConfig(EngineerConfig config) {
        if (CollectionUtil.empty(sObserverList)) return;
        for (int i = 0; i < sObserverList.size(); i++) {
            sObserverList.get(i).applyConfig(config);
        }
    }

    public static void registerObserver(ConfigObserver observer) {
        if (sObserverList == null)
            sObserverList = new ArrayList<>();
        if (sObserverList.contains(observer)) return;
        sObserverList.add(observer);
    }

    public static void unregisterObserver(ConfigObserver observer) {
        if (sObserverList == null)
            sObserverList = new ArrayList<>();
        if (!sObserverList.contains(observer)) return;
        sObserverList.remove(observer);
    }
}
