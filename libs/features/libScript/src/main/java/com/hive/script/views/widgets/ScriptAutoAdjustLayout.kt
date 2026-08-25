// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.hive.script.views.manager.ScriptManager

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/14/21
 */
open class ScriptAutoAdjustLayout(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {

    override fun onFinishInflate() {
        super.onFinishInflate()
        ScriptManager.updateViewLayout()
    }
}