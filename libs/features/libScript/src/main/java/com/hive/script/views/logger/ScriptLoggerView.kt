// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.logger

import android.annotation.SuppressLint
import android.content.Context
import android.os.Message
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.setting.ScriptSetting
import com.hive.script.views.menu.ScriptControlView
import com.hive.utils.GlobalApp
import com.hive.utils.OnClickFilteListener
import com.hive.utils.WorkHandler
import com.hive.utils.extends.dp
import com.hive.utils.utils.ScreenUtils
import com.hive.utils.utils.StringUtils
import java.util.Date

class ScriptLoggerView : BaseLayout, WorkHandler.IWorkHandler,
    View.OnClickListener,
    ScriptInterpreterObserver.InterpreterExecuteObserver,
    ScriptInterpreterObserver.CommandExecuteObserver,
    ScriptInterpreterObserver.CommandRecordObserver, ScriptInterpreterObserver.LoggerObserver {

    private var mViewHolder: ViewHolder? = null
    private var mHandler: WorkHandler? = null

    internal class ViewHolder(view: View) {
        var mIvMin: ImageView = view.findViewById(R.id.iv_min)
        var mTvSelector: TextView = view.findViewById(R.id.tv_selector)
        var mLoggerView: ScriptLoggerListView = view.findViewById(R.id.logger_list_view)
        var mIvPause: ImageView = view.findViewById(R.id.iv_pause)
        var mIvClear: ImageView = view.findViewById(R.id.iv_clear)
        var mLayoutSelector: LinearLayout = view.findViewById(R.id.layout_selector)
        var mLayoutMenu: RelativeLayout = view.findViewById(R.id.layout_menu)
        var mLayoutMain: RelativeLayout = view.findViewById(R.id.layout_main)
        var mIvMax: View = view.findViewById(R.id.iv_max)
        var mTvFilter: TextView = view.findViewById(R.id.tv_filter)
        var nextView: ScriptLoggerNextView = view.findViewById(R.id.next_view)
    }

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun initView(view: View) {
        mViewHolder = ViewHolder(view)
        mHandler = WorkHandler(this)
        initSelector()
        mViewHolder?.mLoggerView?.setLoggerView(this)
        mViewHolder?.mTvSelector?.setOnClickListener(this)
        mViewHolder?.mIvPause?.setOnClickListener(this)
        mViewHolder?.mIvMin?.setOnClickListener(this)
        mViewHolder?.mIvClear?.setOnClickListener(this)
        mViewHolder?.mIvPause?.isSelected = false
        mViewHolder?.mLayoutMenu?.setOnClickListener(this)
        mViewHolder?.mIvMax?.setOnClickListener(this)

        mViewHolder?.mTvFilter?.setOnClickListener(this)
        minimumLoggerView()
    }


    fun setCurrentMode(curMenuMode: ScriptControlView.MenuMode) {
        mViewHolder?.nextView?.setText(com.hive.i8n.R.string.sc_record_logger_title)
    }

    private fun initSelector() {
        mViewHolder?.mLayoutSelector?.removeAllViews()
        for (i in 0..4) {
            val tv = TextView(context)
            var levelName = ""
            if (i == 0) levelName = "Verbose"
            if (i == 1) levelName = "Debug"
            if (i == 2) levelName = "Info"
            if (i == 3) levelName = "Warn"
            if (i == 4) levelName = "Error"
            tv.text = levelName
            tv.tag = i
            tv.gravity = Gravity.CENTER
            tv.textSize = 12f
            tv.setPadding(0, 4 * DP, 0, 4 * DP)
            tv.setTextColor(-0xfb6701)
            tv.setOnClickListener(object : OnClickFilteListener() {
                override fun throttleClick(view: View) {
                    mViewHolder?.mTvSelector?.text = (view as TextView).text
                    mViewHolder?.mLoggerView?.setLevel((view.getTag() as Int))
                    mViewHolder?.mLayoutSelector?.visibility = GONE
                }
            })
            mViewHolder?.mLayoutSelector?.addView(tv)
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.layout_menu) {
            mViewHolder?.mLayoutMenu?.visibility = GONE
        } else if (v.id == R.id.iv_min) {
            minimumLoggerView()
        } else if (v.id == R.id.iv_max) {
            maximumLoggerView()
        } else if (v.id == R.id.iv_clear) {
            mViewHolder?.mLoggerView?.clear()
        } else if (v.id == R.id.iv_pause) {
            mViewHolder?.mIvPause?.isSelected = mViewHolder?.mIvPause?.isSelected == false
        }
    }


    override fun onCommandExecuteAfter(cmd: ScriptCommand) {
        super.onCommandExecuteAfter(cmd)
    }

    override fun onCommandExecuteBefore(cmd: ScriptCommand) {
        onLogger(cmd, LogType.DEBUG, cmd.getCommandDescribe())
    }

    override fun setVisibility(visibility: Int) {
        if (ScriptSetting.script_setting_show_logger) {
            super.setVisibility(visibility)
        } else {
            super.setVisibility(View.GONE)
        }
    }

    override fun onLogger(script: ScriptCommand?, type: LogType, info: String?) {
        val msg = (info ?: "").trimIndent()
        val header = StringUtils.dateFormatHHMMSS(Date())
        val tag = script?.getCommandName() ?: ""
        val tagIcon = script?.getCommandIcon() ?: R.drawable.sc_logger
        ScriptLoggerListView.DataBean(
            header, tagIcon, tag, msg, type.ordinal
        ).apply {
            log(this)
        }
    }

    override fun onCommandRecordAdded(script: ScriptCommand) {
    }

    override fun onCommandRecordRemoved(script: ScriptCommand) {

    }

    override fun onCommandExecuteWait(cmd: ScriptCommand, delay: Long) {
        post {
            val next = findNextCommand(cmd)
            next ?: return@post
            mViewHolder?.nextView?.nextCommand(next, delay)
        }
    }

    private fun findNextCommand(cmd: ScriptCommand): ScriptCommand? {
        cmd.parentCommand?.let {
            val index = it.commandQueue.indexOf(cmd)
            if (index < it.commandQueue.size - 1) {
                return it.commandQueue.getOrNull(index + 1)
            }
        }
        return null
    }

    override fun onInterpreterStart(cmd: ScriptCommand) {
        super.onInterpreterStart(cmd)
    }

    override fun onInterpreterEnd(cmd: ScriptCommand) {
        super.onInterpreterEnd(cmd)
    }

    override fun handleMessage(msg: Message) {
        if (mViewHolder?.mIvPause?.isSelected == true) return
        val bean = msg.obj as ScriptLoggerListView.DataBean
        mViewHolder?.mLoggerView?.addLog(bean)
    }

    private fun log(bean: ScriptLoggerListView.DataBean) {
        val message = Message.obtain()
        message.what = 0
        message.obj = bean
        mHandler?.sendMessage(message)
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ScriptInterpreterObserver.registerCommandRecordObserver(this)
        ScriptInterpreterObserver.registerCommandObserver(this)
        ScriptInterpreterObserver.registerInterpreterObserver(this)
        ScriptInterpreterObserver.registerLoggerObserver(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ScriptInterpreterObserver.unRegisterCommandRecordObserver(this)
        ScriptInterpreterObserver.unRegisterCommandObserver(this)
        ScriptInterpreterObserver.unRegisterInterpreterObserver(this)
        ScriptInterpreterObserver.unRegisterLoggerObserver(this)
    }

    fun resetDataView() {
        mViewHolder?.mLoggerView?.clear()
    }

    private var currentState: State = State.MAX
    private var oldHeight = GlobalApp.getDimension(R.dimen.sc_logger_max_height).toInt()
    private var oldWidth = ScreenUtils.getScreenWidth()
    private var mMaxHeight = GlobalApp.getDimension(R.dimen.sc_logger_max_height)
    private var layoutParams: WindowManager.LayoutParams? = null
    private val windowManager: WindowManager? = null
    private val Min_Height = 28.dp
    private val Min_Width = 120.dp

    private fun minimumLoggerView() {
        currentState = State.MIN
        oldHeight = height
        oldWidth = width
        layoutParams?.gravity = Gravity.CENTER
        layoutParams?.width = Min_Width
        layoutParams?.height = Min_Height
        windowManager?.updateViewLayout(this, layoutParams)
        mViewHolder?.mIvMin?.isSelected = mViewHolder?.mIvMin?.isSelected == false
        mViewHolder?.mLayoutMain?.visibility = GONE
        mViewHolder?.mIvMax?.visibility = VISIBLE
    }

    private fun maximumLoggerView() {
        translationX = 0f
        currentState = State.MAX
        layoutParams?.gravity = Gravity.CENTER
//        layoutParams?.x = 0
//        layoutParams?.y = ScreenUtils.getScreenHeight()
        layoutParams?.height = oldHeight
        layoutParams?.width = oldWidth
        windowManager?.updateViewLayout(this, layoutParams)
        mViewHolder?.mLayoutMain?.visibility = VISIBLE
        mViewHolder?.mIvMax?.visibility = GONE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (State.CLOSE == currentState) {
            setMeasuredDimension(0, 0)
        } else if (State.MIN == currentState) {
            val minWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                Min_Width,
                MeasureSpec.EXACTLY
            )
            val minHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                Min_Height,
                MeasureSpec.EXACTLY
            )
            super.onMeasure(minWidthMeasureSpec, minHeightMeasureSpec)

        } else {
            if (mMaxHeight > 0) {
                val heightMode = MeasureSpec.getMode(heightMeasureSpec)
                var heightSize = MeasureSpec.getSize(heightMeasureSpec)

                if (heightMode == MeasureSpec.EXACTLY) {
                    heightSize = if (heightSize <= mMaxHeight) heightSize
                    else mMaxHeight.toInt()
                }

                if (heightMode == MeasureSpec.UNSPECIFIED) {
                    heightSize = if (heightSize <= mMaxHeight) heightSize
                    else mMaxHeight.toInt()
                }
                if (heightMode == MeasureSpec.AT_MOST) {
                    heightSize = if (heightSize <= mMaxHeight) heightSize
                    else mMaxHeight.toInt()
                }
                val maxHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    heightSize,
                    heightMode
                )
                super.onMeasure(widthMeasureSpec, maxHeightMeasureSpec)
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        }
    }

    enum class State {
        MIN, MAX, CLOSE
    }

    enum class LogType {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }


    override fun getLayoutId(): Int {
        return R.layout.script_logger_view
    }
}
