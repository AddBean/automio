// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.impl

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.hive.script.R
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdPinch
import com.hive.script.views.record.ScriptRecordBaseView
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.record.handler.ScriptMultipleHandler
import com.hive.script.views.widgets.ScriptMultipleView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.color
import com.hive.utils.utils.ScreenUtils
import com.hive.views.popmenu.PopMenuManager

/**
 *
 * @author jiadou
 * @date 7/16/21
 */
class ScriptRecordMultipleView(context: Context, attributeSet: AttributeSet) :
    ScriptRecordBaseView(context, attributeSet) {

    private var mScriptMultipleView = ScriptMultipleView(context, null).apply {
        bottomView.addView(
            this,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    override fun getViewTypes() = mutableListOf(
        ScriptRecordViewManager.RecordViewType.MULTIPLE
    )

    override fun getEventHandler() = ScriptMultipleHandler(this)

    override fun getCtrView(): View {
        return ControlView(context)
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.drawColor(com.hive.i8n.R.color.recordOptBg.color())
        super.dispatchDraw(canvas)
    }

    override fun onShow() {
        super.onShow()
        val sw = ScreenUtils.getScreenWidth()
        val sh = ScreenUtils.getScreenHeight()
        val x1 = sw / 2
        val x2 = sw / 2
        val y1 = sh / 4
        val y2 = sh - sh / 4
        mScriptMultipleView.loadCmd(
            CmdPinch.createCommand(
                1,
                80 * dp,
                ScriptConst.Cmd_Click_Multiple_Default,
                x1,
                y1 + 100,
                x2,
                y2 - 100
            )
        )
    }

    override fun getLayoutName(): String {
        return GlobalApp.getString(com.hive.i8n.R.string.sc_spot_layout_name_5)
    }

    inner class ControlView(context: Context) : FrameLayout(context) {

        private var mBtnSubmit: Button? = null
        private var mTvCount: TextView? = null


        val view = LayoutInflater.from(context)
            .inflate(R.layout.script_multiple_control_view, this@ControlView)

        override fun onConfigurationChanged(newConfig: Configuration?) {
            super.onConfigurationChanged(newConfig)
            post { adjustPosition() }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            post { adjustPosition() }
        }

        private fun adjustPosition() {
            mBtnSubmit = view.findViewById(R.id.btn_submit)
            mTvCount = view.findViewById(R.id.tv_count)
            mBtnSubmit?.setOnClickListener {
                baseEventHandler?.notifyEvent(ScriptRecordEventHandler.RecordResultAction.ACTION_MULTIPLE, mScriptMultipleView.mCmdMultiple)
            }
            mTvCount?.text = GlobalApp.getResources().getStringArray(com.hive.i8n.R.array.sc_finger_count)[0]
            mTvCount?.setOnClickListener {
                val ls = GlobalApp.getResources().getStringArray(com.hive.i8n.R.array.sc_finger_count).toList()
                PopMenuManager.instance.showMenu(
                    mTvCount!!,
                    0,
                    10 * dp,
                    ls,
                    object : PopMenuManager.OnItemClickListener<String> {
                        override fun onItemClicked(view: View, data: String, pos: Int) {
                            when (pos) {
                                0 -> {
                                    mScriptMultipleView.changeFingerCount(1)
                                }

                                1 -> {
                                    mScriptMultipleView.changeFingerCount(2)
                                }

                                2 -> {
                                    mScriptMultipleView.changeFingerCount(3)
                                }

                                3 -> {
                                    mScriptMultipleView.changeFingerCount(4)
                                }
                                4 -> {
                                    mScriptMultipleView.changeFingerCount(5)
                                }
                            }
                            mTvCount?.text = data
                        }
                    })
            }
            mScriptMultipleView.onCommandLoadListener =
                object : ScriptMultipleView.OnCommandLoadListener {
                    override fun onCommandLoaded(cmd: CmdPinch) {
                        val ls = GlobalApp.getResources().getStringArray(com.hive.i8n.R.array.sc_finger_count)
                            .toList()
                        mTvCount?.text = ls[cmd.fingerCount - 1]
                    }
                }
        }
    }

}