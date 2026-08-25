// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.os.Message
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.hive.anim.AnimUtils
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.utils.WorkHandler
import com.hive.utils.utils.StringUtils

class RecordTimeView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs),
    WorkHandler.IWorkHandler {

    private var mHandler = WorkHandler(this)
    private var mTimeSec = 0
    var isRecoding = true

    override fun initView(view: View?) {

    }

    override fun handleMessage(msg: Message?) {
        updateView()
        mHandler.sendEmptyMessageDelayed(1, 1000)
        mTimeSec++
        if (mTimeSec % 2 == 0) {
            AnimUtils.fadeInAnim(findViewById(R.id.iv_dot), 1000L, null)
        } else {
            AnimUtils.fadeOutAnim(findViewById(R.id.iv_dot), 1000L, null)
        }
    }

    fun stopRecord() {
        mTimeSec = 0
        mHandler.removeMessages(1)
        updateView()
        isRecoding = false
    }

    fun pauseRecord() {
        mHandler.removeMessages(1)
        isRecoding = false
    }


    fun resumeRecord() {
        mHandler.removeMessages(1)
        mHandler.sendEmptyMessageDelayed(1, 1000)
        updateView()
        isRecoding = true
    }

    private fun updateView() {
        findViewById<TextView>(R.id.tv_time)?.text = StringUtils.formatMMSSTime(mTimeSec * 1000L)
    }

    override fun getLayoutId() = R.layout.record_time_view


}