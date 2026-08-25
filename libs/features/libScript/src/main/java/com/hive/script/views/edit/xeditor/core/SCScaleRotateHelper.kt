// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.core

import android.graphics.PointF
import android.view.MotionEvent
import java.lang.Math.atan2
import java.lang.Math.sqrt
import kotlin.math.pow

/**
 *
 * @author jiadou
 * @date 5/8/21
 */
class SCScaleRotateHelper {

    private var mPrePointF = PointF()

    private var mNowPointF = PointF()

    private var mPreScale = 1f

    private var mPreItemScale = 1f

    private var mPreAngle = 0f

    var mCurrentAngle = 0f

    var mCurrentScaleX = 0f

    var mCurrentScaleY = 0f

    fun onTouchStart(event: MotionEvent, currentAngle: Float, currentScaleX: Float, currentScaleY: Float) {
        mCurrentScaleX = currentScaleX
        mCurrentScaleY = currentScaleY

        mCurrentAngle = currentAngle
        mPrePointF.x = event.x
        mPrePointF.y = event.y
        mPreScale = 1f
        mPreItemScale = mCurrentScaleX
        mPreAngle = 0f
    }

    fun onTouchMove(event: MotionEvent, centerPoint: PointF) {
        val targetX = event.x
        val targetY = event.y
        mNowPointF.x = targetX
        mNowPointF.y = targetY

        // 计算手指在屏幕上滑动的距离比例
        val preLength = sqrt((mPrePointF.x - centerPoint.x.toDouble()).pow(2.0) + (mPrePointF.y - centerPoint.y.toDouble()).pow(2.0))
        val nowLength = sqrt((mNowPointF.x - centerPoint.x.toDouble()).pow(2.0) + (mNowPointF.y - centerPoint.y.toDouble()).pow(2.0))

        val newScale = (nowLength / preLength).toFloat()
        val dScale = (newScale - mPreScale) * mPreItemScale
        mCurrentScaleX += dScale
        mCurrentScaleY += dScale

        mPreScale = newScale

        // 计算手指滑动的角度
        val radian = (atan2(targetY - centerPoint.y.toDouble(), targetX - centerPoint.x.toDouble()) - atan2(mPrePointF.y - centerPoint.y.toDouble(), mPrePointF.x - centerPoint.x.toDouble())).toFloat()
        var newAngle = (radian * 180 / Math.PI).toFloat() // 弧度转换为角度
        if (newAngle > 360) newAngle = 0f
        mCurrentAngle += (newAngle - mPreAngle)
        if (mCurrentAngle > 360) mCurrentAngle = 0f
        mPreAngle = newAngle
    }

    fun onTouchEnd(event: MotionEvent) {

    }
}