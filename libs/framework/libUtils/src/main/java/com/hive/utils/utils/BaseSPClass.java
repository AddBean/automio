// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import com.hive.utils.GlobalApp;
import com.hive.utils.debug.DLog;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jiadou
 * @date 4/8/21
 */
public abstract class BaseSPClass {

    private static Map<String, Object> mCacheMap = new HashMap<>();

    public synchronized void save() {
        PreferencesUtils.saveObj(GlobalApp.sContext, getSaveName(), this, null);
        mCacheMap.put(getSaveName(), this);
        DLog.d(this);
    }

    protected abstract String getSaveName();


    public synchronized static <T extends BaseSPClass> T restore(T target) {
        target.save();
        mCacheMap.put(target.getSaveName(), target);
        return target;
    }


    public synchronized static <T extends BaseSPClass> T read(T target) {
        T cache = (T) mCacheMap.get(target.getSaveName());
        if (cache != null) {
            return cache;
        }
        T config = (T) PreferencesUtils.getObj(GlobalApp.sContext, target.getSaveName(), target.getClass(), null);
        if (config == null) {
            config = target;
        }
        mCacheMap.put(target.getSaveName(), config);
        config.save();
        return config;
    }

}
