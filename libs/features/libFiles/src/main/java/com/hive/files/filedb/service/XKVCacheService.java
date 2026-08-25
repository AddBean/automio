// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.filedb.service;

import android.text.TextUtils;

import com.hive.files.filedb.XKVCache;
import com.hive.files.filedb.XKVCache_Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.Date;

public class XKVCacheService {

    public static void put(String key, String value) {
        put(key, value, null);
    }

    public static void putExt(String key, String ext) {
        put(key, null, ext);
    }

    public static void put(String key, String value, String ext) {
        XKVCache record = SQLite.select()
                .from(XKVCache.class)
                .where(XKVCache_Table.cacheKey.eq(key))
                .querySingle();
        if (record == null) {
            record = new XKVCache();
        }
        record.setCacheKey(key);
        if (!TextUtils.isEmpty(value))
            record.setCacheValue(value);
        if (!TextUtils.isEmpty(ext))
            record.setCacheExt(ext);
        record.setUpdateTime(new Date());
        record.save();
    }

    public static String get(String key) {
        XKVCache record = SQLite.select()
                .from(XKVCache.class)
                .where(XKVCache_Table.cacheKey.eq(key))
                .querySingle();
        if (record == null)
            return null;
        return record.getCacheValue();
    }

    public static String getExt(String key) {
        XKVCache record = SQLite.select()
                .from(XKVCache.class)
                .where(XKVCache_Table.cacheKey.eq(key))
                .querySingle();
        if (record == null)
            return null;
        return record.getCacheExt();
    }

    public static void remove(String key) {
        XKVCache record = SQLite.select()
                .from(XKVCache.class)
                .where(XKVCache_Table.cacheKey.eq(key))
                .querySingle();
        if (record != null) {
            record.delete();
        }
    }

    public static boolean hasKey(String key) {
        XKVCache record = SQLite.select()
                .from(XKVCache.class)
                .where(XKVCache_Table.cacheKey.eq(key))
                .querySingle();
        return record != null;
    }
}
