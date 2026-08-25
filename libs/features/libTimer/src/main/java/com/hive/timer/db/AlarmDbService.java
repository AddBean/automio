// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.db;

import com.hive.timer.AlarmEntity;
import com.hive.timer.AlarmManagerWrapper;
import com.hive.timer.TimerProvider;
import com.hive.timer.utils.TimerLogger;
import com.hive.utils.debug.DLog;
import com.raizlabs.android.dbflow.sql.language.OrderBy;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.List;

public class AlarmDbService {
    private static void ensureDb() {
        TimerProvider.ensureDbInitialized();
    }

    public static AlarmClock get(long alarmId) {
        ensureDb();
        AlarmClock record = SQLite.select().from(AlarmClock.class).where(AlarmClock_Table.alarmId.eq(alarmId)).querySingle();
        if (record == null) return null;
        return record;
    }

    public static void delete(long alarmId) {
        ensureDb();
        AlarmManagerWrapper.get().deleteAlarm(alarmId);
        AlarmClock record = get(alarmId);
        if (record != null) {
            record.delete();
        }
    }

    public static void saveAlarmEntity(AlarmEntity entity) {
        ensureDb();
        AlarmClock record = get(entity.getAlarmId());
        if (record == null)
            record = new AlarmClock();
        record.alarmId = entity.getAlarmId();
        record.alarmJson = entity.toString();
        record.alarmOn = entity.getEnable();
        record.taskInfo = entity.getTaskInfo();
        record.alarmType = entity.getEnableType().toInt();
        record.save();
        DLog.e("AlarmDbService", "saveAlarmEntity: id=" + record.id + ", alarmId=" + record.alarmId);
        changeAlarm(get(entity.getAlarmId()));
    }

    private static void changeAlarm(AlarmClock alarm) {
        if (alarm == null) return;
        if (alarm.alarmOn) {
            AlarmManagerWrapper.get().deleteAlarm(alarm.alarmId);
            AlarmManagerWrapper.get().addAlarm(alarm);
        } else {
            AlarmManagerWrapper.get().deleteAlarm(alarm.alarmId);
        }
    }

    public static List<AlarmClock> list() {
        ensureDb();
        List<AlarmClock> records = SQLite.select().from(AlarmClock.class).queryList();
        if (records == null) return null;
        return records;
    }

    public static List<AlarmLog> listLog(@TimerLogger.TimerLogLevel int level, Long alarmId) {
        ensureDb();
        List<AlarmLog> records = SQLite.select().from(AlarmLog.class).where(AlarmLog_Table.alarmId.eq(alarmId)).and(AlarmLog_Table.logLevel.greaterThanOrEq(level)).orderBy(OrderBy.fromProperty(AlarmLog_Table.logTime).descending()).queryList();
        if (records == null) return null;
        return records;
    }

    public static void clearLog(Long alarmId) {
        ensureDb();
        SQLite.delete().from(AlarmLog.class).where(AlarmLog_Table.alarmId.eq(alarmId)).query();
    }

}
