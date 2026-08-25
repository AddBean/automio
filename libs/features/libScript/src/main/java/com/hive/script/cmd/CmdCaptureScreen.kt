// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.graphics.RectF
import com.hive.script.ActivityRequestPermissionCapture
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.ScriptScreenShotService
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.utils.CaptureUtil
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.logger.ScriptLoggerView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string
import com.hive.utils.utils.BitmapUtils

/**
 * 截屏命令：支持 Android 14+ MediaProjection 自动恢复。
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdCaptureScreen, name = "captureScreen")
class CmdCaptureScreen : ScriptCommand(), ScriptRegularInterface {

    var targetParamId = ScriptParamEnv.getDefaultParam()?.getFullId()

    /** 是否保存到本机相册，默认 false */
    var saveToGallery = false

    var capturePath: String? = null

    override fun onExecute(): CmdExecuteResult {
        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Capture)

        // 尝试截屏，如果 MediaProjection 失效则自动恢复
        var bmp = ScriptScreenShotService.instance?.getScreenShot()
        if (bmp == null && ScriptScreenShotService.awaitPermissionReady(1200)) {
            bmp = ScriptScreenShotService.instance?.getScreenShot()
        }

        if (bmp == null) {
            // MediaProjection 可能失效，尝试自动恢复
            ScriptInterpreterObserver.notifyLogger(
                this,
                ScriptLoggerView.LogType.WARN,
                com.hive.i8n.R.string.sc_capture_screen_projection_invalid.string()
            )

            val recovered = recoverMediaProjection()
            if (!recovered) {
                return CmdExecuteResult.failure(
                    GlobalApp.getString(com.hive.i8n.R.string.sc_capture_screen_failed)
                )
            }

            if (!ScriptScreenShotService.awaitPermissionReady(3000)) {
                return CmdExecuteResult.failure(
                    GlobalApp.getString(com.hive.i8n.R.string.sc_capture_screen_failed)
                )
            }

            // 恢复成功后重试截屏
            bmp = ScriptScreenShotService.instance?.getScreenShot()
                ?: return CmdExecuteResult.failure(
                    GlobalApp.getString(com.hive.i8n.R.string.sc_capture_screen_failed)
                )
        }

        val compressBmp = BitmapUtils.compressAndResize(bmp, 720, 160)

        capturePath = ScriptConst.newRandomTempImagePath("jpg")
        BitmapUtils.saveBitmapLocal(compressBmp, capturePath)
        bmp.recycle()
        compressBmp.recycle()

        ScriptInterpreterObserver.notifyCommandExecuteEvent(0, this)
        ScriptInterpreterObserver.notifyLogger(
            this,
            ScriptLoggerView.LogType.DEBUG,
            com.hive.i8n.R.string.sc_capture_screen.string(capturePath ?: "")
        )

        writeParam(targetParamId, capturePath)

        if (saveToGallery && capturePath != null) {
            ScriptHelper.saveToGallery(capturePath!!)
        }

        ScriptThreadManager.delay(ScriptConst.Cmd_Default_Base)
        return CmdExecuteResult.success(capturePath)
    }

    /**
     * 自动恢复 MediaProjection 权限。
     * Android 14+ token 失效后，暂停脚本执行，请求用户重新授权，授权后继续执行。
     *
     * @return true 表示恢复成功，false 表示用户拒绝或超时
     */
    private fun recoverMediaProjection(): Boolean {
        // 检查是否需要恢复
        if (ScriptScreenShotService.instance != null && CaptureUtil.get().isProjectionValid()) {
            return true // 投影仍然有效，无需恢复
        }

        ScriptInterpreterObserver.notifyLogger(
            this,
            ScriptLoggerView.LogType.INFO,
            com.hive.i8n.R.string.sc_capture_screen_request_permission.string()
        )

        // 暂停脚本执行，等待用户授权
        ScriptThreadManager.pause()

        var recoverySuccess = false
        var recoveryCompleted = false

        // 请求截屏权限
        ActivityRequestPermissionCapture.checkOrRequestPermission(
            ScriptProvider.getViewContext() ?: GlobalApp.getApp(),
            false,
            success = {
                recoverySuccess = true
                recoveryCompleted = true
            },
            failure = {
                recoverySuccess = false
                recoveryCompleted = true
            }
        )

        // 等待用户授权完成（最多等待 30 秒）
        val maxWaitTime = 30000L
        val checkInterval = 200L
        var waitedTime = 0L

        while (!recoveryCompleted && waitedTime < maxWaitTime) {
            try {
                Thread.sleep(checkInterval)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
            waitedTime += checkInterval
        }

        // 恢复脚本执行
        ScriptThreadManager.resume()

        if (!recoverySuccess) {
            ScriptInterpreterObserver.notifyLogger(
                this,
                ScriptLoggerView.LogType.ERROR,
                com.hive.i8n.R.string.sc_capture_screen_permission_denied.string()
            )
        }

        return recoverySuccess && ScriptScreenShotService.awaitPermissionReady(3000)
    }

    override fun doExecute(): CmdExecuteResult {
        return super.doExecute()
    }

    override fun isSupportDelay() = true

    override fun isSupportRect() = false

    private fun getCommandDuration() = ScriptConst.Cmd_Default_Bias

    override fun getCommand() = "${cmdPrefix()} output=$targetParamId gallery=$saveToGallery"

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_capture_screen)

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_capture_screen)

    override fun getCommandIcon() = R.drawable.sc_ic_code

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        targetParamId = p["output"] ?: targetParamId
        saveToGallery = p["gallery"]?.toBooleanStrictOrNull() ?: false
    }

    override fun getNormalizedActiveArea() = RectF(0f, 0f, 1f, 1f)

    override fun getPermissionRequest() = mutableListOf(ScriptHelper.PERMISSION_CAPTURE)

    companion object {
        fun createCommand(paramFullId: String?, saveToGallery: Boolean = false) = CmdCaptureScreen().apply {
            targetParamId = paramFullId ?: "main.param0"
            this.saveToGallery = saveToGallery
        }
    }
}
