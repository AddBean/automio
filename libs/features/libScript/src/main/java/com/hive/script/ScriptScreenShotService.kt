// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.os.Build
import com.hive.script.utils.CaptureUtil
import com.hive.utils.GlobalApp
import com.hive.utils.utils.IntentUtils

/**
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/29/21
 */
class ScriptScreenShotService : Service() {


    override fun onBind(intent: Intent?) = null


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            createNotificationChannel()
            instance = this
            if (sResultCode == -1000) return START_NOT_STICKY
            val data = intent?.extras?.get("data") as Intent?
            if (data == null && !CaptureUtil.get().isProjectionValid()) {
                stopSelf()
                return START_NOT_STICKY
            }
            val mediaProjectionManager =
                getSystemService(Activity.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            CaptureUtil.get().init(mediaProjectionManager, sResultCode, data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_NOT_STICKY
    }

    fun getScreenShot(): Bitmap? =
        CaptureUtil.get().screenCapture


    private fun createNotificationChannel() {
        val builder: Notification.Builder = Notification.Builder(this)//获取一个Notification构造器
        builder.setLargeIcon(
            BitmapFactory.decodeResource(
                this.resources,
                com.hive.i8n.R.drawable.logo
            )
        ) // 设置下拉列表中的图标(大图标)
            .setSmallIcon(com.hive.i8n.R.drawable.logo) // 设置状态栏内的小图标
            .setContentText(getString(com.hive.i8n.R.string.sc_shot_service_context)) // 设置上下文内容
            .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id")
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "notification_id",
                "notification_name",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notificationIntent = Intent(this, GlobalApp.getMainActivityClass());
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            0 or PendingIntent.FLAG_IMMUTABLE
        );

        val notification: Notification =
            builder.setContentIntent(pendingIntent).build() // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_VIBRATE //设置为默认的声音
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(110, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(110, notification)
        }
    }

    override fun onDestroy() {
        try {
            CaptureUtil.get().release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        instance = null
        super.onDestroy()
    }

    companion object {
        var instance: ScriptScreenShotService? = null
        var sResultCode = -1000

        /**
         * 停止截屏服务并释放 MediaProjection。工作流结束时调用，避免持续占用屏幕投影。
         */
        fun stop(ctx: Context?) {
            val context = ctx ?: GlobalApp.getApp()
            context.stopService(Intent(context, ScriptScreenShotService::class.java))
        }

        fun start(ctx: Context, resultCode: Int?, data: Intent?) {
            sResultCode = resultCode ?: -1000
            val intent = Intent(ctx, ScriptScreenShotService::class.java).apply {
                putExtra("data", data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun awaitPermissionReady(timeoutMs: Long = 2000L): Boolean {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                if (checkPermission()) {
                    return true
                }
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return false
                }
            }
            return checkPermission()
        }

        /**
         * 检查截屏权限是否有效。
         * Android 14+ MediaProjection token 失效后需要重新授权。
         */
        fun checkPermission(): Boolean {
            return instance != null && CaptureUtil.get().isProjectionValid()
        }
    }
}
