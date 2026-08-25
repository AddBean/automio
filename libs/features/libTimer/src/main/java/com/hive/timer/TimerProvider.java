// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer;

import android.content.Context;

import com.hive.annotation.NotProguard;
import com.hive.timer.alarm.HiveAlarmManager;
import com.hive.timer.alarm.HiveAlarmTimer;
import com.hive.timer.db.AlarmClock;
import com.hive.timer.db.AlarmDbService;
import com.hive.utils.GlobalApp;
import com.raizlabs.android.dbflow.config.FlowManager;

import com.raizlabs.android.dbflow.config.alarmTimerGeneratedDatabaseHolder;
import com.hive.timer.broadcast.AlarmMainReceiver;
import com.hive.timer.alarm.HiveAlarmService;

import android.content.IntentFilter;
import android.os.Build;

import java.util.List;

@NotProguard
public class TimerProvider {


    private HiveAlarmTimer alarmTimer = new HiveAlarmTimer();
    private static volatile boolean dbInitialized = false;
    private volatile boolean receiverRegistered = false;

    private TimerProvider() {
    }

    /**
     * DBFlow 模块必须在查询 AlarmClock 前注册。
     * Component 初始化在后台线程，主界面可能更早访问数据库，因此做幂等、线程安全初始化。
     */
    public static void ensureDbInitialized() {
        if (dbInitialized) return;
        synchronized (TimerProvider.class) {
            if (dbInitialized) return;
            FlowManager.initModule(alarmTimerGeneratedDatabaseHolder.class);
            dbInitialized = true;
        }
    }

    public void init(Context context) {
        ensureDbInitialized();
        startService();
        if (receiverRegistered) return;
        synchronized (this) {
            if (receiverRegistered) return;
            IntentFilter filter = new IntentFilter(HiveAlarmManager.Companion.getAlarm_Action());
            filter.addAction(HiveAlarmManager.Companion.getBefore_Alarm_Action());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                GlobalApp.getApp().registerReceiver(new AlarmMainReceiver(), filter, Context.RECEIVER_EXPORTED);
            } else {
                GlobalApp.getApp().registerReceiver(new AlarmMainReceiver(), filter);
            }
            receiverRegistered = true;
        }
    }

    public void startService() {
        // Check if exact alarms are allowed (Android 12+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) GlobalApp.getApp().getSystemService(android.content.Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                // Request permission to schedule exact alarms
                android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(android.net.Uri.parse("package:" + GlobalApp.getApp().getPackageName()));
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    GlobalApp.getApp().startActivity(intent);
                } catch (Exception e) {
                    // Handle case where settings activity is not available
                }
                return;
            }
        }
        
        if (!HiveAlarmService.start(GlobalApp.getApp())) {
            alarmTimer.start(null);
        }
    }


    public void autoRegisterAlarm() {
        List<AlarmClock> list = AlarmDbService.list();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).alarmOn) {
                AlarmManagerWrapper.get().addAlarm(list.get(i));
            } else {
                AlarmManagerWrapper.get().deleteAlarm(list.get(i).alarmId);
            }
        }
    }


    private static TimerProvider instance = new TimerProvider();

    public static TimerProvider getInstance() {
        return instance;
    }
}
