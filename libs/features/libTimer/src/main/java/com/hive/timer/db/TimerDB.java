// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.db;

import com.hive.annotation.NotProguard;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.sql.SQLiteType;
import com.raizlabs.android.dbflow.sql.migration.AlterTableMigration;
@NotProguard
@Database(name = TimerDB.NAME, version = TimerDB.VERSION)
public class TimerDB {
    public static final String NAME = "timer_db";
    public static final int VERSION = 4;
}