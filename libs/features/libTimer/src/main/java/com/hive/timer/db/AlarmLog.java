// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.db;

import com.hive.annotation.NotProguard;
import com.hive.db.AutoUpgrade;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@AutoUpgrade
@NotProguard
@Table(database = TimerDB.class)
public class AlarmLog extends BaseModel {

    @PrimaryKey(autoincrement = true)
    public int id;

    @AutoUpgrade
    @Column
    public long alarmId;

    @AutoUpgrade
    @Column
    public int logLevel = 0;//0:info,1:warn,2:error

    @AutoUpgrade
    @Column
    public String logTag = "";

    @AutoUpgrade
    @Column
    public Long logTime;

    @AutoUpgrade
    @Column
    public String logInfo = "";

    @AutoUpgrade
    @Column
    public String taskName = "";

    @AutoUpgrade
    @Column
    public String taskJson = "";
}
