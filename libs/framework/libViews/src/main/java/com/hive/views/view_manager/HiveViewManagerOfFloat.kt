// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.view_manager

import android.content.Context
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager

class HiveViewManagerOfFloat(var context: Context, var width: Int, var height: Int) :
    IHiveViewManager {

    private var mLayoutParams = WindowManager.LayoutParams().also { lp ->
        lp.width = width
        lp.height = height
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION
        lp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        lp.format = PixelFormat.RGBA_8888
    }

    private var view: View? = null

    override fun updateLayoutParams(layoutParams: Any) {
        view ?: return
        if (layoutParams !is WindowManager.LayoutParams) return
        mLayoutParams = layoutParams
        updateViewLayout(view)
    }

    override fun updateViewLayout(view: View?) {
        this.view = view
        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.updateViewLayout(view, mLayoutParams)
    }

    override fun addView(view: View?) {
        this.view = view
        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(view, mLayoutParams)
    }

    override fun removeView(view: View?) {
        this.view = null
        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.removeView(view)
    }
}