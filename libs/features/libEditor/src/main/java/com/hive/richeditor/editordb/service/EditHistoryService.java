// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.editordb.service;

import android.text.TextUtils;

import com.hive.richeditor.editordb.EditHistory;
import com.hive.richeditor.editordb.EditHistory_Table;
import com.hive.utils.utils.CollectionUtil;
import com.raizlabs.android.dbflow.sql.language.OrderBy;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * @author jiadou
 * @email 172111432@qq.com
 * @date 5/1/21
 */
public class EditHistoryService {

    public static void insertOrUpdate(String path) {
        if (path == null) return;
        File file = new File(path);
        EditHistory record = SQLite.select()
                .from(EditHistory.class)
                .where(EditHistory_Table.path.eq(file.getPath()))
                .querySingle();
        if (record == null) {
            record = new EditHistory();
        }
        if (!TextUtils.isEmpty(file.getPath()))
            record.setPath(file.getPath());
        if (!TextUtils.isEmpty(file.getName()))
            record.setName(file.getName());
        record.setAddTime(new Date());
        record.save();
        List<EditHistory> list = SQLite.select()
                .from(EditHistory.class)
                .offset(200)
                .limit(200)
                .queryList();
        if (!CollectionUtil.empty(list)) {
            for (EditHistory history : list) {
                history.delete();
            }
        }
    }

    public static List<EditHistory> list() {
        return SQLite.select()
                .from(EditHistory.class)
                .orderBy(OrderBy.fromNameAlias(EditHistory_Table.addTime.getNameAlias()).descending())
                .queryList();
    }

    public static boolean hasAdd(String path) {
        EditHistory record = SQLite.select()
                .from(EditHistory.class)
                .where(EditHistory_Table.path.eq(path))
                .querySingle();
        return record != null;
    }

    public static void remove(String path) {
        EditHistory record = SQLite.select()
                .from(EditHistory.class)
                .where(EditHistory_Table.path.eq(path))
                .querySingle();
        if (record != null) {
            record.delete();
        }
    }
}
