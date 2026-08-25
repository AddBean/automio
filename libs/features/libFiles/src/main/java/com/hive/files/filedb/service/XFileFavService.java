// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.filedb.service;

import android.text.TextUtils;

import com.hive.files.filedb.XFileFav;
import com.hive.files.filedb.XFileFav_Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.io.File;
import java.util.Date;
import java.util.List;

public class XFileFavService {


    public static void add(String path) {
        File file = new File(path);
        XFileFav record = SQLite.select()
                .from(XFileFav.class)
                .where(XFileFav_Table.path.eq(file.getPath()))
                .querySingle();
        if (record == null) {
            record = new XFileFav();
        }
        if (!TextUtils.isEmpty(file.getPath()))
            record.setPath(file.getPath());
        if (!TextUtils.isEmpty(file.getName()))
            record.setName(file.getName());
        record.setAddTime(new Date());
        record.save();
    }

    public static List<XFileFav> list() {
        return SQLite.select()
                .from(XFileFav.class)
                .queryList();
    }

    public static boolean hasAdd(String path) {
        XFileFav record = SQLite.select()
                .from(XFileFav.class)
                .where(XFileFav_Table.path.eq(path))
                .querySingle();
        return record != null;
    }

    public static void remove(String path) {
        XFileFav record = SQLite.select()
                .from(XFileFav.class)
                .where(XFileFav_Table.path.eq(path))
                .querySingle();
        if (record != null) {
            record.delete();
        }
    }

}
