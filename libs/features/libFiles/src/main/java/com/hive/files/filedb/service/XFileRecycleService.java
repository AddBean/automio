// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.filedb.service;

import com.hive.files.filedb.XFileRecycleBin;
import com.hive.files.filedb.XFileRecycleBin_Table;
import com.hive.utils.system.CommonUtils;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.io.File;
import java.util.Date;
import java.util.List;

public class XFileRecycleService {


    public static XFileRecycleBin add(String path) {
        File file = new File(path);
        String randomName = CommonUtils.getRandomName();
        XFileRecycleBin record = new XFileRecycleBin();
        if (file.getParentFile() != null)
            record.setOriginPath(file.getParentFile().getPath());
        else
            record.setOriginPath("/");
        record.setRecyclerKey(randomName);
        record.setFileName(file.getName());
        record.setDelTime(new Date());
        record.save();
        return record;
    }

    public static List<XFileRecycleBin> list() {
        return SQLite.select()
                .from(XFileRecycleBin.class)
                .queryList();
    }


    public static XFileRecycleBin get(String path) {
        XFileRecycleBin record = SQLite.select()
                .from(XFileRecycleBin.class)
                .where(XFileRecycleBin_Table.originPath.eq(path))
                .querySingle();
        return record;
    }

    public static XFileRecycleBin getByKey(String key) {
        XFileRecycleBin record = SQLite.select()
                .from(XFileRecycleBin.class)
                .where(XFileRecycleBin_Table.recyclerKey.eq(key))
                .querySingle();
        return record;
    }

    public static void remove(String path) {
        XFileRecycleBin record = SQLite.select()
                .from(XFileRecycleBin.class)
                .where(XFileRecycleBin_Table.originPath.eq(path))
                .querySingle();
        if (record != null) {
            record.delete();
        }
    }

    public static void clear() {
        SQLite.delete()
                .from(XFileRecycleBin.class);
    }
}
