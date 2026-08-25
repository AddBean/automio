// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandRoot
import com.hive.utils.GlobalApp
import com.hive.views.widgets.TextDrawableView

/**
 *
 * @author jiadou
 * @date 4/7/21
 */
class ScriptNavigationBar(context: Context, attrs: AttributeSet) : BaseLayout(context, attrs) {

    private var mCurrentCmd: ScriptCommand? = null

    var mNavigationListener: INavigationListener? = null

    private var layout_content: ViewGroup? = null

    private var scroll_view: View? = null

    override fun getLayoutId(): Int = R.layout.cmd_navigation_bar

    override fun initView(view: View?) {
        layout_content = findViewById(R.id.layout_content)
        scroll_view = findViewById(R.id.scroll_view)
    }

    fun updateBar(cmd: ScriptCommand?) {
        mCurrentCmd = cmd
        val list = mutableListOf<ScriptCommand>()

        while (mCurrentCmd?.parentCommand != null) {
            list.add(mCurrentCmd!!)
            mCurrentCmd = mCurrentCmd!!.parentCommand
        }
        list.reverse()
        list.add(0, ScriptCommandRoot())
        layout_content?.removeAllViews()
        var lastView: NavigationItemView? = null
        list.forEach { cmd ->
            lastView = NavigationItemView().apply {
                if (cmd is ScriptCommandRoot) {
                    text = getString(com.hive.i8n.R.string.sc_nav_root_cmd)
                    tag = cmd
                } else {
                    text = cmd.getCommandName()
                    tag = cmd
                }
                setLastStatus(false)
            }
            layout_content?.addView(
                lastView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        lastView?.setLastStatus(true)
        layout_content?.post {
            scroll_view?.scrollX = layout_content?.measuredWidth ?: 0
        }

    }


    inner class NavigationItemView : TextDrawableView(context), OnClickListener {
        init {
            setPadding(2 * DP, 0, 2 * DP, 0)
            setOnClickListener(this)
            setDrawableColor(Color.WHITE)
            gravity = Gravity.CENTER
            drawableWidth = 10f * DP
            drawableHeight = 10f * DP
            compoundDrawablePadding = 1 * DP
            textSize = 12f
            setLastStatus(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                compoundDrawableTintList = ColorStateList(
                    arrayOf(
                        intArrayOf()
                    ),
                    intArrayOf(
                        GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary)
                    )
                )
            }
        }

        fun setLastStatus(isLast: Boolean) {
            if (isLast) {
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.colorAccent))
                setDrawableRight(null)
            } else {
                typeface = Typeface.DEFAULT
                setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary))
                setDrawableRight(resources?.getDrawable(com.hive.libfiles.R.drawable.x_file_arr))
            }
        }

        override fun onClick(v: View?) {
            mNavigationListener?.onNavigationClicked(tag as ScriptCommand)
        }
    }

    interface INavigationListener {
        fun onNavigationClicked(cmd: ScriptCommand)
    }
}