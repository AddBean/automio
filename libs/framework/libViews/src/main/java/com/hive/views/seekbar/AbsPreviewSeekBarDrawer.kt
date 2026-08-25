// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.widget.FrameLayout
import com.hive.utils.GlobalApp
import com.hive.utils.system.UIUtils

/**
 *
 * @author jiadou
 * @date 2022/9/19
 */
abstract class AbsPreviewSeekBarDrawer {

    protected var dp = UIUtils.dp2px(GlobalApp.sContext,1)

    var seekBar: PreviewSeekBar? = null

    var hostView: FrameLayout? = null

    var originRect = RectF()

    var insetRect = RectF()

    open fun onAttached() {}

    open fun onDetached() {}

    open fun onShow() {}

    open fun onHidden() {}

    abstract fun onDraw(canvas: Canvas, progress: Float, segments: List<PreviewSegmentData>)

    protected fun getContext(): Context? = hostView?.context

    fun postInvalidate() {
        hostView?.postInvalidate()
    }
}