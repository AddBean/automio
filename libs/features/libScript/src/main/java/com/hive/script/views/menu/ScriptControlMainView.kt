// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.menu

import android.content.Context
import android.os.Message
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import com.hive.base.BaseLayout
import com.hive.extension.isVisible
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptMenuManager
import com.hive.script.views.menu.contents.ScriptAgentContentView
import com.hive.script.views.menu.contents.ScriptRecordContentView
import com.hive.script.views.menu.contents.ScriptWorkflowContentView
import com.hive.utils.WorkHandler
import com.hive.utils.utils.ScreenUtils
import com.hive.views.widgets.TextDrawableView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2021/10/14
 */
class ScriptControlMainView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs),
    View.OnClickListener, WorkHandler.IWorkHandler {

    private var isOpen = true
    private var currentTab = 0 // 0: 智能体, 1: 录制, 2: 工作流

    private var btn_back: View? = null
    private var btn_close: View? = null
    private var btn_drawer_left: View? = null
    private var btn_drawer_right: View? = null
    private var btn_mini: View? = null

    private var main_menu: View? = null

    // Tab 相关
    private var tab_agent: TextDrawableView? = null
    private var tab_record: TextDrawableView? = null
    private var tab_workflow: TextDrawableView? = null
    private var content_agent: ScriptAgentContentView? = null
    private var content_record: ScriptRecordContentView? = null
    private var content_workflow: ScriptWorkflowContentView? = null

    var parentControl: ScriptControlView? = null

    var clickEditListener: (() -> Unit)? = null

    override fun initView(view: View?) {
        btn_back = findViewById(R.id.btn_back)
        btn_close = findViewById(R.id.btn_close)
        btn_drawer_left = findViewById(R.id.btn_drawer_left)
        btn_drawer_right = findViewById(R.id.btn_drawer_right)
        btn_mini = findViewById(R.id.btn_mini)

        main_menu = findViewById(R.id.main_menu)

        // Tab 相关
        tab_agent = findViewById(R.id.tab_agent)
        tab_record = findViewById(R.id.tab_record)
        tab_workflow = findViewById(R.id.tab_workflow)
        content_agent = findViewById(R.id.content_agent)
        content_record = findViewById(R.id.content_record)
        content_workflow = findViewById(R.id.content_workflow)


        btn_back?.setOnClickListener(this)
        btn_close?.setOnClickListener(this)

        btn_drawer_left?.setOnClickListener(this)
        btn_drawer_right?.setOnClickListener(this)
        btn_mini?.setOnClickListener(this)

        // Tab 点击事件
        tab_agent?.setOnClickListener(this)
        tab_record?.setOnClickListener(this)
        tab_workflow?.setOnClickListener(this)

        post {
            updateCurrentStatus()
            addInitialAnimation() // 添加初始动画
            switchTab(0) // 默认显示智能体 tab
        }
    }


    /**
     * 添加初始动画效果 - 简化版本
     */
    private fun addInitialAnimation() {
        main_menu?.let { menu ->
            val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)
            fadeIn.duration = 300
            menu.startAnimation(fadeIn)
        }
    }

    /**
     * 添加按钮点击动画 - 简化版本
     */
    private fun addButtonClickAnimation(view: View?) {
        view?.let { v ->
            val alphaAnimation = android.view.animation.AlphaAnimation(1.0f, 0.7f)
            alphaAnimation.duration = 100
            alphaAnimation.fillAfter = true

            val alphaBack = android.view.animation.AlphaAnimation(0.7f, 1.0f)
            alphaBack.duration = 100
            alphaBack.startOffset = 100

            val animationSet = android.view.animation.AnimationSet(true)
            animationSet.addAnimation(alphaAnimation)
            animationSet.addAnimation(alphaBack)

            v.startAnimation(animationSet)
        }
    }


    override fun onClick(v: View?) {
        // 添加按钮点击动画
        addButtonClickAnimation(v)

        when (v?.id) {
            R.id.btn_mini -> {
                isOpen = false
            }

            R.id.btn_record_start -> {
                // 录制按钮点击事件由 ScriptRecordContentView 处理
                content_record?.let { _ ->
                    // 这里可以添加额外的录制逻辑
                }
            }

            R.id.btn_back -> {
                ScriptEventHelper.get().performBackToApp()
            }

            R.id.btn_close -> {
                ScriptManager.stopPlay()
                ScriptMenuManager.hiddenMenuView()
            }

            R.id.tvBtnEdit -> {
                clickEditListener?.invoke()
            }

            R.id.btn_drawer_right, R.id.btn_drawer_left -> {
                isOpen = !isOpen
            }

            // Tab 切换
            R.id.tab_agent -> {
                switchTab(0)
            }

            R.id.tab_record -> {
                switchTab(1)
            }

            R.id.tab_workflow -> {
                switchTab(2)
            }
        }
        updateCurrentStatus {
            if (isAttachedToWindow.not()) return@updateCurrentStatus
            closeToEdge(it)
        }
        startAutoHidden()
    }

    /**
     * 切换 Tab
     */
    private fun switchTab(tabIndex: Int) {

        currentTab = tabIndex

        // 重置所有 tab 样式
        resetTabStyles()

        // 隐藏所有内容
        content_agent?.visibility = View.GONE
        content_record?.visibility = View.GONE
        content_workflow?.visibility = View.GONE

        when (tabIndex) {
            0 -> {
                // 智能体
                tab_agent?.setBackgroundResource(R.drawable.xml_tab_active_bg)
                tab_agent?.setTextColor(getColor(com.hive.i8n.R.color.white))
                tab_agent?.setDrawableColor(getColor(com.hive.i8n.R.color.white))
                content_agent?.visibility = View.VISIBLE
                addContentAnimation(content_agent)
            }

            1 -> {
                // 录制
                tab_record?.setBackgroundResource(R.drawable.xml_tab_active_bg)
                tab_record?.setTextColor(getColor(com.hive.i8n.R.color.white))
                tab_record?.setDrawableColor(getColor(com.hive.i8n.R.color.white))
                content_record?.visibility = View.VISIBLE
                addContentAnimation(content_record)
            }

            2 -> {
                // 工作流
                tab_workflow?.setBackgroundResource(R.drawable.xml_tab_active_bg)
                tab_workflow?.setTextColor(getColor(com.hive.i8n.R.color.white))
                tab_workflow?.setDrawableColor(getColor(com.hive.i8n.R.color.white))
                content_workflow?.visibility = View.VISIBLE
                addContentAnimation(content_workflow)
            }
        }
    }

    /**
     * 添加内容切换动画 - 简化版本
     */
    private fun addContentAnimation(view: View?) {
        view?.let { v ->
            val fadeIn = android.view.animation.AlphaAnimation(0.0f, 1.0f)
            fadeIn.duration = 200
            v.startAnimation(fadeIn)
        }
    }

    /**
     * 重置所有 tab 样式
     */
    private fun resetTabStyles() {
        tab_agent?.setBackgroundResource(R.drawable.xml_tab_inactive_bg)
        tab_agent?.setTextColor(getColor(com.hive.i8n.R.color.colorTextSecondary))
        tab_agent?.setDrawableColor(getColor(com.hive.i8n.R.color.tech_cyan))

        tab_record?.setBackgroundResource(R.drawable.xml_tab_inactive_bg)
        tab_record?.setTextColor(getColor(com.hive.i8n.R.color.colorTextSecondary))
        tab_record?.setDrawableColor(getColor(com.hive.i8n.R.color.tech_cyan))

        tab_workflow?.setBackgroundResource(R.drawable.xml_tab_inactive_bg)
        tab_workflow?.setTextColor(getColor(com.hive.i8n.R.color.colorTextSecondary))
        tab_workflow?.setDrawableColor(getColor(com.hive.i8n.R.color.tech_cyan))
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (this.isVisible()) {
            startAutoHidden()
        }
    }

    private fun closeToEdge(left: Boolean) {
        this.post {
//            cancelAutoHidden()
            parentControl?.backToEdge(left)
        }
    }

    fun updateCurrentStatus(afterStatusDone: (Boolean) -> Unit = {}) {
        val left =
            (parent as ScriptControlView).mTransX < ScreenUtils.getScreenWidth() / 2 - measuredWidth / 2
        btn_drawer_right?.visibleOrGone(!isOpen && !left)
        btn_drawer_left?.visibleOrGone(!isOpen && left)

        // 修改自动收起逻辑：收起时只隐藏内容区域，保留 tab 和控制按钮
        if (isOpen) {
            main_menu?.visibility = View.VISIBLE
        } else {
            main_menu?.visibility = View.GONE
        }

        post {
            afterStatusDone.invoke(left)
        }
    }


    private val handler = WorkHandler(this)

    private val Msg_Hidden_Flag = -1

    fun cancelAutoHidden() {
        handler.removeMessages(Msg_Hidden_Flag)
    }

    fun startAutoHidden() {
        handler.removeMessages(Msg_Hidden_Flag)
        post {
            if (this.isVisible()) {
                handler.sendEmptyMessageDelayed(Msg_Hidden_Flag, 6 * 1000)
            }
        }
    }

    override fun handleMessage(msg: Message?) {
        if (msg?.what == Msg_Hidden_Flag) {
            if (this.isVisible() && isOpen) {
                isOpen = false
                updateCurrentStatus {
                    if (isAttachedToWindow.not()) return@updateCurrentStatus
                    closeToEdge(it)
                }
            }
        }
    }

    override fun getLayoutId() = R.layout.script_control_main_view

}