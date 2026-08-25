// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.core

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import com.hive.script.views.edit.xeditor.core.gesture.SCRotateGestureDetector
import com.hive.script.views.edit.xeditor.core.gesture.SCScaleGestureDetector

/**
 *
 * @author jiadou
 * @date 5/6/21
 */
class SCEditTouchEventHelper(var context: Context) {

    private var mGestureDetector: GestureDetector? = null

    private var mRotateGestureDetector: SCRotateGestureDetector? = null

    private var mScaleGestureDetector: SCScaleGestureDetector? = null

    fun onTouchEvent(event: MotionEvent): Boolean {
        val result1 = mGestureDetector?.onTouchEvent(event) ?: false
        val result2 = mRotateGestureDetector?.onTouchEvent(event) ?: false
        val result3 = mScaleGestureDetector?.onTouchEvent(event) ?: false
        return result1.or(result2).or(result3)
    }

    fun registerEventListener(listener: SCEditTouchEventListener) {
        mGestureDetector = GestureDetector(context, listener)
        mRotateGestureDetector =
            SCRotateGestureDetector(
                context,
                listener
            )
        mScaleGestureDetector =
            SCScaleGestureDetector(
                context,
                listener
            )
    }
}