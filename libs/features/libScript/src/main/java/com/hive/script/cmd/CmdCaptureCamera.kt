// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.graphics.Bitmap
import android.graphics.RectF
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.utils.CaptureCameraHelper
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.logger.ScriptLoggerView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string
import com.hive.utils.utils.BitmapUtils
import java.util.concurrent.CountDownLatch

/**
 * 摄像头拍照命令
 * @author jiadou
 */
@AutoCmdRegister(type = IDS.CmdCaptureCamera, name = "captureCamera")
class CmdCaptureCamera : ScriptCommand(), ScriptRegularInterface {

    var targetParamId = ScriptParamEnv.getDefaultParam()?.getFullId()

    // 0: 后置摄像头, 1: 前置摄像头
    var cameraId = 0

    /** 是否启用闪光灯，默认关 */
    var enableFlash = false

    /** 是否保存到本机相册，默认 false */
    var saveToGallery = false

    var capturePath: String? = null

    override fun onExecute(): CmdExecuteResult {
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Capture)

        val latch = CountDownLatch(1)
        var bitmap: Bitmap? = null
        var errorMsg: String? = null

        ScriptHelper.runInMain {
            try {
                takePicture(cameraId, enableFlash) { bmp, error ->
                    bitmap = bmp
                    errorMsg = error
                    latch.countDown()
                }
            } catch (e: Exception) {
                errorMsg = GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed_with_error, e.message ?: "")
                latch.countDown()
            }
        }

        try {
            latch.await()
        } catch (e: InterruptedException) {
            return CmdExecuteResult.failure(GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_interrupted))
        }

        if (bitmap == null) {
            return CmdExecuteResult.failure(errorMsg ?: GlobalApp.getString(com.hive.i8n.R.string.script_capture_camera_failed))
        }

        val compressBmp = BitmapUtils.compressAndResize(bitmap!!, 720, 260)

        capturePath = ScriptConst.newRandomTempImagePath("jpg")
        BitmapUtils.saveBitmapLocal(compressBmp, capturePath)
        bitmap?.recycle()
        compressBmp.recycle()

        ScriptInterpreterObserver.notifyCommandExecuteEvent(0, this)
        ScriptInterpreterObserver.notifyLogger(
            this,
            ScriptLoggerView.LogType.DEBUG,
            com.hive.i8n.R.string.sc_capture_camera.string(capturePath ?: "")
        )

        writeParam(targetParamId, capturePath)
        if (saveToGallery && capturePath != null) {
            ScriptHelper.saveToGallery(capturePath!!)
        }
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Capture_Camera)
        return CmdExecuteResult.success(capturePath)
    }

    private fun takePicture(cameraId: Int, enableFlash: Boolean, callback: (Bitmap?, String?) -> Unit) {
        CaptureCameraHelper.takePicture(cameraId, enableFlash, callback)
    }

    override fun doExecute(): CmdExecuteResult {
        return super.doExecute()
    }

    override fun isSupportDelay() = true

    override fun isSupportRect() = false

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommand() = "${cmdPrefix()} camera=$cameraId flash=${if (enableFlash) 1 else 0} output=$targetParamId gallery=$saveToGallery"

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_capture_camera)

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_capture_camera)

    override fun getCommandIcon() = R.drawable.sc_ic_code

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        cameraId = p["camera"]?.toIntOrNull() ?: 0
        enableFlash = p["flash"]?.toIntOrNull() == 1
        targetParamId = p["output"] ?: targetParamId
        saveToGallery = p["gallery"]?.toBooleanStrictOrNull() ?: false
    }

    override fun getNormalizedActiveArea() = RectF(0f, 0f, 1f, 1f)

    override fun getPermissionRequest() = mutableListOf(
        ScriptHelper.PERMISSION_CAMERA,
    )

    companion object {
        fun createCommand(cameraId: Int, paramFullId: String?, enableFlash: Boolean = false, saveToGallery: Boolean = false) = CmdCaptureCamera().apply {
            this.cameraId = cameraId
            this.enableFlash = enableFlash
            this.saveToGallery = saveToGallery
            targetParamId = paramFullId ?: "main.param0"
        }
    }
}

