// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.driver

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.hive.script.BuildConfig
import com.hive.script.R
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.condition.ConditionNotificationProcessor
import com.hive.script.utils.CaptureUtil
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptMenuManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.menu.ScriptControlView
import com.hive.utils.GlobalApp
import com.hive.utils.thread.UIHandlerUtils
import com.hive.views.widgets.CommonToast
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random


/**
 * Created by AddBean on 2016/10/13.
 */
class ServiceAccessibility : AccessibilityService() {

    private lateinit var mBroadcastReceiver: InnerBroadcastReceiver
    private val maxCount = 8

    private val maxTime = 2500L

    private var mHints = LongArray(maxCount) //初始全部为0

    private var showFlag = false


    override fun onCreate() {
        super.onCreate()
        mBroadcastReceiver = InnerBroadcastReceiver()
        try {
            val filter = IntentFilter().apply { addAction(ACTION_STOP) }
            ContextCompat.registerReceiver(this, mBroadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        ScriptEventHelper.get().initService(this)
        startNotificationChannel()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ScriptKeepAliveHelper.get().start()
        ScriptEventHelper.get().initService(this)
        ScriptHelper.runInMain({
            ScriptManager.releaseAllViews()
            ScriptEventHelper.get().initService(this)
            ScriptManager.start(false)
            CommonToast.getInstance().showToastLong(com.hive.i8n.R.string.service_access_start_success)
            ScriptEventHelper.get().performBackToApp()
        }, 500)
    }

    private fun startNotificationChannel() {
        val builder: Notification.Builder = Notification.Builder(this)//获取一个Notification构造器
        builder.setLargeIcon(
            BitmapFactory.decodeResource(
                this.resources, com.hive.i8n.R.drawable.logo
            )
        ) // 设置下拉列表中的图标(大图标)
            .setSmallIcon(com.hive.i8n.R.drawable.logo) // 设置状态栏内的小图标
            .setContentText(getString(com.hive.i8n.R.string.sc_shot_service_context)) // 设置上下文内容
            .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "notification_id", "notification_name", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notificationIntent = Intent(this, GlobalApp.getMainActivityClass());
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 0 or PendingIntent.FLAG_MUTABLE
        );

        val notification: Notification =
            builder.setContentTitle(getString(com.hive.i8n.R.string.app_name) + getString(com.hive.i8n.R.string.script_running_title))
                .setContentText(getString(com.hive.i8n.R.string.script_running_text))
                .setContentIntent(pendingIntent)
                .setTicker(getString(com.hive.i8n.R.string.script_running_ticker)).build()
        notification.defaults = Notification.DEFAULT_SOUND //设置为默认的声音
        startForeground(111, notification)
    }

    fun preformPress(x: Int, y: Int, duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dispatchGesture(GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(Path().apply {
                    moveTo(x.toFloat(), y.toFloat())
                }, 10L, duration)).build(), object : GestureResultCallback() {}, null
            )
            ScriptThreadManager.delay(10L + duration)
        }
    }

    fun preformClick(x: Int, y: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dispatchGesture(
                GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(Path().apply {
                        moveTo(x.toFloat(), y.toFloat())
                    }, 10L, ScriptConst.Cmd_Click_Default)).build(),
                object : GestureResultCallback() {
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                    }

                    override fun onCompleted(gestureDescription: GestureDescription?) {
                    }
                },
                null
            )
            ScriptThreadManager.delay(10L + ScriptConst.Cmd_Click_Default)
        }
    }

    fun performScale(points1: List<Point>, points2: List<Point>, duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val p1 = GestureDescription.StrokeDescription(Path().apply {
                for (i in points1.indices) {
                    if (i == 0) {
                        moveTo(points1[i].x.toFloat(), points1[i].y.toFloat())
                    } else {
                        lineTo(points1[i].x.toFloat(), points1[i].y.toFloat())
                    }
                }
            }, 10L, duration)
            val p2 = GestureDescription.StrokeDescription(Path().apply {
                for (i in points2.indices) {
                    if (i == 0) {
                        moveTo(points2[i].x.toFloat(), points2[i].y.toFloat())
                    } else {
                        lineTo(points2[i].x.toFloat(), points2[i].y.toFloat())
                    }
                }
            }, 10L, duration)
            val gestureDescription =
                GestureDescription.Builder().addStroke(p1).addStroke(p2).build()
            dispatchGesture(gestureDescription, object : GestureResultCallback() {
                override fun onCancelled(gestureDescription: GestureDescription?) {
                }

                override fun onCompleted(gestureDescription: GestureDescription?) {
                }
            }, null)
            ScriptThreadManager.delay(10L + duration)
        }
    }

    fun performMultipleScroll(pointsList: MutableList<MutableList<Point>>, duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pointsList.forEach { points ->
                points.forEach {
                    if (it.x < 0) {
                        it.x = 0
                    }
                    if (it.y < 0) {
                        it.y = 0
                    }
                }
            }

            val builder = GestureDescription.Builder()
            pointsList.forEach {
                builder.addStroke(GestureDescription.StrokeDescription(Path().apply {
                    for (i in it.indices) {
                        if (i == 0) {
                            moveTo(it[i].x.toFloat(), it[i].y.toFloat())
                        } else {
                            lineTo(it[i].x.toFloat(), it[i].y.toFloat())
                        }
                    }
                }, 10L, duration))
            }
            val gestureDescription = builder.build()
            dispatchGesture(gestureDescription, object : GestureResultCallback() {
                override fun onCancelled(gestureDescription: GestureDescription?) {
                }

                override fun onCompleted(gestureDescription: GestureDescription?) {
                }
            }, null)
            ScriptThreadManager.delay(10L + duration)
        }
    }


    fun performScrollMultiple(
        pointMap: MutableMap<Int, MutableList<Point>>,
        timesMap: MutableMap<Int, MutableList<Long>>,
        startTimeMap: MutableMap<Int, Long>
    ) {//仿滑动
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val builder = GestureDescription.Builder()
            pointMap.forEach { it ->
                val points = it.value
                val startTime = startTimeMap[it.key]
                points.forEach {
                    if (it.x < 0) {
                        it.x = 0
                    }
                    if (it.y < 0) {
                        it.y = 0
                    }
                }
                val path = Path()
                for (i in points.indices) {
                    if (i == 0) {
                        path.moveTo(points[i].x.toFloat(), points[i].y.toFloat())
                    } else {
                        path.lineTo(points[i].x.toFloat(), points[i].y.toFloat())
                    }
                }
                val duration = timesMap[it.key]?.sum() ?: 0
                builder.addStroke(GestureDescription.StrokeDescription(path, startTime!!, duration))
            }
//            val durationMax =
//                timesMap.map { it.value.sum() }.max() + startTimeMap.map { it.value }.max()
            val gestureRunning = AtomicBoolean(true)
            dispatchGesture(builder.build(), object : GestureResultCallback() {
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    gestureRunning.set(false)
                }

                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    gestureRunning.set(false)
                }
            }, null)
            while (gestureRunning.get()) {
                ScriptThreadManager.delay(100)
            }
        }
    }

    private fun performLongPressWithScrollBelow26(
        startPoint: Point,
        endPoint: Point,
        pressDuration: Long,
        moveDuration: Long
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val builder = GestureDescription.Builder()
            val path = Path()
            path.moveTo(startPoint.x.toFloat(), startPoint.y.toFloat())

            val jitterRadius = 10

            // Add jitter effect
            val jitterInterval = 100L // Interval between jitters in milliseconds
            val jitterCount = (pressDuration / jitterInterval).toInt()
            for (i in 1..jitterCount) {
                val jitterX = startPoint.x + Random.nextInt(-jitterRadius, jitterRadius)
                val jitterY = startPoint.y + Random.nextInt(-jitterRadius, jitterRadius)
                path.lineTo(jitterX.toFloat(), jitterY.toFloat())
            }
            path.lineTo(endPoint.x.toFloat(), endPoint.y.toFloat())

            builder.addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0L,
                    pressDuration + moveDuration
                )
            )
            val gestureRunning = AtomicBoolean(true)
            dispatchGesture(builder.build(), object : GestureResultCallback() {
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    gestureRunning.set(false)
                }

                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    gestureRunning.set(false)
                }
            }, null)
            while (gestureRunning.get()) {
                ScriptThreadManager.delay(100)
            }
        }
    }


    fun performLongPressWithScroll(
        startPoint: Point, endPoint: Point, pressDuration: Long, moveDuration: Long
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val random = Random.nextInt(-5, 5)
            val sd = GestureDescription.StrokeDescription(
                Path().apply {
                    moveTo(startPoint.x.toFloat(), startPoint.y.toFloat())
                    lineTo(startPoint.x.toFloat() + random, startPoint.y.toFloat() + random)
                }, 0L, pressDuration, true
            )
            val sd2 = sd.continueStroke(Path().apply {
                moveTo(startPoint.x.toFloat() + random, startPoint.y.toFloat() + random)
                lineTo(endPoint.x.toFloat(), endPoint.y.toFloat())
            }, 0L, moveDuration, false)

            val gestureRunning = AtomicBoolean(true)

            val resultCallback2 = object : GestureResultCallback() {
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    gestureRunning.set(false)
                }

                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    gestureRunning.set(false)
                }
            }

            val resultCallback1 = object : GestureResultCallback() {

                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    dispatchGesture(
                        GestureDescription.Builder().addStroke(sd2).build(), resultCallback2, null
                    )
                }
            }
            dispatchGesture(
                GestureDescription.Builder().addStroke(sd).build(),
                resultCallback1,
                null
            )
            while (gestureRunning.get()) {
                ScriptThreadManager.delay(100)
            }
        } else {
            performLongPressWithScrollBelow26(startPoint, endPoint, pressDuration, moveDuration)
        }
    }

    fun performScroll(
        points: List<Point>, times: List<Long>, startTime: Long, duration: Long
    ) {//仿滑动
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            points.forEach {
                if (it.x < 0) {
                    it.x = 0
                }
                if (it.y < 0) {
                    it.y = 0
                }
            }
            val builder = GestureDescription.Builder()
            val path = Path()
            for (i in points.indices) {
                if (i == 0) {
                    path.moveTo(points[i].x.toFloat(), points[i].y.toFloat())
                } else {
                    path.lineTo(points[i].x.toFloat(), points[i].y.toFloat())
                }
            }
            builder.addStroke(
                GestureDescription.StrokeDescription(
                    path, startTime, duration
                )
            )
            val gestureRunning = AtomicBoolean(true)
            dispatchGesture(builder.build(), object : GestureResultCallback() {
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    gestureRunning.set(false)
                }

                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    gestureRunning.set(false)
                }
            }, null)
            while (gestureRunning.get()) {
                ScriptThreadManager.delay(100)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (BuildConfig.DEBUG) {
//            DLog.e(
//                ">>>>>", "AccessibilityEvent:\n" +
//                        "type : ${event.eventType} \n" +
//                        "text : ${event.text} \n" +
//                        "time : ${event.eventTime} \n" +
//                        "action : ${event.action} \n" +
//                        "contentChangeTypes : ${event.contentChangeTypes} \n" +
//                        "recordCount : ${event.recordCount} \n" +
//                        "windowChanges : ${event.windowChanges} \n" +
//                        "packageName:  ${event.packageName}"
//            )
        }


        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED, AccessibilityEvent.TYPE_VIEW_CLICKED, AccessibilityEvent.TYPE_GESTURE_DETECTION_END, AccessibilityEvent.TYPE_TOUCH_INTERACTION_START, AccessibilityEvent.TYPE_GESTURE_DETECTION_START, AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START -> {
                ScriptEventHelper.get().onUserTouchEvent?.invoke()
            }

            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                ConditionNotificationProcessor.accessInstance()
                    .pushNotification(event.parcelableData as? Notification)
            }

            else -> {
                ScriptEventHelper.get().accessibilityViewEvent = event
            }
        }
    }


    override fun onInterrupt() {
        CommonToast.show(com.hive.i8n.R.string.sc_accessibility_serice_interrupt_msg)
        ScriptEventHelper.get().initService(null)
    }


    override fun onKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP -> {
                checkNeedExit()
            }
        }
        return false
    }

    private fun checkNeedExit() {
        System.arraycopy(mHints, 1, mHints, 0, mHints.size - 1)
        //获得当前系统已经启动的时间
        mHints[mHints.size - 1] = System.currentTimeMillis()
        if (System.currentTimeMillis() - mHints[0] <= maxTime) {
            mHints = LongArray(maxCount)
            for (i in mHints.indices) {
                mHints[i] = 0L
            }
            if (!showFlag) {
                showFlag = true
                ScriptManager.stopPlay()
                ScriptRecordManager.hiddenRecordView()
                ScriptMenuManager.switchMenuMode(ScriptControlView.MenuMode.MAIN_MENU)
//                DialogPlayStop(ScriptProvider.getViewContext()).loadCmd(null, 0L).show()
                CommonToast.show(com.hive.i8n.R.string.sc_click_to_exit)
                ScriptHelper.runInMain({ showFlag = false }, maxTime)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ScriptKeepAliveHelper.get().stop()
        unregisterReceiver(mBroadcastReceiver)
        stopForeground(true)
        ScriptEventHelper.get().initService(null)
        ScriptMenuManager.hiddenMenuView()
        ScriptManager.releaseAllViews()
        ScriptInterpreter.getDefault().stopExecute()
    }

    override fun getRootInActiveWindow(): AccessibilityNodeInfo? {
        return try {
            super.getRootInActiveWindow()
        } catch (e: Exception) {
            null
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        ScriptManager.updateViewLayout()
        ScriptHelper.runInMain({ ScriptManager.updateViewLayout() }, 300L)
        CaptureUtil.get().onConfigurationChanged(newConfig)
    }

    inner class InnerBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_STOP -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        disableSelf()
                    }
                }
            }
        }

    }

    companion object {

        val ACTION_STOP = "${GlobalApp.getApp().packageName}.script_service_stop"

        fun stopServiceIntent() {
            GlobalApp.getApp().sendBroadcast(Intent(ACTION_STOP))
        }
    }
}
