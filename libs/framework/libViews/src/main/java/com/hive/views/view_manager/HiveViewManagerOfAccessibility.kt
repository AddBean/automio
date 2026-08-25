// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.view_manager

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class HiveViewManagerOfAccessibility(var context: Context, var width: Int, var height: Int) :
    IHiveViewManager {

    private var view: View? = null

    private var mLayoutParams = WindowManager.LayoutParams().also { lp ->
        lp.width = width
        lp.height = height
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            lp.type = WindowManager.LayoutParams.TYPE_TOAST
        }
        lp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        lp.format = PixelFormat.RGBA_8888
        lp.gravity = Gravity.TOP
        val screenSize = Point()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getRealSize(screenSize)
        if (width == WindowManager.LayoutParams.MATCH_PARENT) {
            lp.width = screenSize.x
        }
        if (height == WindowManager.LayoutParams.MATCH_PARENT) {
            lp.height = screenSize.y
        }
    }

    override fun updateLayoutParams(layoutParams: Any) {
        view ?: return
        if (layoutParams !is WindowManager.LayoutParams) return
        mLayoutParams = layoutParams
        updateViewLayout(view)
    }

    override fun updateViewLayout(view: View?) {
        try {
            this.view = view
            val screenSize = Point()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getRealSize(screenSize)
            if (width == WindowManager.LayoutParams.MATCH_PARENT) {
                mLayoutParams.width = screenSize.x
            }
            if (height == WindowManager.LayoutParams.MATCH_PARENT) {
                mLayoutParams.height = screenSize.y
            }
            windowManager.updateViewLayout(view, mLayoutParams)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    override fun addView(view: View?) {
        try {
            this.view = view
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.addView(view, mLayoutParams)
        } catch (i: Exception) {
            i.printStackTrace()
        }
    }

    override fun removeView(view: View?) {
        this.view = null
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.removeView(view)
    }

}