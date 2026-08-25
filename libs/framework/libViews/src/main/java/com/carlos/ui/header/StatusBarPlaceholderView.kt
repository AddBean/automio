// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.carlos.ui.header

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.blankj.utilcode.util.BarUtils
import com.hive.views.R

class StatusBarPlaceholderView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    val statusBarHeight = BarUtils.getStatusBarHeight()

    /**
     * 额外增加的高度，单位：像素
     * 可通过 XML 属性 app:extraHeight 或 setExtraHeight() 方法设置
     */
    var extraHeight: Int = 0
        set(value) {
            field = value
            requestLayout()
        }

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.StatusBarPlaceholderView)
            extraHeight = typedArray.getDimensionPixelSize(R.styleable.StatusBarPlaceholderView_extraHeight, 0)
            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            widthMeasureSpec,
            statusBarHeight + extraHeight
        )
    }
}