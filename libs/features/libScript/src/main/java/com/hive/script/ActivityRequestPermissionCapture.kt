// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import com.hive.script.driver.ScriptEventHelper
import com.hive.utils.GlobalApp
import com.hive.utils.utils.IntentUtils

/**
 * 请求 Media Projection（截屏/录屏）权限的透明 Activity。
 * Android 14 (API 34+) 下使用 [MediaProjectionConfig.createConfigForDefaultDisplay()]
 * 将授权限定为「仅全屏」，避免用户选择单应用导致脚本无法截取全屏。
 *
 * @author jiadou
 * @date 6/18/21
 */
class ActivityRequestPermissionCapture : Activity() {

    private val MEDIA_PROJECTION_REQUEST_CODE = 11
    private var mediaProjectionManager: MediaProjectionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+：限定为仅全屏截取，不展示「单应用」选项
            mediaProjectionManager?.createScreenCaptureIntent(
                MediaProjectionConfig.createConfigForDefaultDisplay()
            )
        } else {
            mediaProjectionManager?.createScreenCaptureIntent()
        }
        startActivityForResult(captureIntent, MEDIA_PROJECTION_REQUEST_CODE)
        ScriptEventHelper.get().tryAutoGrantPermission()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //处理截屏权限申请后的结果
        if (RESULT_OK == resultCode && MEDIA_PROJECTION_REQUEST_CODE == requestCode) {
            ScriptScreenShotService.start(this, resultCode, data)
            successFun?.invoke()
        } else {
            failureFun?.invoke()
        }
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        failureFun = null
        successFun = null
    }

    companion object {

        var successFun: (() -> Unit)? = null

        var failureFun: (() -> Unit)? = null

        var showRecord = false

        fun checkOrRequestPermission(
            context: Context?,
            showRecord: Boolean = false,
            success: (() -> Unit)?,
            failure: (() -> Unit)?
        ) {
            if (ScriptScreenShotService.checkPermission()) {
                success?.invoke()
                return
            }
            val cxt = context ?: GlobalApp.getApp()
            this.failureFun = failure
            this.successFun = success
            this.showRecord = showRecord
            IntentUtils.safeStartActivity(cxt, Intent(cxt, ActivityRequestPermissionCapture::class.java))
        }

    }
}
