// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import com.hive.base.BaseLayout
import com.hive.files.model.FileCardData
import com.hive.libfiles.R
import com.hive.views.widgets.TextDrawableView

/**
 *
 * @author jiadou
 * @date 4/7/21
 */
class XFileNavigationBar(context: Context, attrs: AttributeSet) : BaseLayout(context, attrs) {

    private var mCurrentFile: FileCardData? = null
    var mNavigationListener: INavigationListener? = null
    private var layout_content: ViewGroup? = null
    private var scroll_view: View? = null


    override fun getLayoutId(): Int = R.layout.x_file_navigation_bar

    override fun initView(view: View?) {
        layout_content=view?.findViewById(R.id.layout_content)
        scroll_view = view?.findViewById(R.id.scroll_view)
    }

    fun updateBar(file: FileCardData) {
        mCurrentFile = file
        var list = mutableListOf<FileCardData>()

        while (mCurrentFile?.parent != null) {
            list.add(mCurrentFile!!)
            mCurrentFile = mCurrentFile!!.parent
        }
        list.reverse()
        list.add(0, FileCardData())
        layout_content?.removeAllViews()
        var lastView: NavigationItemView? = null
        list.forEach { file ->
            lastView = NavigationItemView().apply {
                text = file.fileName
                tag = file
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
            setPadding(4 * DP, 0, 4 * DP, 0)
            setOnClickListener(this)
            setDrawableColor(Color.WHITE)
            gravity = Gravity.CENTER
            drawableWidth = 12f * DP
            drawableHeight = 12f * DP
            compoundDrawablePadding = 2 * DP
            setLastStatus(false)
        }

        fun setLastStatus(isLast: Boolean) {
            if (isLast) {
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.BLACK)
                setDrawableRight(null)
            } else {
                typeface = Typeface.DEFAULT
                setTextColor(0xFFB2B2B2.toInt())
                setDrawableRight(resources?.getDrawable(R.drawable.x_file_arr))
            }
        }

        override fun onClick(v: View?) {
            mNavigationListener?.onNavigationClicked(tag as FileCardData)
        }
    }

    interface INavigationListener {
        fun onNavigationClicked(file: FileCardData)
    }
}