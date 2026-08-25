// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.filedb.service;

import android.text.TextUtils;

import com.hive.files.filedb.XFileHistory;
import com.hive.files.filedb.XFileHistory_Table;
import com.hive.files.model.XFileSetting;
import com.hive.utils.utils.CollectionUtil;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class XFileHistoryService {


    public static void add(String path) {
        File file = new File(path);
        //如果不记录文件
        if (XFileSetting.Companion.getInstance().getDisableRecordFile()) {
            return;
        }
        XFileHistory record = SQLite.select()
                .from(XFileHistory.class)
                .where(XFileHistory_Table.path.eq(file.getPath()))
                .querySingle();
        if (record == null) {
            record = new XFileHistory();
        }
        if (!TextUtils.isEmpty(file.getPath()))
            record.setPath(file.getPath());
        if (!TextUtils.isEmpty(file.getName()))
            record.setName(file.getName());
        record.setAddTime(new Date());
        record.save();
        List<XFileHistory> list = SQLite.select()
                .from(XFileHistory.class)
                .offset(500)
                .limit(500)
                .queryList();
        if (!CollectionUtil.empty(list)) {
            for (XFileHistory history : list) {
                history.delete();
            }
        }
    }

    public static List<XFileHistory> list() {
        return SQLite.select()
                .from(XFileHistory.class)
                .queryList();
    }

    public static boolean hasAdd(String path) {
        XFileHistory record = SQLite.select()
                .from(XFileHistory.class)
                .where(XFileHistory_Table.path.eq(path))
                .querySingle();
        return record != null;
    }

    public static void remove(String path) {
        XFileHistory record = SQLite.select()
                .from(XFileHistory.class)
                .where(XFileHistory_Table.path.eq(path))
                .querySingle();
        if (record != null) {
            record.delete();
        }
    }

}
