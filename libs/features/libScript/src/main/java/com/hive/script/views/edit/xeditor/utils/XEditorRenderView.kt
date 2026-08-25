// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import com.hive.script.R

abstract class XEditorRenderView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    private val colorBg = resources.getColor(com.hive.i8n.R.color.colorPrimary)
    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(colorBg)
        onRender(canvas)
    }

    abstract fun onRender(canvas: Canvas?)
}