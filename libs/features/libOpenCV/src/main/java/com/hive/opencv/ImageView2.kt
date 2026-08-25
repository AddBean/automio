// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.opencv

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet

/**
 *
 * @author jiadou
 * @date 6/28/21
 */
class ImageView2(context: Context?, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatImageView(
        context!!, attrs
    ) {
    var onDrawListener: OnDrawListener? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        onDrawListener?.onDraw(canvas)
    }

    interface OnDrawListener {
        fun onDraw(canvas: Canvas?)
    }
}