// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.driver

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent.CanceledException
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.media.SoundPool
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.PowerManager
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.hive.permissions.PermissionsChecker
import com.hive.plugin.ComponentManager
import com.hive.plugin.ocr.OcrResult
import com.hive.plugin.provider.IOpenCVProvider
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.ScriptScreenShotService
import com.hive.script.base.ScriptConst
import com.hive.script.setting.ScriptSetting
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.script.utils.sortByPriority
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.utils.CommomListener
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.utils.thread.UIHandlerUtils
import com.hive.utils.utils.BitmapUtils
import com.hive.utils.utils.IntentUtils
import com.hive.views.widgets.CommonToast
import java.util.concurrent.CountDownLatch
import kotlin.random.Random
import com.hive.script.utils.ScriptHelper
import com.hive.utils.LanguageManager

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/9/21
 */
class ScriptEventHelper {

    var serviceEntity: ServiceAccessibility? = null

    var accessibilityViewEvent: AccessibilityEvent? = null

    var onUserTouchEvent: (() -> Unit)? = null


    fun initService(service: ServiceAccessibility?) {
        serviceEntity = service
        LanguageManager.setLanguage(serviceEntity?.baseContext ?: GlobalApp.getContext())
        CommonToast.initContext(ScriptProvider.getViewContext())
    }

    private var openCVProvider =
        ComponentManager.getInstance().getProvider(IOpenCVProvider::class.java) as IOpenCVProvider?

    fun getCurrentClickNodeList(clickViewType: ScriptRecordManager.RecordClickViewType?): MutableList<AccessibilityNodeInfo>? {
        val list = mutableListOf<AccessibilityNodeInfo>()
        serviceEntity?.rootInActiveWindow?.run {
            traverseNode(this, CommomListener.Callback2 { node ->
                val n = node as AccessibilityNodeInfo
                if (
                //node.childCount == 0 &&
                    (!TextUtils.isEmpty(n.text) || !TextUtils.isEmpty(n.viewIdResourceName) || !TextUtils.isEmpty(
                        n.contentDescription
                    ))
                ) {
                    if (clickViewType == ScriptRecordManager.RecordClickViewType.SELECT_VIEW) {
                        if (!TextUtils.isEmpty(n.viewIdResourceName)) {
                            list.add(n)
                        }
                    } else if (clickViewType == ScriptRecordManager.RecordClickViewType.SELECT_EDIT_VIEW) {
                        if (n.isEditable && n.isFocusable
                        ) {
                            list.add(n)
                        }
                    } else if (clickViewType == ScriptRecordManager.RecordClickViewType.CLICK_VIEW) {
                        list.add(n)
                    } else if (clickViewType == ScriptRecordManager.RecordClickViewType.INPUT_VIEW) {
                        if (n.isEditable && n.isFocusable
                        ) {
                            list.add(n)
                        }
                    } else if (clickViewType == ScriptRecordManager.RecordClickViewType.READ_VIEW_TEXT) {
                        if (n.className != null && n.viewIdResourceName != null) {
                            list.add(n)
                        }
                    } else {
                        list.add(n)
                    }
                }
                return@Callback2 false
            })
        }

        return list
    }

    private fun traverseNode(node: AccessibilityNodeInfo, cb: CommomListener.Callback2): Boolean {
//        DLog.e(node.viewIdResourceName)
        if (!node.isVisibleToUser) {
            return false
        }
        cb.onEvent(node)
        if (node.childCount == 0) {
            return false
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.run {
                if (traverseNode(this, cb)) {
                    return true
                }
            }
        }
        return false
    }

    fun openNotification(event: AccessibilityEvent) {
        if (event.parcelableData != null && event.parcelableData is Notification) {
            val notification = event.parcelableData as Notification?
            val pendingIntent = notification!!.contentIntent
            try {
                val intent = Intent()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                pendingIntent.send(GlobalApp.getContext(), 0, intent)
                DLog.e("ScriptEventHelper", "openNotification")
            } catch (e: CanceledException) {
                e.printStackTrace()
            }
        }
    }


