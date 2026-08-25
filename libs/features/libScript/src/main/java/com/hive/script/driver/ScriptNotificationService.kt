// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.driver

import android.app.Activity
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.hive.script.condition.ConditionNotificationProcessor
import com.hive.utils.GlobalApp

class ScriptNotificationService : NotificationListenerService() {


    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        val appInfo = ConditionNotificationProcessor.serviceInstance()
            .getAppInfoFromNotification(sbn.notification)
        // 如果不是当前应用的通知,则推送通知
        if (appInfo?.packageName != GlobalApp.getPackageName()) {
            ConditionNotificationProcessor.serviceInstance().pushNotification(sbn.notification)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)

    }

    companion object {
        private var instance: ScriptNotificationService? = null

        fun isAlive(): Boolean {
            return instance != null
        }

        fun start(activity: Activity) {
            if (!isAlive()) {
                val serviceIntent = Intent(activity, ScriptNotificationService::class.java)
                activity.startService(serviceIntent)
            }
        }
    }
}