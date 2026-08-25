// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.filedb;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.Date;

@Table(database = XFileDataBase.class)
public class XFileRecycleBin extends BaseModel {

    @PrimaryKey(autoincrement = true)
    private int id;

    @Column
    private String originPath;

    @Column
    private String recyclerKey;

    @Column
    private String fileName;

    @Column
    private Date delTime;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOriginPath() {
        return originPath;
    }

    public void setOriginPath(String originPath) {
        this.originPath = originPath;
    }

    public String getRecyclerKey() {
        return recyclerKey;
    }

    public void setRecyclerKey(String recyclerKey) {
        this.recyclerKey = recyclerKey;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Date getDelTime() {
        return delTime;
    }

    public void setDelTime(Date delTime) {
        this.delTime = delTime;
    }
}
