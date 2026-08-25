// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.hive.utils.GlobalApp

/** 预览预热时间(ms)，让 AE 自动曝光收敛后再拍照 */
private const val AE_CONVERGENCE_DELAY_MS = 800L

/**
 * 摄像头拍照辅助类
 * 拍照偏暗原因：相机打开后立即拍照，AE(自动曝光)未收敛，使用默认保守参数导致偏暗。
 * 解决：添加预览预热 + 曝光补偿。
 *
 * @author jiadou
 * @email 172111432@qq.com
 */
class CaptureCameraHelper {
    companion object {
        /**
         * 拍照
         * @param cameraId 摄像头ID (0: 后置摄像头, 1: 前置摄像头)
         * @param enableFlash 是否启用闪光灯，默认关
         * @param callback 回调函数，返回Bitmap或错误信息
         */
        fun takePicture(cameraId: Int, enableFlash: Boolean = false, callback: (Bitmap?, String?) -> Unit) {
            val context = GlobalApp.getApp()
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val cameraThread = HandlerThread("CameraThread").apply { start() }
            val cameraHandler = Handler(cameraThread.looper)

            try {
                val cameraIdStr = cameraId.toString()

                // 提前计算旋转角度，供解码后校正方向（decodeByteArray 不会应用 EXIF）
                val characteristics = cameraManager.getCameraCharacteristics(cameraIdStr)
                val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                val displayRotation = (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
                    .defaultDisplay.rotation
                val jpegRotation = when (displayRotation) {
                    Surface.ROTATION_0 -> sensorOrientation
                    Surface.ROTATION_90 -> (sensorOrientation + 270) % 360
                    Surface.ROTATION_180 -> (sensorOrientation + 180) % 360
                    Surface.ROTATION_270 -> (sensorOrientation + 90) % 360
                    else -> sensorOrientation
                }

                // 创建 ImageReader 接收 JPEG
                val imageReader = ImageReader.newInstance(1280, 720, android.graphics.ImageFormat.JPEG, 2)
                imageReader.setOnImageAvailableListener({ reader ->
                    val image = reader.acquireNextImage()
                    try {
                        if (image != null) {
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            image.close()

                            val options = BitmapFactory.Options().apply { inSampleSize = 1 }
                            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                            // 应用旋转：BitmapFactory 不应用 EXIF，需手动旋转像素
                            if (bitmap != null && jpegRotation != 0) {
                                val matrix = Matrix().apply { postRotate(jpegRotation.toFloat()) }
                                val rotated = Bitmap.createBitmap(
                                    bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true
                                )
                                if (rotated != bitmap) {
                                    bitmap.recycle()
                                    bitmap = rotated
                                }
                            }

                            if (bitmap != null) {
                                callback(bitmap, null)
                            } else {
                                callback(null, GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed))
                            }
                        } else {
                            callback(null, GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed))
                        }
                    } catch (e: Exception) {
                        callback(null, GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed_with_error, e.message ?: ""))
                    } finally {
                        try {
                            image?.close()
                            imageReader.close()
                            cameraThread.quitSafely()
                        } catch (e: Exception) {
                            // 忽略关闭异常
                        }
                    }
                }, cameraHandler)

                // 创建虚拟预览 Surface，供 AE 分析场景亮度（无界面预览时 AE 仍可收敛）
                val surfaceTexture = SurfaceTexture(10).apply {
                    setDefaultBufferSize(1280, 720)
                }
                val previewSurface = Surface(surfaceTexture)

                cameraManager.openCamera(cameraIdStr, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        try {
                            val characteristics = cameraManager.getCameraCharacteristics(cameraIdStr)

                            // 曝光补偿，用于提亮（约 +1 EV）
                            val aeCompRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                            val exposureComp = if (aeCompRange != null && aeCompRange.upper > 0) {
                                minOf(aeCompRange.upper, 2)
                            } else 0

                            // 闪光灯：仅后置摄像头通常支持
                            val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                            val useFlash = enableFlash && flashAvailable

                            camera.createCaptureSession(
                                listOf(previewSurface, imageReader.surface),
                                object : CameraCaptureSession.StateCallback() {
                                    override fun onConfigured(session: CameraCaptureSession) {
                                        try {
                                            // 1. 先启动预览，让 AE 收敛
                                            val previewRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                                addTarget(previewSurface)
                                                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                                                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                                if (exposureComp != 0) {
                                                    set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, exposureComp)
                                                }
                                            }
                                            session.setRepeatingRequest(previewRequest.build(), null, cameraHandler)

                                            // 2. 延迟后拍照，此时 AE 已收敛
                                            cameraHandler.postDelayed({
                                                try {
                                                    val stillRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                                        addTarget(imageReader.surface)
                                                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                                                        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                                        set(CaptureRequest.JPEG_QUALITY, 85.toByte())
                                                        set(CaptureRequest.JPEG_ORIENTATION, jpegRotation)
                                                        if (exposureComp != 0) {
                                                            set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, exposureComp)
                                                        }
                                                        if (useFlash) {
                                                            set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE)
                                                        }
                                                    }
                                                    session.capture(stillRequest.build(), object : CameraCaptureSession.CaptureCallback() {
                                                        override fun onCaptureFailed(
                                                            session: CameraCaptureSession,
                                                            request: CaptureRequest,
                                                            failure: CaptureFailure
                                                        ) {
                                                            callback(null, GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed_with_error, failure.toString()))
                                                            camera.close()
                                                        }
                                                    }, cameraHandler)
                                                } catch (e: Exception) {
                                                    callback(null, GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed_with_error, e.message ?: ""))
                                                    camera.close()
                                                }
                                            }, AE_CONVERGENCE_DELAY_MS)
                                        } catch (e: Exception) {
                                            callback(null, GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed_with_error, e.message ?: ""))
                                            camera.close()
                                        }
                                    }

                                    override fun onConfigureFailed(session: CameraCaptureSession) {
                                        callback(null, GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed))
                                        camera.close()
                                    }
                                },
                                cameraHandler
                            )
                        } catch (e: Exception) {
                            callback(null, GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed_with_error, e.message ?: ""))
                            camera.close()
                        }
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        callback(null, GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed))
                        camera.close()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        callback(null, GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed_with_error, error.toString()))
                        camera.close()
                    }
                }, cameraHandler)
            } catch (e: CameraAccessException) {
                callback(null, GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed_with_error, e.message ?: ""))
                cameraThread.quitSafely()
            } catch (e: SecurityException) {
                callback(null, GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed_with_error, e.message ?: ""))
                cameraThread.quitSafely()
            } catch (e: Exception) {
                callback(null, GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed_with_error, e.message ?: ""))
                cameraThread.quitSafely()
            }
        }
    }
}
