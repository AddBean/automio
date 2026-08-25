// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer

import com.hive.annotation.NotProguard
import com.hive.utils.GlobalApp
import com.hive.utils.utils.GsonHelper
import com.hive.timer.utils.TimerIdGenerator
import java.io.Serializable

/**
 *
 * @author jiadou
 * @date 7/30/21
 */
@NotProguard
class AlarmEntity : Serializable {

    var alarmId: Long? = null

    var enable = false//是否启用

    var enableType: AlarmType = AlarmType.DAILY //0每天 1按星期 2按周期

    var alarmList: MutableList<AlarmTime>? = null

    var taskInfo: String? = null

    class AlarmTime : Serializable {

        var id: Long? = TimerIdGenerator.newId()

        var enable = true//是否启用

        var type: AlarmType = AlarmType.DAILY //0每天 1按星期 2按周期

        var week: Int = 0  //0周日,1周一,2周二,3周三,4周四,5周五,6周六

        var triggerAtTime: Long = 0//UTC时间 使用System.currentTimeMillis()获取

        var repeatInterval: Long = 0//间隔时间

        override fun toString(): String {
            return GsonHelper.getInstance().toJson(this)
        }
    }

    enum class AlarmType(val index: Int, var info: String) {
        DAILY(0, GlobalApp.getStringArray(com.hive.i8n.R.array.schedule_menu_repeat_array)[0]),
        WEEKLY(1, GlobalApp.getStringArray(com.hive.i8n.R.array.schedule_menu_repeat_array)[1]),
        REPEAT(2, GlobalApp.getStringArray(com.hive.i8n.R.array.schedule_menu_repeat_array)[2]);

        fun toInt(): Int {
            return index
        }

        override fun toString(): String {
            return info
        }

    }

    override fun toString(): String {
        return GsonHelper.getInstance().toJson(this)
    }

    fun getEnableAlarmList(): List<AlarmTime>? {
        return alarmList?.filter { it.enable && it.type == enableType }
    }

    fun getAllAlarmList(): List<AlarmTime>? {
        return alarmList
    }

    /**
     * 查找最近的一个闹钟
     */
    fun findNextLastAlarm(): Long? {
        val alarmList = getEnableAlarmList().takeIf { it?.size ?: 0 > 0 } ?: return null
        var triggerAtTimeInFuture = 0L
        val sortedList = alarmList.sortedBy { it.triggerAtTime }
        var paddingTime = 0L
        while (triggerAtTimeInFuture < System.currentTimeMillis()) {
            sortedList.forEach {
                triggerAtTimeInFuture =
                    it.triggerAtTime + paddingTime
                if (triggerAtTimeInFuture > System.currentTimeMillis()) {
                    return@findNextLastAlarm triggerAtTimeInFuture
                }
            }
            paddingTime += sortedList.firstOrNull()?.repeatInterval ?: 0
        }
        return triggerAtTimeInFuture
    }

    fun deepCopy(): AlarmEntity {
        return GsonHelper.getInstance()
            .fromJson(GsonHelper.getInstance().toJson(this), AlarmEntity::class.java)
    }
}
