// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import com.hive.utils.utils.DeviceCompatHelper
import com.hive.utils.utils.ScreenUtils

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/16/21
 */
class ScriptCoordinateAdapter {

    private var screenWidth = ScreenUtils.getScreenWidth()

    private var screenHeight = ScreenUtils.getScreenHeight()

    /**
     * 确保被调用
     */
    fun initScreen() {
        //确保横竖屏得到分辨率一致；
        val w = ScreenUtils.getScreenWidth()
        val h = ScreenUtils.getScreenHeight()
        if (w > h) {
            screenWidth = h
            screenHeight = w
        } else {
            screenWidth = w
            screenHeight = h
        }
    }

    fun toRealX(x: Float): Int {
        val p = toRealPoint(x, 0f)
        return p.x
    }

    fun toRealY(y: Float): Int {
        val p = toRealPoint(0f, y)
        return p.y
    }


    fun toNormalizedX(x: Int): Float {
        val p = toNormalizedPoint(x, 0)
        return p.x
    }

    fun toNormalizedY(y: Int): Float {
        val p = toNormalizedPoint(0, y)
        return p.y
    }

    fun toRealPoints(points: MutableList<PointF>): MutableList<Point> {
        val newPoints = mutableListOf<Point>()
        points.forEach {
            val p = toRealPoint(it.x, it.y)
            newPoints.add(p)
        }
        return newPoints
    }

    private fun toRealPoint(x: Float, y: Float): Point {
        val wa = if (!DeviceCompatHelper.isLandscape()) {
            screenWidth
        } else {
            screenHeight
        }
        val ha = if (!DeviceCompatHelper.isLandscape()) {
            screenHeight
        } else {
            screenWidth
        }

        return Point((x * wa).toInt(), (y * ha).toInt())
    }

    fun toNormalizedPoint(x: Int, y: Int): PointF {
        val wa = if (!DeviceCompatHelper.isLandscape()) {
            screenWidth
        } else {
            screenHeight
        }
        val ha = if (!DeviceCompatHelper.isLandscape()) {
            screenHeight
        } else {
            screenWidth
        }

        return PointF(x.toFloat() / wa, y.toFloat() / ha)
    }

    fun toNormalizedRect(rect: Rect): RectF {
        val r = RectF()
        r.left = toNormalizedX(rect.left)
        r.top = toNormalizedY(rect.top)
        r.right = toNormalizedX(rect.right)
        r.bottom = toNormalizedY(rect.bottom)
        return r
    }

    companion object {

        private val instance: ScriptCoordinateAdapter by lazy {
            ScriptCoordinateAdapter()
        }

        fun get() = instance

        fun getScreenWidth() = get().screenWidth

        fun getScreenHeight() = get().screenHeight

        fun getScreenWidthByOrientation() =
            if (!DeviceCompatHelper.isLandscape()) getScreenWidth() else getScreenHeight()

        fun getScreenHeightByOrientation() =
            if (!DeviceCompatHelper.isLandscape()) getScreenHeight() else getScreenWidth()

        fun toNormalizedPoint(x: Int, y: Int, format: String = "%.3f"): PointF {
            val p = get().toNormalizedPoint(x, y)
            return PointF(
                String.format(format, p.x).toFloat(),
                String.format(format, p.y).toFloat()
            )
        }

        fun toNormalizedX(x: Int, format: String = "%.3f"): Float {
            val r = get().toNormalizedX(x)
            return String.format(format, r).toFloat()
        }

        fun toNormalizedY(y: Int, format: String = "%.3f"): Float {
            val r = get().toNormalizedY(y)
            return String.format(format, r).toFloat()
        }
    }
}