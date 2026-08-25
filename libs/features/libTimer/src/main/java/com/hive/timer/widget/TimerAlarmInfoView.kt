// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.widget


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.hive.base.BaseLayout
import com.hive.extension.visibleOrGone
import com.hive.timer.R
import com.hive.timer.alarm.HiveAlarmFactory
import com.hive.timer.event.UpdateTimerEvent
import com.hive.timer.utils.AlarmTimerUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.Timer

class TimerAlarmInfoView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs) {

    private var timer = Timer()

    private var layoutEmpty: View? = null
    private var layoutNext: View? = null
    private var tvLeft: TextView? = null
    private var tvTime: TextView? = null

    override fun initView(p0: View?) {
        layoutEmpty = findViewById(R.id.layoutEmpty)
        layoutNext = findViewById(R.id.layoutNext)
        tvLeft = findViewById(R.id.tvLeft)
        tvTime = findViewById(R.id.tvTime)
        onCheckAlarm()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        EventBus.getDefault().register(this)
        timer = Timer()
        timer.schedule(object : java.util.TimerTask() {
            override fun run() {
                post {
                    onCheckAlarm()
                }
            }
        }, 0, 1000)
    }

    private fun onCheckAlarm() {
        HiveAlarmFactory.getDefault().getNextAlarmDate()?.run {
            layoutNext?.visibleOrGone(true)
            layoutEmpty?.visibleOrGone(false)
            tvLeft?.text = AlarmTimerUtils.formatHHMMSSTime(this.time - System.currentTimeMillis())
            tvTime?.text = AlarmTimerUtils.dateFormat(this)
        } ?: run {
            layoutNext?.visibleOrGone(false)
            layoutEmpty?.visibleOrGone(true)
        }
    }

    @Subscribe
    fun onUpdateTimerEvent(e: UpdateTimerEvent) {
        onCheckAlarm()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        EventBus.getDefault().unregister(this)
        timer.cancel()
    }

    override fun getLayoutId() = R.layout.timer_alarm_info_view
}