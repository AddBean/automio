// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import kotlin.system.exitProcess

object AppHelper {

    /**
     * 彻底退出应用，包括辅助功能相关进程
     */
    fun killAndExitApp() {
        try {
            DLog.e("AppHelper", "开始彻底退出应用...")
            
            // 1. 停止所有Activity
            finishAllActivities()
            
            // 2. 停止所有服务
            stopAllServices()
            
            // 3. 杀死所有相关进程（包括辅助功能进程）
            killAllAppProcesses()
            
            // 4. 清理系统资源
            clearSystemResources()
            
            // 5. 最后杀死当前进程
            DLog.e("AppHelper", "杀死当前进程: ${Process.myPid()}")
            Process.killProcess(Process.myPid())
            exitProcess(0)
            
        } catch (e: Exception) {
            DLog.e("AppHelper", "退出应用时出错: ${e.message}")
            // 即使出错也要强制退出
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    }

    /**
     * 停止所有Activity
     */
    private fun finishAllActivities() {
        try {
            val activityManager = GlobalApp.getApp().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val appTasks = activityManager.appTasks
            
            appTasks?.forEach { taskInfo ->
                try {
                    taskInfo.finishAndRemoveTask()
                } catch (e: Exception) {
                    DLog.e("AppHelper", "停止Activity时出错: ${e.message}")
                }
            }
        } catch (e: Exception) {
            DLog.e("AppHelper", "停止Activity时出错: ${e.message}")
        }
    }

    /**
     * 停止所有服务
     */
    private fun stopAllServices() {
        try {
            val context = GlobalApp.getApp()
            val packageManager = context.packageManager
            val packageName = context.packageName
            
            // 获取所有服务
            val serviceInfoList = packageManager.getPackageInfo(packageName, 0).services
            serviceInfoList?.forEach { serviceInfo ->
                try {
                    val intent = Intent()
                    intent.setClassName(packageName, serviceInfo.name)
                    context.stopService(intent)
                    DLog.e("AppHelper", "停止服务: ${serviceInfo.name}")
                } catch (e: Exception) {
                    DLog.e("AppHelper", "停止服务时出错: ${e.message}")
                }
            }
        } catch (e: Exception) {
            DLog.e("AppHelper", "停止服务时出错: ${e.message}")
        }
    }

    /**
     * 杀死所有相关进程，包括辅助功能进程
     */
    private fun killAllAppProcesses() {
        try {
            val context = GlobalApp.getApp()
            val packageName = context.packageName
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            
            // 获取所有运行中的进程
            val runningProcesses = activityManager.runningAppProcesses
            runningProcesses?.forEach { processInfo ->
                val processName = processInfo.processName
                
                // 检查是否是我们应用的进程（包括主进程、辅助功能进程等）
                if (processName.startsWith(packageName) || 
                    processName.contains("accessibility") ||
                    processName.contains("service") ||
                    processName.contains(":") && processName.startsWith(packageName)) {
                    
                    try {
                        DLog.e("AppHelper", "正在杀死进程: $processName (PID: ${processInfo.pid})")
                        Process.killProcess(processInfo.pid)
                    } catch (e: Exception) {
                        DLog.e("AppHelper", "杀死进程 $processName 时出错: ${e.message}")
                    }
                }
            }
            
            // 额外尝试杀死可能的辅助功能进程
            killAccessibilityProcesses()
            
        } catch (e: Exception) {
            DLog.e("AppHelper", "杀死进程时出错: ${e.message}")
        }
    }

    /**
     * 专门处理辅助功能相关进程
     */
    private fun killAccessibilityProcesses() {
        try {
            val context = GlobalApp.getApp()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val packageName = context.packageName
            
            // 查找可能的辅助功能进程
            val runningProcesses = activityManager.runningAppProcesses
            runningProcesses?.forEach { processInfo ->
                val processName = processInfo.processName
                
                // 检查辅助功能相关的进程名称模式
                if (processName.contains("accessibility") ||
                    processName.contains(":accessibility") ||
                    processName.contains(":service") ||
                    processName.contains(":background") ||
                    (processName.startsWith(packageName) && processName.contains(":"))) {
                    
                    try {
                        DLog.e("AppHelper", "正在杀死辅助功能进程: $processName (PID: ${processInfo.pid})")
                        Process.killProcess(processInfo.pid)
                    } catch (e: Exception) {
                        DLog.e("AppHelper", "杀死辅助功能进程 $processName 时出错: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            DLog.e("AppHelper", "杀死辅助功能进程时出错: ${e.message}")
        }
    }

    /**
     * 清理系统资源
     */
    private fun clearSystemResources() {
        try {
            val context = GlobalApp.getApp()
            
            // 清理内存
            System.gc()
            
            // 清理缓存
            try {
                val cacheDir = context.cacheDir
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                }
            } catch (e: Exception) {
                DLog.e("AppHelper", "清理缓存时出错: ${e.message}")
            }
            
            // 如果是Android 6.0以上，尝试清理内存
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    activityManager.clearApplicationUserData()
                } catch (e: Exception) {
                    DLog.e("AppHelper", "清理应用数据时出错: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            DLog.e("AppHelper", "清理系统资源时出错: ${e.message}")
        }
    }

    /**
     * 强制退出（备用方法）
     */
    fun forceExit() {
        try {
            DLog.e("AppHelper", "执行强制退出...")
            Process.killProcess(Process.myPid())
            exitProcess(0)
        } catch (e: Exception) {
            DLog.e("AppHelper", "强制退出时出错: ${e.message}")
            exitProcess(0)
        }
    }
}