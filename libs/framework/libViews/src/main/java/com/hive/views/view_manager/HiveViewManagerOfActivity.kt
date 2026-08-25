// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.view_manager

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout

class HiveViewManagerOfActivity(var activity: Activity, var width: Int, var height: Int) :
    IHiveViewManager {

    private var mViewLayoutParams = FrameLayout.LayoutParams(width, height)

    private var view: View? = null

    override fun updateLayoutParams(layoutParams: Any) {
        view ?: return
        mViewLayoutParams = when (layoutParams) {
            is WindowManager.LayoutParams -> layoutParams.toActivityLayoutParams()
            is ViewGroup.LayoutParams -> layoutParams.toActivityLayoutParams()
            else -> return
        }
        updateViewLayout(view)
    }

    override fun updateViewLayout(view: View?) {
        this.view = view
        val viewGroup =
            activity.window?.decorView?.rootView as ViewGroup
        try {
            viewGroup.updateViewLayout(view, mViewLayoutParams)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    override fun addView(view: View?) {
        this.view = view
        val viewGroup =
            activity.window?.decorView?.rootView as ViewGroup
        viewGroup.addView(view, mViewLayoutParams)
    }

    override fun removeView(view: View?) {
        this.view = null
        val viewGroup =
            activity.window?.decorView?.rootView as ViewGroup
        viewGroup.removeView(view)
    }

    private fun WindowManager.LayoutParams.toActivityLayoutParams(): FrameLayout.LayoutParams {
        val frameLayoutParams = FrameLayout.LayoutParams(width, height)
        frameLayoutParams.gravity = gravity

        val absoluteGravity =
            Gravity.getAbsoluteGravity(gravity, activity.resources.configuration.layoutDirection)

        when {
            absoluteGravity and Gravity.RIGHT == Gravity.RIGHT -> frameLayoutParams.rightMargin = x
            else -> frameLayoutParams.leftMargin = x
        }

        when {
            gravity and Gravity.BOTTOM == Gravity.BOTTOM -> frameLayoutParams.bottomMargin = y
            else -> frameLayoutParams.topMargin = y
        }

        return frameLayoutParams
    }

    private fun ViewGroup.LayoutParams.toActivityLayoutParams(): FrameLayout.LayoutParams {
        return when (this) {
            is FrameLayout.LayoutParams -> FrameLayout.LayoutParams(this)
            is ViewGroup.MarginLayoutParams -> FrameLayout.LayoutParams(this)
            else -> FrameLayout.LayoutParams(width, height)
        }
    }
}
