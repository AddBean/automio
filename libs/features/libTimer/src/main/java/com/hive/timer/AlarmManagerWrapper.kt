// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer

import android.content.Intent
import com.hive.timer.alarm.HiveAlarmFactory
import com.hive.timer.db.AlarmClock
import com.hive.timer.db.AlarmDbService
import com.hive.utils.debug.DLog
import com.hive.utils.utils.GsonHelper
import java.util.Calendar


/**
 *
 * @author jiadou
 * @date 7/30/21
 */
class AlarmManagerWrapper {

    /**
     * 增加一个定时闹钟
     */
    fun addAlarm(alarm: AlarmClock) {
        deleteAlarm(alarm.alarmId)
        val entity = GsonHelper.getInstance().fromJson(alarm.alarmJson, AlarmEntity::class.java)
        entity.getEnableAlarmList()?.forEach {
            val intent = Intent()
            intent.putExtra("entity", entity)
            val requestId = generateAlarmId(entity.alarmId!!, it.id!!)
            DLog.e("AlarmManagerWrapper", "addAlarm requestId:$requestId entity: $entity")
            HiveAlarmFactory.getDefault()
                .setRepeating(it.triggerAtTime, it.repeatInterval, requestId, intent)
        }
    }

    /**
     * 删除一个定时
     */
    fun deleteAlarm(alarmId: Long) {
        cleanDeletedAlarm()
        val alarm = AlarmDbService.get(alarmId) ?: return
        val entity = GsonHelper.getInstance().fromJson(alarm.alarmJson, AlarmEntity::class.java)
        entity.getAllAlarmList()?.forEach {
            val requestId = generateAlarmId(entity.alarmId!!, it.id!!)
            DLog.e("AlarmManagerWrapper", "deleteAlarm requestId:$requestId entity: $entity")
            HiveAlarmFactory.getDefault().cancel(requestId)
        }
    }

    /**
     * 清理已经删除的定时
     */
    fun cleanDeletedAlarm() {
        val alarmDbList = AlarmDbService.list()
        val requestIdList = mutableListOf<Long>()
        alarmDbList?.forEach {
            val entity = GsonHelper.getInstance().fromJson(it.alarmJson, AlarmEntity::class.java)
            entity.getAllAlarmList()?.forEach {
                val requestId = generateAlarmId(entity.alarmId!!, it.id!!)
                requestIdList.add(requestId)
            }
        }
        val curRequestIdList = HiveAlarmFactory.getDefault().getRequestIdList()
        //如果数据库中的定时在系统中不存在则删除
        curRequestIdList.forEach {
            if (!requestIdList.contains(it)) {
                HiveAlarmFactory.getDefault().cancel(it)
            }
        }
    }


    private fun generateAlarmId(alarmId: Long, itemId: Long): Long {
        // Use bit-shifting to avoid collisions: high 32 bits = alarmId, low 32 bits = itemId
        return (alarmId shl 32) or (itemId and 0xFFFFFFFFL)
    }

    companion object {

        val First_Day_Of_Week = Calendar.MONDAY

        val instance: AlarmManagerWrapper by lazy {
            AlarmManagerWrapper()
        }

        @JvmStatic
        fun get() = instance

    }
}