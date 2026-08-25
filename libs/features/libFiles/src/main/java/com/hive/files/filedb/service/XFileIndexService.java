// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.filedb.service;

import android.text.TextUtils;

import com.hive.files.XFileUtils;
import com.hive.files.filedb.XFileIndex;
import com.hive.files.filedb.XFileIndex_Table;
import com.hive.utils.file.MediaFileUtil;
import com.raizlabs.android.dbflow.sql.language.OrderBy;
import com.raizlabs.android.dbflow.sql.language.SQLOperator;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class XFileIndexService {


    public static void add(File file) {
        XFileIndex record = SQLite.select()
                .from(XFileIndex.class)
                .where(XFileIndex_Table.path.eq(file.getPath()))
                .querySingle();
        if (record == null) {
            record = new XFileIndex();
        }

        record.setPath(file.getPath());
        record.setName(file.getName());
        record.setFileSize(file.getTotalSpace());
        record.setFileType(MediaFileUtil.getFileType(file.getPath()).fileType);
        record.setLastModified(file.lastModified());
        record.save();
    }

    public static List<XFileIndex> list() {
        return SQLite.select()
                .from(XFileIndex.class)
                .queryList();
    }

    public static boolean hasAdd(String path) {
        XFileIndex record = SQLite.select()
                .from(XFileIndex.class)
                .where(XFileIndex_Table.path.eq(path))
                .querySingle();
        return record != null;
    }

    public static XFileIndex getLastFile() {
        try {
            XFileIndex record = SQLite.select()
                    .from(XFileIndex.class)
                    .orderBy(OrderBy.fromNameAlias(XFileIndex_Table.lastModified.getNameAlias()).descending())
                    .querySingle();
            return record;
        } catch (Exception e) {
            return null;
        }
    }

    public static void remove(String path) {
        XFileIndex record = SQLite.select()
                .from(XFileIndex.class)
                .where(XFileIndex_Table.path.eq(path))
                .querySingle();
        if (record != null) {
            record.delete();
        }
    }

    public static Observable<List<File>> getFileList(final int page, final int pageSize, final SQLOperator... conditions) {
        Observable observable = Observable.fromPublisher(new Publisher<List<File>>() {
            @Override
            public void subscribe(Subscriber<? super List<File>> s) {
                List<XFileIndex> ls = SQLite.select()
                        .from(XFileIndex.class)
                        .where(conditions)
                        .orderBy(OrderBy.fromNameAlias(XFileIndex_Table.lastModified.getNameAlias()).descending())
                        .offset(page * pageSize)
                        .limit(pageSize)
                        .queryList();
                List<File> fs = new ArrayList<>();
                for (int i = 0; i < ls.size(); i++) {
                    fs.add(new File(ls.get(i).getPath()));
                }
                s.onNext(fs);
                s.onComplete();
            }
        }).subscribeOn(Schedulers.io());
        return observable;
    }
}
