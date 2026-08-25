// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.core

import android.view.GestureDetector
import android.view.MotionEvent
import com.hive.script.views.edit.xeditor.core.gesture.SCRotateGestureDetector
import com.hive.script.views.edit.xeditor.core.gesture.SCScaleGestureDetector

/**
 *
 * @author jiadou
 * @date 5/6/21
 */
abstract class SCEditTouchEventListener: GestureDetector.OnGestureListener, SCRotateGestureDetector.OnRotateGestureListener, SCScaleGestureDetector.OnScaleGestureListener {

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent) = false

    override fun onDown(e: MotionEvent) = false

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float) = false

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float) = false

    override fun onLongPress(e: MotionEvent) {

    }

    override fun onRotate(detector: SCRotateGestureDetector?) = false

    override fun onRotateEnd(detector: SCRotateGestureDetector?) {}

    override fun onRotateBegin(detector: SCRotateGestureDetector?) = false

    override fun onScaleBegin(detector: SCScaleGestureDetector?) = false

    override fun onScaleEnd(detector: SCScaleGestureDetector?) {}

    override fun onScale(detector: SCScaleGestureDetector?) = false
}