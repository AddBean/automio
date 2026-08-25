// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.opencv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.hive.plugin.provider.IOpenCVProvider
import org.opencv.core.Point

/**
 *
 * @author jiadou
 * @date 6/11/21
 */
class OpenCVProvider : IOpenCVProvider {

    private var mImagesHelper: ImagesHelper? = null

    override fun init(context: Context?) {
        OpenCVHelper.initIfNeeded(context) {
            mImagesHelper = ImagesHelper()
        }
    }

    override fun findColor(bmp: Bitmap?, color: Int, threshold: Int): android.graphics.Point {
        var p = ColorFinder(ScreenMetrics()).findColor(ImageWrapper.ofBitmap(bmp), color, threshold)
        return android.graphics.Point().apply {
            x = p.x.toInt()
            y = p.y.toInt()
        }
    }

    override fun findColors(bmp: Bitmap?, color: Int, threshold: Int): Array<android.graphics.Point> {
        var ps = ColorFinder(ScreenMetrics()).findAllPointsForColor(ImageWrapper.ofBitmap(bmp), color, threshold, null)
        return ps.map {
            android.graphics.Point().apply {
                x = it.x.toInt()
                y = it.y.toInt()
            }
        }.toTypedArray()
    }

    override fun findColorToRect(bmp: Bitmap?, color: Int, threshold: Int): Array<Rect> {
        var ps = ColorFinder(ScreenMetrics()).findColorRect(ImageWrapper.ofBitmap(bmp), color, threshold, null)
        ps.sortByDescending { it.width * it.height }
        return ps.map {
            Rect((it.x), (it.y), (it.x + it.width), (it.y + it.height))
        }.toTypedArray()
    }


    override fun findImage(sample: Bitmap, dest: Bitmap, desiredAccuracy: Double): Rect? {
        try {
            mImagesHelper
                    ?.run {
                        var point: Point? = findImage(ImageWrapper.ofBitmap(dest), ImageWrapper.ofBitmap(sample), desiredAccuracy.toFloat())
                        point?.let {
                            return@findImage Rect(point.x.toInt(), point.y.toInt(), (point.x + sample.width).toInt(), (point.y + sample.height).toInt())
                        }
                    }

        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
        return null
    }


}