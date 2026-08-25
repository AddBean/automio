// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.content.ContextCompat
import com.hive.timer.R
import com.hive.timer.utils.AlarmTimerUtils
import com.hive.utils.GlobalApp
import com.hive.utils.utils.IntentUtils
import com.hive.utils.utils.StringUtils

class HiveAlarmService : Service() {

    private val alarmTimer = HiveAlarmTimer()

    override fun onBind(intent: Intent?) = null


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        postNotificationChannel(
            getString(com.hive.i8n.R.string.timer_service_title),
            getString(com.hive.i8n.R.string.timer_service_content)
        )
        alarmTimer.start {
            if (it != null) {
                postNotificationChannel(
                    getString(
                        com.hive.i8n.R.string.timer_service_running_title,
                        AlarmTimerUtils.formatHHMMSSTime(it.time - System.currentTimeMillis())
                    ),
                    getString(
                        com.hive.i8n.R.string.timer_service_running_content,
                        StringUtils.dateFormat(it)
                    )
                )
            } else {
                postNotificationChannel(
                    getString(com.hive.i8n.R.string.timer_service_title),
                    getString(com.hive.i8n.R.string.timer_service_content)
                )
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun postNotificationChannel(title: String, content: String) {
        val builder: Notification.Builder = Notification.Builder(this)//获取一个Notification构造器
        builder.setLargeIcon(
            BitmapFactory.decodeResource(
                this.resources,
                com.hive.i8n.R.drawable.logo
            )
        ) // 设置下拉列表中的图标(大图标)
            .setSmallIcon(com.hive.i8n.R.drawable.logo) // 设置状态栏内的小图标
            .setContentTitle(title)
            .setContentText(content) // 设置上下文内容
            .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "notification_id",
                "notification_name",
                NotificationManager.IMPORTANCE_LOW
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
        startForeground(111, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        alarmTimer.stop()
    }

    companion object {

        @JvmStatic
        fun start(ctx: Context): Boolean {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.FOREGROUND_SERVICE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                GlobalApp.getAvailableActivity() ?: return false
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ctx.startForegroundService(Intent(ctx, HiveAlarmService::class.java))
                    } else {
                        IntentUtils.safeStartService(
                            ctx,
                            Intent(ctx, HiveAlarmService::class.java)
                        )
                    }
                    return true
                } catch (e: SecurityException) {
                    // Handle security exception gracefully
                    return false
                }
            }
            return false
        }
    }
}