    fun openNotification(notification: Notification) {
        val pendingIntent = notification!!.contentIntent
        try {
            val intent = Intent()
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            pendingIntent.send(GlobalApp.getContext(), 0, intent)
            DLog.e("ScriptEventHelper", "openNotification")
        } catch (e: CanceledException) {
            e.printStackTrace()
        }
    }

    fun performFindLayoutInText(
        targetText: String?,
        bottomToTop: Boolean = false
    ): AccessibilityNodeInfo? {
        serviceEntity?.rootInActiveWindow?.let {
            if (targetText != null && targetText != ScriptConst.NONE_CHAR) {
                val targetNodeList =
                    it.findAccessibilityNodeInfosByText(targetText)?.takeIf { it.isNotEmpty() }
                        ?.toList()
                return if (bottomToTop) {
                    targetNodeList?.reversed()?.firstOrNull()
                } else {
                    targetNodeList?.firstOrNull()
                }
            }
        }
        return null
    }

    fun performFindEditText(
        targetId: String?,
        limitRect: RectF?,
        callback: ((targetNodes: List<AccessibilityNodeInfo>?) -> Unit)? = null
    ): List<AccessibilityNodeInfo>? {
        serviceEntity?.rootInActiveWindow?.let {
            val targetNodes = mutableListOf<AccessibilityNodeInfo>()
            if (targetId != null && targetId != ScriptConst.NONE_CHAR) {
                val foundNodes = it.findAccessibilityNodeInfosByViewId(targetId)
                if (foundNodes != null) {
                    targetNodes.addAll(foundNodes.filter { it.isVisibleToUser })
                }
            } else {
                traverseNode(it, CommomListener.Callback2 { node ->
                    val n = node as AccessibilityNodeInfo
                    if (n.isEditable() && n.isFocusable() && n.isVisibleToUser) {
                        targetNodes.add(n)
                    }
                    return@Callback2 false
                })
            }

            // 按屏幕视觉顺序排序（从上到下，从左到右）
            targetNodes.sortWith(compareBy<AccessibilityNodeInfo> {
                val bounds = Rect()
                it.getBoundsInScreen(bounds)
                bounds.top
            }.thenBy {
                val bounds = Rect()
                it.getBoundsInScreen(bounds)
                bounds.left
            })

            if (targetNodes.isEmpty()) {
                //则默认使用当前焦点
                val focusedNode = it.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focusedNode.isEditable() && focusedNode.isFocusable()) {
                    val outBounds = Rect()
                    focusedNode.getBoundsInScreen(outBounds)
                    if (outBounds.centerX() > 0 && outBounds.centerY() > 0 && focusedNode.isVisibleToUser) {
                        targetNodes.add(focusedNode)
                    }
                }
                callback?.invoke(targetNodes)
                return targetNodes
            } else {
                val filteredNodes =
                    targetNodes.filter { it.isEditable() && it.isFocusable() }
                        .filter { it.isVisibleToUser }
                val nodesInRect = findNodeInRect(limitRect, filteredNodes)

                if (nodesInRect != null && nodesInRect.isNotEmpty()) {
                    callback?.invoke(nodesInRect)
                    return nodesInRect
                }
                return if (filteredNodes.isNotEmpty()) filteredNodes else null
            }
        }
        return null
    }

    fun performFindLayout(
        targetId: String?,
        targetText: String?,
        targetTag: String?,
        type: Int,//0 contains, 1 equals
        direction: Int,//0 优先左上角、1 优先右上角、2 优先左下角、3 优先右下角
        limitRect: RectF?,
        callback: ((targetNode: AccessibilityNodeInfo?, x: Int, y: Int) -> Unit)?
    ): AccessibilityNodeInfo? {
        return performFindAllLayout(targetId, targetText, targetTag, type, direction, limitRect) {
            if (it.isNotEmpty()) {
                val node = it.first().first
                val point = it.first().second
                callback?.invoke(node, point.x, point.y)
            } else {
                callback?.invoke(null, 0, 0)
            }
        }?.firstOrNull()?.first
    }


    fun performFindAllLayout(
        targetId: String?,
        targetText: String?,
        targetTag: String?,
        type: Int,//0 contains, 1 equals
        direction: Int,//0 优先左上角、1 优先右上角、2 优先左下角、3 优先右下角
        limitRect: RectF?,
        callback: ((targetNodes: List<Pair<AccessibilityNodeInfo?, Point>>) -> Unit)?
    ): List<Pair<AccessibilityNodeInfo?, Point>>? {

        serviceEntity?.rootInActiveWindow?.let {
            var targetNodes = mutableListOf<AccessibilityNodeInfo>()

            if (targetText != null && targetText != ScriptConst.NONE_CHAR) {
                if (type == 1) {
                    val targetNodeList =
                        it.findAccessibilityNodeInfosByText(targetText)?.takeIf { it.isNotEmpty() }
                            ?.toList()
                    if (targetNodeList != null) {
                        targetNodes.addAll(targetNodeList)
                    }
                } else {
                    traverseNode(it, CommomListener.Callback2 { node ->
                        val n = node as AccessibilityNodeInfo
                        if (n.text?.toString()?.contains(targetText) == true) {
                            targetNodes.add(n)
                        }
                        return@Callback2 false
                    })
                }
            }
            if (targetId != null && targetId != ScriptConst.NONE_CHAR) {
                val targetNode = it.findAccessibilityNodeInfosByViewId(targetId)
                if (targetNode != null) {
                    targetNodes.addAll(targetNode)
                }
            }
            if (targetTag != null && targetTag != ScriptConst.NONE_CHAR) {
                traverseNode(it, CommomListener.Callback2 { node ->
                    val n = node as AccessibilityNodeInfo
                    if (type == 1) {
                        if (n.contentDescription?.contains(targetTag) == true) {
                            targetNodes.add(n)
                        }

                    } else {
                        if (n.contentDescription == targetTag) {
                            targetNodes.add(n)
                        }
                    }
                    return@Callback2 false
                })
            }
            if (targetNodes.isEmpty()) {
//                CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.sc_node_notfound_msg))
                return null
            } else {

                targetNodes =
                    targetNodes.sortByPriority(targetText, targetId, targetTag).toMutableList()
                var nodeList = findNodeInRect(limitRect, targetNodes)

                fun getRectNode(node: AccessibilityNodeInfo): Rect {
                    val outBounds = Rect()
                    node.getBoundsInScreen(outBounds)
                    return outBounds
                }

                when (direction) {
                    0 -> {
                        nodeList = nodeList?.sortedBy { getRectNode(it).left }
                        nodeList = nodeList?.sortedBy { getRectNode(it).top }
                    }

                    1 -> {
                        nodeList = nodeList?.sortedByDescending { getRectNode(it).left }
                        nodeList = nodeList?.sortedBy { getRectNode(it).top }
                    }

                    2 -> {
                        nodeList = nodeList?.sortedBy { getRectNode(it).left }
                        nodeList = nodeList?.sortedByDescending { getRectNode(it).top }
                    }

                    3 -> {
                        nodeList = nodeList?.sortedByDescending { getRectNode(it).left }
                        nodeList = nodeList?.sortedByDescending { getRectNode(it).top }
                    }
                }

                val results = mutableListOf<Pair<AccessibilityNodeInfo?, Point>>()
                nodeList?.forEach { node ->
                    val outBounds = Rect()
                    node.getBoundsInScreen(outBounds)

                    val screenWidth = ScriptCoordinateAdapter.getScreenWidthByOrientation()
                    val screenHeight = ScriptCoordinateAdapter.getScreenHeightByOrientation()
                    val screenRect = Rect(0, 0, screenWidth, screenHeight)
                    //判断outBounds是否整体在屏幕之外不可见，或者部分在屏幕之外
                    if (screenRect.intersect(outBounds) || screenRect.contains(outBounds)) {
                        if (outBounds.centerX() > 0 && outBounds.centerY() > 0) {
                            results.add(Pair(node, Point(outBounds.centerX(), outBounds.centerY())))
                        }
                    }
                }
                callback?.invoke(results)
                return results
            }
        }
        return null
    }

    fun findNodeInRect(
        rectF: RectF?,
        nodes: List<AccessibilityNodeInfo>
    ): List<AccessibilityNodeInfo>? {
        if (rectF == null) return nodes
        val w = ScriptCoordinateAdapter.getScreenWidthByOrientation()
        val h = ScriptCoordinateAdapter.getScreenHeightByOrientation()
        val rect = Rect().apply {
            left = (rectF.left * w).toInt()
            top = (rectF.top * h).toInt()
            right = (rectF.right * w).toInt()
            bottom = (rectF.bottom * h).toInt()
        }
        val finds = nodes.filter {
            val outBounds = Rect()
            it.getBoundsInScreen(outBounds)
            outBounds.left < outBounds.right
                    && outBounds.top < outBounds.bottom
                    && rect.contains(outBounds)
        }
        return finds
    }

    fun performClick(x: Int, y: Int) {
        serviceEntity?.preformClick(x, y)
    }

    fun performPress(x: Int, y: Int, duration: Long) {
        serviceEntity?.preformPress(x, y, duration)
    }

    fun performScroll(points: List<Point>, times: List<Long>, duration: Long) {
        serviceEntity?.performScroll(points, times, 0L, duration)
    }

    fun performLongPressThenScroll(
        startPoint: Point,
        endPoint: Point,
        pressDuration: Long,
        duration: Long
    ) {
        serviceEntity?.performLongPressWithScroll(startPoint, endPoint, pressDuration, duration)
    }

    fun performScrollMultiple(
        points: MutableMap<Int, MutableList<Point>>,
        times: MutableMap<Int, MutableList<Long>>,
        startTimeMap: MutableMap<Int, Long>,
        duration: Long
    ) {
        serviceEntity?.performScrollMultiple(points, times, startTimeMap)
    }

    fun performScale(points1: List<Point>, points2: List<Point>, duration: Long) {
        serviceEntity?.performScale(points1, points2, duration)
    }

    fun performMultipleScroll(pointsList: MutableList<MutableList<Point>>, duration: Long) {
        serviceEntity?.performMultipleScroll(pointsList, duration)
    }

    fun performActionBack() {
        serviceEntity?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    fun performActionHome() {
        serviceEntity?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    fun performActionRecent() {
        serviceEntity?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    fun performActionScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            serviceEntity?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
        }
    }

    fun performActionLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            serviceEntity?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        }
    }

    fun performActionWakeUpScreen(onWakeUp: (() -> Unit)? = null) {
        serviceEntity ?: return
        val context = serviceEntity?.baseContext
        if (context != null) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "tag:CpuKeepRunning"
            )
            wakeLock.acquire(1000)
            onWakeUp?.invoke()
            wakeLock.release();
        }
    }

    fun isScreenLocked(): Boolean {
        serviceEntity ?: return false
        val keyguardManager =
            serviceEntity?.baseContext?.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager?
        return keyguardManager!!.isKeyguardLocked()
    }

    fun isScreenOn(): Boolean {
        serviceEntity ?: return false
        val pm =
            serviceEntity?.baseContext?.getSystemService(Context.POWER_SERVICE) as PowerManager?
        return pm?.isInteractive == true
    }

    @SuppressLint("InvalidWakeLockTag")
    fun wakeScreen() {
        try {
            //获取电源管理器对象
            val pm = GlobalApp.sContext.getSystemService(POWER_SERVICE) as PowerManager

            //获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是调试用的Tag
            val wl = pm.newWakeLock(
                PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                "bright"
            )

            //点亮屏幕
            wl.acquire();

            //得到键盘锁管理器对象
            val km =
                GlobalApp.sContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val kl = km.newKeyguardLock("unLock")

            //解锁
            kl.disableKeyguard()
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
    }

    fun isDozeMode(): Boolean {
        serviceEntity ?: return false
        val pm = serviceEntity?.baseContext?.getSystemService(POWER_SERVICE) as PowerManager?
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm!!.isDeviceIdleMode
        } else {
            isScreenOn()
        }
    }

    fun performActionNotifications() {
        serviceEntity?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
    }


    fun performBackToApp() {
        UIHandlerUtils.getInstance().executeInMainThread {
            try {
                val intent = Intent(ScriptProvider.getViewContext(), GlobalApp.getMainActivityClass())
                IntentUtils.safeStartActivity(ScriptProvider.getViewContext(), intent)
//            val checker = PermissionsChecker(GlobalApp.getAvailableActivity())
//            if (!checker.lacksPermission("android.permission.REORDER_TASKS")) {
//                val activityManager =
//                    GlobalApp.getContext()
//                        .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//                val recentTasks = activityManager.getRunningTasks(Int.MAX_VALUE)
//                var findApp = false
//                for (i in recentTasks.indices) {
//                    if (recentTasks[i].baseActivity!!.toShortString()
//                            .contains(GlobalApp.getContext().packageName)
//                    ) {
//                        activityManager.moveTaskToFront(
//                            recentTasks[i].id, ActivityManager.MOVE_TASK_WITH_HOME
//                        )
//                        findApp = true
//                    }
//                }
//                if (!findApp) {
//                    IntentUtils.safeStartActivity(ScriptProvider.getViewContext(), intent)
//                }
//            }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun tryReadOcrTextInSync(
        limitRect: RectF?
    ): Pair<String, OcrResult?>? {
        val ocrProvider = ScriptProvider.getOcrProvider()
        var bmp = ScriptScreenShotService.instance?.getScreenShot() ?: return null

        val latch = CountDownLatch(1)
        val bmpWidth = bmp.width
        val bmpHeight = bmp.height
        var limit = limitRect
        if (limit == null) {
            limit = RectF(0f, 0f, 1f, 1f)
        }

        val targetRect = Rect().apply {
            left = (limit.left * bmpWidth).toInt()
            top = (limit.top * bmpHeight).toInt()
            right = (limit.right * bmpWidth).toInt()
            bottom = (limit.bottom * bmpHeight).toInt()
        }
        if (targetRect.width() != bmp.width || targetRect.height() != bmp.height) {
            bmp = Bitmap.createBitmap(
                bmp,
                targetRect.left,
                targetRect.top, targetRect.width(), targetRect.height(), null, false
            )
        }

        var result1 = ""
        var result2: OcrResult? = null
        ocrProvider.readText(bmp, null, {}) {
            result1 = it?.blocks?.map { it.lines }
                ?.joinToString("\n") { it.joinToString("\n") { it.text } } ?: ""
            result2 = it
            latch.countDown()
        }
        bmp.recycle()
        latch.await()
        return result1 to result2
    }

    fun tryFindOcrTextInSync(
        text: String?,
        type: Int,//0 contains, 1 equals
        findDirection: Int,//0 优先左上角、1 优先右上角、2 优先左下角、3 优先右下角
        limitRect: RectF?
    ): Rect? {
        val ocrProvider = ScriptProvider.getOcrProvider()
        var bmp = ScriptScreenShotService.instance?.getScreenShot() ?: return null

        val latch = CountDownLatch(1)
        var resultRect: Rect? = null
        val bmpWidth = bmp.width
        val bmpHeight = bmp.height
        var limit = limitRect
        if (limit == null) {
            limit = RectF(0f, 0f, 1f, 1f)
        }

        val targetRect = Rect().apply {
            left = (limit.left * bmpWidth).toInt()
            top = (limit.top * bmpHeight).toInt()
            right = (limit.right * bmpWidth).toInt()
            bottom = (limit.bottom * bmpHeight).toInt()
        }
        if (targetRect.width() != bmp.width || targetRect.height() != bmp.height) {
            bmp = Bitmap.createBitmap(
                bmp,
                targetRect.left,
                targetRect.top, targetRect.width(), targetRect.height(), null, false
            )
        }

        ocrProvider.findText(
            bmp,
            type,
            mutableListOf(text),
            null
        ) { it ->
//            resultRect = it?.blocks?.firstOrNull()?.lines?.firstOrNull()?.findResult?.get(text)
            when (findDirection) {
                0 -> {
                    resultRect =
                        it?.blocks?.flatMap { it.lines }?.map { it.text to it.rect }
                            ?.sortedBy { it.second.left }?.minByOrNull { it.second.top }?.second
                }

                1 -> {
                    resultRect =
                        it?.blocks?.flatMap { it.lines }?.map { it.text to it.rect }
                            ?.sortedByDescending { it.second.left }
                            ?.minByOrNull { it.second.top }?.second
                }

                2 -> {
                    resultRect =
                        it?.blocks?.flatMap { it.lines }?.map { it.text to it.rect }
                            ?.sortedBy { it.second.left }?.maxByOrNull { it.second.top }?.second
                }

                3 -> {
                    resultRect =
                        it?.blocks?.flatMap { it.lines }?.map { it.text to it.rect }
                            ?.sortedByDescending { it.second.left }
                            ?.maxByOrNull { it.second.top }?.second
                }
            }

            latch.countDown()
        }
        bmp.recycle()
        latch.await()
        resultRect?.offset(targetRect.left, targetRect.top)
        return resultRect
    }

    fun tryRecogniseImage(
        imagePaths: List<String>?,
        limitRect: RectF,
        desiredAccuracy: Double
    ): List<Rect>? {
        ensureOpenCVProvider()
        var bmp: Bitmap? = ScriptScreenShotService.instance?.getScreenShot() ?: return null
        bmp ?: return null
        val bmpWidth = bmp.width
        val bmpHeight = bmp.height
        val targetRect = Rect().apply {
            left = (limitRect.left * bmpWidth).toInt()
            top = (limitRect.top * bmpHeight).toInt()
            right = (limitRect.right * bmpWidth).toInt()
            bottom = (limitRect.bottom * bmpHeight).toInt()
        }
        if (targetRect.width() != bmp.width || targetRect.height() != bmp.height) {
            bmp = Bitmap.createBitmap(
                bmp,
                targetRect.left,
                targetRect.top, targetRect.width(), targetRect.height(), null, false
            )
        }

        val results = mutableListOf<Rect>()
        imagePaths?.forEach {
            val sample = BitmapUtils.getLocalBitmap(it)
            if (sample != null) {
                val rect = openCVProvider?.findImage(sample, bmp, desiredAccuracy)
                if (rect != null) {
                    results.add(
                        Rect(
                            rect.left + targetRect.left,
                            rect.top + targetRect.top,
                            rect.right + targetRect.left,
                            rect.bottom + targetRect.top
                        )
                    )
                }
            }
        }
        return results
    }

    private fun ensureOpenCVProvider() {
        openCVProvider =
            ComponentManager.getInstance()
                .getProvider(IOpenCVProvider::class.java) as IOpenCVProvider?
    }

    fun tryFindColor(color: Int, limitRect: RectF, threshold: Int): Point? {
        ensureOpenCVProvider()
        var bmp: Bitmap? =
            ScriptScreenShotService.instance?.getScreenShot() ?: return null
        bmp ?: return null
        val bmpWidth = bmp.width
        val bmpHeight = bmp.height
        val targetRect = Rect().apply {
            left = (limitRect.left * bmpWidth).toInt()
            top = (limitRect.top * bmpHeight).toInt()
            right = (limitRect.right * bmpWidth).toInt()
            bottom = (limitRect.bottom * bmpHeight).toInt()
        }
        if (targetRect.width() != bmp.width || targetRect.height() != bmp.height) {
            bmp = Bitmap.createBitmap(
                bmp,
                targetRect.left,
                targetRect.top, targetRect.width(), targetRect.height(), null, false
            )
        }
        val point = openCVProvider?.findColor(bmp, color, threshold)
        point?.run {
            val result = (point.x > 0.0 || point.y > 0.0)
            if (result) {
                return Point(
                    point.x + targetRect.left,
                    point.y + targetRect.top
                )
            }
        }
        return null
    }

    fun tryFindColors(color: Int, limitRect: RectF, threshold: Int): Array<Point>? {
        ensureOpenCVProvider()
        var bmp: Bitmap? =
            ScriptScreenShotService.instance?.getScreenShot() ?: return null
        bmp ?: return null
        val bmpWidth = bmp.width
        val bmpHeight = bmp.height
        val targetRect = Rect().apply {
            left = (limitRect.left * bmpWidth).toInt()
            top = (limitRect.top * bmpHeight).toInt()
            right = (limitRect.right * bmpWidth).toInt()
            bottom = (limitRect.bottom * bmpHeight).toInt()
        }
        if (targetRect.width() != bmp.width || targetRect.height() != bmp.height) {
            bmp = Bitmap.createBitmap(
                bmp,
                targetRect.left,
                targetRect.top, targetRect.width(), targetRect.height(), null, false
            )
        }
        val points = openCVProvider?.findColors(bmp, color, threshold)
        if (points?.isEmpty() == true || (points?.size == 1 && (points[0].x == 0 && points[0].y == 0))) {
            return null
        }
        return points?.map {
            Point(
                it.x + targetRect.left,
                it.y + targetRect.top
            )
        }?.toTypedArray()
    }

    fun tryFindColorRect(color: Int, limitRect: RectF, threshold: Int): Array<Rect>? {
        ensureOpenCVProvider()
        var bmp: Bitmap? =
            ScriptScreenShotService.instance?.getScreenShot() ?: return null
        bmp ?: return null
        val bmpWidth = bmp.width
        val bmpHeight = bmp.height
        val targetRect = Rect().apply {
            left = (limitRect.left * bmpWidth).toInt()
            top = (limitRect.top * bmpHeight).toInt()
            right = (limitRect.right * bmpWidth).toInt()
            bottom = (limitRect.bottom * bmpHeight).toInt()
        }
        if (targetRect.width() != bmp.width || targetRect.height() != bmp.height) {
            bmp = Bitmap.createBitmap(
                bmp,
                targetRect.left,
                targetRect.top, targetRect.width(), targetRect.height(), null, false
            )
        }
        val rects = openCVProvider?.findColorToRect(bmp, color, threshold)
        if (rects?.isNotEmpty() == false) {
            return null
        }
        return rects?.map { rect ->
            Rect(
                rect.left + targetRect.left,
                rect.top + targetRect.top,
                rect.right + targetRect.left,
                rect.bottom + targetRect.top
            )
        }?.toTypedArray()
    }

    /**
     * 自动授权功能
     */
    fun tryAutoGrantPermission() {
        if (ScriptSetting.script_setting_auto_authorize) {
            ScriptHelper.runInMain({
                var isGrant = false
                GlobalApp.getStringArray(com.hive.i8n.R.array.permission_confirm_btn_list)
                    ?.forEach {
                        val node = get().performFindLayoutInText(it, true)
                        if (node != null && node.text.length < 12) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            isGrant = true
                            return@forEach
                        }
                    }
            }, 500L + Random(1).nextInt(200))
        }
    }


    fun getAccessService(): ServiceAccessibility? {
        return serviceEntity
    }

    fun copyNotificationEvent(event: AccessibilityEvent): AccessibilityEvent {
        val parcel = Parcel.obtain()
        event.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val e: AccessibilityEvent = AccessibilityEvent.CREATOR.createFromParcel(parcel)
        parcel.recycle()
        return e
    }

    fun copyParcelableData(input: Parcelable?): Parcelable? {
        var parcel: Parcel? = null
        return try {
            parcel = Parcel.obtain()
            parcel.writeParcelable(input, 0)
            parcel.setDataPosition(0)
            parcel.readParcelable(input?.javaClass?.getClassLoader())
        } finally {
            parcel?.recycle()
        }
    }


    fun checkIfNeedUnLockScreen(): Boolean {
        return if (ScriptEventHelper.get()
                .isScreenLocked()
        ) {
            ScriptManager.isUnlockScriptExist()
        } else if (!ScriptEventHelper.get().isScreenOn()) {
            return true
        } else {
            false
        }
    }

    private val soundPool = SoundPool.Builder().build()

    private val soundId =
        soundPool.load(GlobalApp.getContext(), R.raw.sc_audio_reminder, 1)

    //使用系统播放音频sc_audio_reminder.mp3
    fun performPlayAudio() {
        soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
    }

    companion object {

        private val instance: ScriptEventHelper by lazy {
            ScriptEventHelper()
        }

        @JvmStatic
        fun get(): ScriptEventHelper {
            return instance
        }
    }
}