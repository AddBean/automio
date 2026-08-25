// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.condition

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.os.Build
import com.hive.script.base.ScriptConst
import java.util.Stack

class ConditionNotificationProcessor {

    private val maxCacheCount = 10


    /**
     * Stack to store the last 10 notifications
     */
    private var lastNotifications: Stack<Pair<Notification, Long>> = Stack()


    /**
     * Push notification to stack,max size is 10
     */
    fun pushNotification(notification: Notification?) {
        removeExpiredNotification()
        notification ?: return
        if (lastNotifications.size >= maxCacheCount) {
            lastNotifications.removeAt(0)
        }
        lastNotifications.push(notification to System.currentTimeMillis())
    }

    /**
     * Get the appInfo from the notification
     */
    fun getAppInfoFromNotification(notification: Notification): ApplicationInfo? {
        val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notification.extras?.getParcelable(
                "android.appInfo",
                ApplicationInfo::class.java
            )
        } else {
            notification.extras?.getParcelable("android.appInfo") as? ApplicationInfo
        }
        return appInfo
    }

    /**
     * Check if the notification contains the keyword, if so, return the notification,and remove it from the stack,
     * filter the notification by appList
     */
    fun checkNotification(keyword: String?, appList: List<String>?): Notification? {
        keyword ?: return null
        removeExpiredNotification()
        for (i in lastNotifications.indices) {
            val notification = lastNotifications[i].first
            val title = notification.extras?.get("android.title") as? String
                ?: ""
            val text = notification.extras?.get("android.text") as? String
                ?: ""

            var titleContains = title.contains(keyword)
            var textContains = text.contains(keyword)
            if (keyword == ScriptConst.NONE_CHAR) {
                titleContains = true
                textContains = true
            }
            val tickerContains =
                notification.tickerText != null && notification.tickerText.toString()
                    .contains(keyword)
            if (tickerContains || titleContains || textContains) {
                val appInfo = getAppInfoFromNotification(notification)
                if (!appList.isNullOrEmpty()) {
                    for (app in appList) {
                        if (appInfo?.packageName == app) {
                            return lastNotifications.removeAt(i).first
                        }
                    }
                    return null
                } else {
                    return lastNotifications.removeAt(i).first
                }
            }
        }
        return null
    }

    /**
     * 仅保留最近5s的通知,删除过期的通知
     */
    private fun removeExpiredNotification() {
        val currentTime = System.currentTimeMillis()
        val iterator = lastNotifications.iterator()
        while (iterator.hasNext()) {
            val pair = iterator.next()
            if (currentTime - pair.second > 2000) {
                iterator.remove()
            }
        }
    }


    companion object {

        private var serviceInstance: ConditionNotificationProcessor? = null

        private var accessInstance: ConditionNotificationProcessor? = null

        /**
         * Get the instance of the service
         */
        fun serviceInstance(): ConditionNotificationProcessor {
            if (serviceInstance == null) {
                serviceInstance = ConditionNotificationProcessor()
            }
            return serviceInstance!!
        }

        /**
         * Get the instance of the accessibility
         */
        fun accessInstance(): ConditionNotificationProcessor {
            if (accessInstance == null) {
                accessInstance = ConditionNotificationProcessor()
            }
            return accessInstance!!
        }


    }

}