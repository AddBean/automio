// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.os.Message
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.base.ScriptRecordHelper
import com.hive.utils.WorkHandler

class RecordReplayView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs),
    WorkHandler.IWorkHandler {

    private var mHandler = WorkHandler(this)
    private var mTimeSec = 0

    override fun initView(view: View?) {

    }

    override fun handleMessage(msg: Message?) {
        mHandler.sendEmptyMessageDelayed(1, 1000)
        mTimeSec++
        updateProgress()
    }

    fun startPlay() {
        mTimeSec = 0
        mHandler.removeMessages(1)
        mHandler.sendEmptyMessageDelayed(1, 1000)
        updateProgress()
    }

    fun pausePlay() {
        mHandler.removeMessages(1)
    }

    fun stopPlay() {
        mTimeSec = 0
        mHandler.removeMessages(1)
    }

    @SuppressLint("SetTextI18n")
    fun updateProgress() {
        val pr = ScriptRecordHelper.instance.getTotalCommandCount()
        findViewById<TextView>(R.id.tv_loop)?.text = "${pr.second + 1}/${pr.first}"
    }

    override fun getLayoutId() = R.layout.record_replay_view
}