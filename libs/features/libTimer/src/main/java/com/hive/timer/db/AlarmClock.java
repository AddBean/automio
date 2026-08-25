// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.db;

import com.hive.annotation.NotProguard;
import com.hive.db.AutoUpgrade;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.io.Serializable;

@AutoUpgrade
@NotProguard
@Table(database = TimerDB.class)
public class AlarmClock extends BaseModel implements Serializable {

    @PrimaryKey(autoincrement = true)
    public int id;
    @AutoUpgrade
    @Column
    public long alarmId;
    @AutoUpgrade
    @Column
    public boolean alarmOn = true;
    @AutoUpgrade
    @Column
    public String alarmJson = "";
    @AutoUpgrade
    @Column
    public int alarmType = -1;
    @AutoUpgrade
    @Column
    public String alarmDes = "";
    @AutoUpgrade
    @Column
    public String taskInfo = "";


}
