// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.utils.ScriptCoordinateAdapter.Companion.getScreenHeightByOrientation
import com.hive.script.utils.ScriptCoordinateAdapter.Companion.getScreenWidthByOrientation
import com.hive.script.views.logger.ScriptLoggerView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string
import com.hive.utils.utils.DeviceCompatHelper
import com.hive.utils.utils.ScreenUtils

class CaptureUtil : VirtualDisplay.Callback() {
    private val projectionLock = Any()

    private var currentVirtualDisplay: VirtualDisplay? = null
    private var currentOrientation = Configuration.ORIENTATION_UNDEFINED
    private var currentReaderWidth = 0
    private var currentReaderHeight = 0

    private var mediaProjectionManager: MediaProjectionManager? = null

    private var mediaProjectionManagerResultDate: Intent? = null

    private var mediaProjectionManagerResultCode = 0

    private var mediaProjection: MediaProjection? = null

    private var userMirror: Boolean = false

    private val mediaProjectionCallback: MediaProjection.Callback =
        object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                ScriptInterpreterObserver.notifyLogger(
                    null,
                    ScriptLoggerView.LogType.ERROR,
                    com.hive.i8n.R.string.sc_snop_stoped.string()
                )
                // Android 14+ MediaProjection token 会失效，无法用旧 token 重启
                // 需要用户重新授权，清除旧状态让脚本检测到权限缺失
                clearProjectionState()
            }

            override fun onCapturedContentResize(width: Int, height: Int) {
                super.onCapturedContentResize(width, height)
            }

            override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
                super.onCapturedContentVisibilityChanged(isVisible)
            }
        }

    /**
     * 清除 MediaProjection 状态，让脚本检测到权限缺失并重新请求授权。
     * Android 14+ token 失效后无法自动重启，必须用户重新授权。
     */
    private fun clearProjectionState() {
        synchronized(projectionLock) {
            mediaProjection = null
            currentVirtualDisplay?.release()
            currentVirtualDisplay = null
            currentOrientation = Configuration.ORIENTATION_UNDEFINED
            currentReaderWidth = 0
            currentReaderHeight = 0
            mediaProjectionManagerResultDate = null
            mediaProjectionManagerResultCode = 0
            releaseImageReaders()
        }
    }

    private val screenWidth: Int
        get() = getScreenWidthByOrientation()

    private val screenHeight: Int
        get() = getScreenHeightByOrientation()

    private val screenCapHorizontal: Bitmap?
        /**
         * 获取横屏模式下的截屏
         */
        get() {
            synchronized(projectionLock) {
                initHorizontal()
            }
            var image: Image?
            var tryCount = 0
            do {
                image = imageReaderHorizontal?.acquireLatestImage()
                tryCount++
                ScriptThreadManager.delay(50)
            } while (image == null && tryCount < 10)
            if (image == null) return null
            return try {
                ImageUtil.covetBitmap(image)
            } finally {
                image.close()
            }
        }

    /**
     * 获取竖屏模式下的截屏
     */
    private val screenCapVertical: Bitmap?
        get() {
            synchronized(projectionLock) {
                initVertical()
            }
            var image: Image?
            var tryCount = 0
            do {
                image = imageReaderVertical?.acquireLatestImage()
                tryCount++
                ScriptThreadManager.delay(50)
            } while (image == null && tryCount < 10)
            if (image == null) return null
            return try {
                ImageUtil.covetBitmap(image)
            } finally {
                image.close()
            }
        }

    /**
     * 返回对应模式下的屏幕截图
     */
    val screenCapture: Bitmap?
        get() {
            if (DeviceCompatHelper.isVertical()) {
                return screenCapVertical
            }
            return screenCapHorizontal
        }

    fun init(manager: MediaProjectionManager?, resultCode: Int, data: Intent?) {
        synchronized(projectionLock) {
            mediaProjectionManager = manager
            mediaProjectionManagerResultCode = resultCode
            mediaProjectionManagerResultDate = data
            if (DeviceCompatHelper.isLandscape()) {
                initHorizontal()
            } else {
                initVertical()
            }
        }
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        synchronized(projectionLock) {
            if (mediaProjectionManager != null || mediaProjection != null) {
                if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    initHorizontal()
                } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    initVertical()
                }
            }
        }
    }

    private fun initVertical() {
        try {
            imageReaderHorizontal?.close()
            imageReaderHorizontal = null
            if (!ensureMediaProjection()) return
            val width = screenWidth
            val height = screenHeight
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT &&
                currentVirtualDisplay != null &&
                imageReaderVertical != null &&
                currentReaderWidth == width &&
                currentReaderHeight == height
            ) {
                return
            }
            if (imageReaderVertical == null ||
                imageReaderVertical?.width != width ||
                imageReaderVertical?.height != height
            ) {
                imageReaderVertical?.close()
                imageReaderVertical = ImageReader.newInstance(
                    width,
                    height,
                    PixelFormat.RGBA_8888,
                    2
                )
            }
            // 竖屏模式下的截屏初始化
            rebuildVirtualDisplay(
                "VerticalScreenShot",
                imageReaderVertical ?: return,
                width,
                height,
                Configuration.ORIENTATION_PORTRAIT
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun initHorizontal() {
        try {
            imageReaderVertical?.close()
            imageReaderVertical = null
            if (!ensureMediaProjection()) return
            val width = screenWidth
            val height = screenHeight
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE &&
                currentVirtualDisplay != null &&
                imageReaderHorizontal != null &&
                currentReaderWidth == width &&
                currentReaderHeight == height
            ) {
                return
            }
            if (imageReaderHorizontal == null ||
                imageReaderHorizontal?.width != width ||
                imageReaderHorizontal?.height != height
            ) {
                imageReaderHorizontal?.close()
                imageReaderHorizontal = ImageReader.newInstance(
                    width,
                    height,
                    PixelFormat.RGBA_8888,
                    2
                )
            }
            // 横屏模式下的截屏初始化
            rebuildVirtualDisplay(
                "HorizontalScreenShot",
                imageReaderHorizontal ?: return,
                width,
                height,
                Configuration.ORIENTATION_LANDSCAPE
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ensureMediaProjection(): Boolean {
        if (mediaProjection != null) return true
        val resultData = mediaProjectionManagerResultDate ?: return false
        if (mediaProjectionManagerResultCode == 0) return false
        if (mediaProjectionManager == null) {
            mediaProjectionManager = GlobalApp.getApp()
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        }
        return try {
            mediaProjection = mediaProjectionManager?.getMediaProjection(
                mediaProjectionManagerResultCode,
                resultData
            )
            mediaProjection?.unregisterCallback(mediaProjectionCallback)
            mediaProjection?.registerCallback(mediaProjectionCallback, null)
            mediaProjection != null
        } catch (e: Exception) {
            e.printStackTrace()
            clearProjectionState()
            false
        }
    }

    private fun rebuildVirtualDisplay(
        displayName: String,
        reader: ImageReader,
        width: Int,
        height: Int,
        orientation: Int
    ) {
        val displayFlag = if (userMirror) {
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
        } else {
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
        }
        if (currentVirtualDisplay == null) {
            currentVirtualDisplay = mediaProjection?.createVirtualDisplay(
                displayName,
                width,
                height,
                ScreenUtils.getDpi(),
                displayFlag,
                reader.surface,
                this,
                null
            )
        } else {
            currentVirtualDisplay?.resize(width, height, ScreenUtils.getDpi())
            currentVirtualDisplay?.setSurface(reader.surface)
        }
        currentOrientation = orientation
        currentReaderWidth = width
        currentReaderHeight = height
    }

    private fun releaseImageReaders() {
        imageReaderHorizontal?.close()
        imageReaderHorizontal = null
        imageReaderVertical?.close()
        imageReaderVertical = null
    }

    /**
     * 预览截图
     */
    fun show(img: Bitmap?, context: Context?) {
    }

    fun stop() {
        synchronized(projectionLock) {
            userMirror = false
            mediaProjection?.stop()
            mediaProjection = null
        }
    }

    /**
     * 检查 MediaProjection 是否有效。
     * Android 14+ token 失效后需要用户重新授权。
     */
    fun isProjectionValid(): Boolean {
        synchronized(projectionLock) {
            return mediaProjection != null &&
                currentVirtualDisplay != null &&
                (imageReaderVertical != null || imageReaderHorizontal != null)
        }
    }

    /**
     * 释放 MediaProjection 与 VirtualDisplay、ImageReader 等资源。
     * 在截屏服务销毁时调用，避免工作流结束后仍占用屏幕投影。
     */
    fun release() {
        try {
            synchronized(projectionLock) {
                mediaProjection?.unregisterCallback(mediaProjectionCallback)
                currentVirtualDisplay?.release()
                currentVirtualDisplay = null
                currentOrientation = Configuration.ORIENTATION_UNDEFINED
                currentReaderWidth = 0
                currentReaderHeight = 0
                mediaProjection?.stop()
                mediaProjection = null
                mediaProjectionManager = null
                mediaProjectionManagerResultDate = null
                mediaProjectionManagerResultCode = 0
                userMirror = false
                releaseCompanionResources()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStopped() {
        super.onStopped()
        clearProjectionState()
    }

    companion object {

        private var instance: CaptureUtil = CaptureUtil()

        private var imageReaderVertical: ImageReader? = null

        private var imageReaderHorizontal: ImageReader? = null

        fun get(): CaptureUtil {
            return instance
        }

        internal fun releaseCompanionResources() {
            try {
                imageReaderHorizontal?.close()
                imageReaderHorizontal = null
                imageReaderVertical?.close()
                imageReaderVertical = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
