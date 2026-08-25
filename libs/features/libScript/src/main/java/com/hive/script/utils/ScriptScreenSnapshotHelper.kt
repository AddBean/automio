// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.hive.utils.GlobalApp
import java.nio.ByteBuffer


/**
 *
 * @author jiadou
 * @date 6/29/21
 */
class ScriptScreenSnapshotHelper {

    //初始化数据
    fun getScreenSnapshot(): Bitmap? {
        val mWindowManager = GlobalApp.getContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val mWindowWidth = mWindowManager.defaultDisplay.width
        val mWindowHeight = mWindowManager.defaultDisplay.height
        val mImageReader = ImageReader.newInstance(mWindowWidth, mWindowHeight, 1, 2)
        return startCapture(mImageReader)
    }

    //开始截图
    private fun startCapture(mImageReader: ImageReader): Bitmap? {
        val image: Image = mImageReader.acquireLatestImage() ?: return null
        val width: Int = image.width
        val height: Int = image.height
        val planes: Array<Image.Plane> = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride: Int = planes[0].pixelStride
        val rowStride: Int = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width
        var mBitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        mBitmap.copyPixelsFromBuffer(buffer)
        mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height)
        image.close()
        return mBitmap
    }

}