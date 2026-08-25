// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.filedb;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.Date;

@Table(database = XFileDataBase.class)
public class XKVCache extends BaseModel {

    @PrimaryKey(autoincrement = true)
    private int id;

    @Column
    private String cacheKey;

    @Column
    private String cacheValue;

    @Column
    private String cacheExt;

    @Column
    private Date updateTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public String getCacheValue() {
        return cacheValue;
    }

    public void setCacheValue(String cacheValue) {
        this.cacheValue = cacheValue;
    }

    public String getCacheExt() {
        return cacheExt;
    }

    public void setCacheExt(String cacheExt) {
        this.cacheExt = cacheExt;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
