// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.driver.ServiceAccessibility
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.utils.GlobalApp
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.utils.thread.UIHandlerUtils
import com.hive.utils.utils.IntentUtils
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.hive.script.utils.ScriptHelper

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
@AutoCmdRegister(type = IDS.CmdOpenApp, name = "openApp")
class CmdOpenApp : ScriptCommand(), ScriptRegularInterface {
    private val context = GlobalApp.getContext()

    var targetAppPackage: String? = null

    var targetAppClass: String? = null

    var targetAppName: String? = null

    var action: String? = "reopen"  // reopen, open


    override fun onExecute(): CmdExecuteResult {
        // 首先检查应用是否已安装
        if (!isAppInstalled()) {
            return CmdExecuteResult.failure(context.getString(com.hive.i8n.R.string.sc_app_not_installed, targetAppPackage))
        }

        // 检查无障碍服务是否可用
        if (ScriptEventHelper.get().serviceEntity == null) {
            return CmdExecuteResult.failure(context.getString(com.hive.i8n.R.string.sc_accessibility_service_not_started))
        }

        var isSuccess = false
        val latch = CountDownLatch(1)

        ScriptHelper.runInMain {
            var cxt: Context? = ScriptEventHelper.get().serviceEntity
            if (cxt == null)
                cxt = GlobalApp.getContext()

            if (GlobalApp.getAvailableActivity() == null) {
                val intentToResolve =
                    Intent(GlobalApp.getApp(), GlobalApp.getMainActivityClass()).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                GlobalApp.getApp().startActivity(intentToResolve)
                ScriptHelper.runInMain({
                    realJump(cxt!!)
                    // 使用优雅的等待机制检查应用启动状态
                    checkAppLaunchWithTimeout { success ->
                        isSuccess = success
                        latch.countDown()
                    }
                }, 500)
            } else {
                realJump(cxt!!)
                // 使用优雅的等待机制检查应用启动状态
                checkAppLaunchWithTimeout { success ->
                    isSuccess = success
                    latch.countDown()
                }
            }
        }

        // 等待检查完成，最多等待5秒
        latch.await(5, TimeUnit.SECONDS)

        return if (isSuccess) {
            CmdExecuteResult.success()
        } else {
            CmdExecuteResult.failure(context.getString(com.hive.i8n.R.string.sc_app_launch_failed, targetAppPackage))
        }
    }

    private fun realJump(cxt: Context): Boolean {
        val intent = Intent()

        return if (TextUtils.isEmpty(targetAppClass) || targetAppClass == "-") {
            val launchIntent =
                GlobalApp.getApp().packageManager.getLaunchIntentForPackage(targetAppPackage!!)
            if (launchIntent == null) {
                false
            } else {
                IntentUtils.safeStartActivity(cxt, launchIntent)
            }
        } else {
            //如果是reopen，先杀掉进程
            if (action == "reopen") {
                val am = GlobalApp.getApp()
                    .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
                am!!.killBackgroundProcesses(targetAppPackage)
            }
            val cmp = ComponentName(targetAppPackage!!, targetAppClass!!)
            intent.action = Intent.ACTION_MAIN
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            intent.component = cmp
            IntentUtils.safeStartActivity(cxt, intent)
        }
    }

    /**
     * 检查应用是否已安装
     */
    private fun isAppInstalled(): Boolean {
        return try {
            targetAppPackage?.let { packageName ->
                GlobalApp.getApp().packageManager.getPackageInfo(packageName, 0)
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 带超时的应用启动检查
     */
    private fun checkAppLaunchWithTimeout(callback: (Boolean) -> Unit) {
        val startTime = System.currentTimeMillis()
        val timeout = 3000L // 3秒超时
        val checkInterval = 200L // 每200ms检查一次

        val checkRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()

                // 检查是否超时
                if (currentTime - startTime > timeout) {
                    callback(false)
                    return
                }

                // 检查应用是否启动成功
                if (checkAppLaunchSuccessByAccessibility()) {
                    callback(true)
                    return
                }

                // 继续检查
                ScriptHelper.runInMain(this, checkInterval)
            }
        }

        // 开始检查
        ScriptHelper.runInMain(checkRunnable, checkInterval)
    }

    /**
     * 通过无障碍服务检查应用启动是否成功
     */
    private fun checkAppLaunchSuccessByAccessibility(): Boolean {
        val serviceEntity = ScriptEventHelper.get().serviceEntity ?: return false

        // 方法1: 检查当前窗口的包名
        val currentPackageName = getCurrentWindowPackageName(serviceEntity)
        if (currentPackageName == targetAppPackage) {
            return true
        }

        // 方法2: 检查根节点是否属于目标应用
        val rootNode = serviceEntity.rootInActiveWindow
        if (rootNode != null) {
            val packageName = rootNode.packageName?.toString()
            if (packageName == targetAppPackage) {
                return true
            }
        }

        // 方法3: 检查最近的无障碍事件
        val lastEvent = ScriptEventHelper.get().accessibilityViewEvent
        if (lastEvent != null && lastEvent.packageName == targetAppPackage) {
            // 检查是否是窗口状态变化事件
            if (lastEvent.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                lastEvent.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                return true
            }
        }

        return false
    }

    /**
     * 获取当前窗口的包名
     */
    private fun getCurrentWindowPackageName(serviceEntity: ServiceAccessibility): String? {
        return try {
            // 通过根节点获取包名
            val rootNode = serviceEntity.rootInActiveWindow
            rootNode?.packageName?.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun getCommandDuration() = ScriptConst.Cmd_Default_OpenApp

    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_openapp)

    override fun getCommandDescribe() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_openapp, targetAppName)

    override fun getCommandIcon() = R.drawable.sc_icon_app

    override fun getCommand() =
        "${cmdPrefix()} package=$targetAppPackage class=${targetAppClass ?: "-"} name=\"${targetAppName?.encode()}\" action=${action ?: "reopen"}"

    override fun parseCmd(cmd: String) {
        if (matchCmd(cmd)) {
            val kv = ScriptLineTokenizer.parseKeyValueParams(cmd)
            targetAppPackage = kv["package"]
            targetAppClass = kv["class"] ?: "-"
            targetAppName = kv["name"]?.decode()
            kv["action"]?.let { action = it }
        }
    }

    override fun getPermissionRequest() = null

    companion object {
        fun createCommand(
            targetAppPackage: String?,
            targetAppClass: String?,
            targetAppName: String?,
            action: String?
        ) = CmdOpenApp().apply {
            this.targetAppPackage = targetAppPackage
            this.targetAppClass = targetAppClass ?: "-"
            this.targetAppName = targetAppName ?: "-"
            this.action = action ?: "reopen"
        }
    }
}