// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.filedb;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.Date;

@Table(database = XFileDataBase.class)
public class XFileIndex extends BaseModel {

    @PrimaryKey(autoincrement = true)
    private int id;

    @Column
    private String path;

    @Column
    private String name;

    @Column
    private long fileSize;

    @Column
    private long fileType;

    @Column
    private long lastModified;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getFileType() {
        return fileType;
    }

    public void setFileType(long fileType) {
        this.fileType = fileType;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}
