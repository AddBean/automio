// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 5/6/21
 */
class ScriptTouchEventHelper(var context: Context) {

    private var mScriptTouchEventListener: ScriptTouchEventListener? = null
    private var mGestureDetector: GestureDetector? = null

    private var mScaleGestureDetector: ScaleGestureDetector? = null

    fun onTouchEvent(event: MotionEvent) {
        mScriptTouchEventListener?.onTouchEvent(event)
        var result1 = mGestureDetector?.onTouchEvent(event) ?: false
        var result3 = mScaleGestureDetector?.onTouchEvent(event) ?: false
    }

    fun registerEventListener(listener: ScriptTouchEventListener) {
        mScriptTouchEventListener = listener
        mGestureDetector = GestureDetector(context, listener)
        mScaleGestureDetector = ScaleGestureDetector(context, listener)
    }
}