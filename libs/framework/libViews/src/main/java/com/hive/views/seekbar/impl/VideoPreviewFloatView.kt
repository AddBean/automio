// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar.impl

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import com.hive.views.R

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2022/9/20
 */
class VideoPreviewFloatView(context: Context) : FrameLayout(context) {

    val layoutView =
        LayoutInflater.from(context).inflate(R.layout.video_preview_float_view, this, true)

    private var desireHeight: Int = 0

    private var desireWidth: Int = 0

    fun setViewSize(width: Int, height: Int) {
        desireWidth = width
        desireHeight = height
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthSpec = widthMeasureSpec
        var heightSpec = heightMeasureSpec
        if (desireWidth > 0) {
            widthSpec = MeasureSpec.makeMeasureSpec(desireWidth, MeasureSpec.EXACTLY)
        }
        if (desireHeight > 0) {
            heightSpec = MeasureSpec.makeMeasureSpec(desireHeight, MeasureSpec.EXACTLY)
        }
        super.onMeasure(widthSpec, heightSpec)
    }

    fun loadBitmap(bmp: Bitmap) {
        findViewById<ImageView>(R.id.ivPreview)?.setImageBitmap(bmp)
    }


}