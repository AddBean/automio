// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.schedule

import android.view.View
import android.content.Context
import com.hive.anim.AnimUtils
import com.hive.base.BaseFragment
import com.hive.script.R
import com.hive.timer.ActivityScheduleTimerEditor
import com.hive.timer.event.EditTimerEvent
import com.hive.timer.widget.schedule.ScheduleTimerManagerView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class ScriptFragmentSchedule : BaseFragment() {

    private var layoutTimerActionBar: View? = null
    private var layoutTimerAdd: View? = null
    private var timerManagerView: ScheduleTimerManagerView? = null

    override fun initView() {
        layoutTimerActionBar = view?.findViewById(R.id.layoutTimerActionBar)
        layoutTimerAdd=view?.findViewById(R.id.layoutTimerAdd)
        timerManagerView=view?.findViewById(R.id.timerManagerView)
        EventBus.getDefault().register(this)
        layoutTimerActionBar?.visibility =
            if (arguments?.getBoolean(ARG_SHOW_EMBEDDED_CREATE_ACTION, true) == true) View.VISIBLE
            else View.GONE
        layoutTimerAdd?.setOnClickListener { showCreateTimerDialog() }
    }

    override fun onResume() {
        super.onResume()
        timerManagerView?.refreshList()
    }

    @Subscribe
    fun onEditTimerEvent(e: EditTimerEvent?) {
        ActivityScheduleTimerEditor.start(requireContext(), e?.timer)
    }

    fun showCreateTimerDialog() {
        layoutTimerAdd?.let { AnimUtils.scaleAnim(it) }
        ActivityScheduleTimerEditor.start(requireContext(), null)
    }


    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun getLayoutId() = R.layout.script_fragment_timer

    companion object {
        const val ARG_SHOW_EMBEDDED_CREATE_ACTION = "show_embedded_create_action"
    }
}
