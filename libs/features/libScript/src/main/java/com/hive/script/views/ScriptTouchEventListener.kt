// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 5/6/21
 */
abstract class ScriptTouchEventListener : GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean = false

    override fun onDown(e: MotionEvent): Boolean = false

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean = false

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean = false

    override fun onLongPress(e: MotionEvent) {

    }

    open fun onTouchEvent(event: MotionEvent) {

    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = false

    override fun onScaleEnd(detector: ScaleGestureDetector) {}

    override fun onScale(detector: ScaleGestureDetector): Boolean = false

}