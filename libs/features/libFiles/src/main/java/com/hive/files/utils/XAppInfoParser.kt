// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.format.Formatter
import java.io.File

object XAppInfoParser {

    private const val tag = "AppInfoParser"

    fun getAppInfoList(context: Context): List<AppInfo> {
        return getAppInfoList(context, 0)
    }

    fun getAppInfoList(context: Context, flag: Int): List<AppInfo> {
        //首先获取到包的管理者
        val packageManager = context.packageManager

        //获取到所有的安装包
        val app1 = packageManager.getInstalledPackages(flag)
        val appInfos = ArrayList<AppInfo>()
        if (app1.size > 1) {
            for (pkg in app1) {
                val appInfo = AppInfo()
                //程序包名
                val packageName = pkg.packageName
                appInfo.packageName = packageName
                //获取到图标
                val icon = pkg.applicationInfo?.loadIcon(packageManager)
                appInfo.icon = icon
                //获取到应用的名字
                val appName = pkg.applicationInfo?.loadLabel(packageManager).toString()
                appInfo.appName = appName
                //获取到安装包的路径
                val sourceDir = pkg.applicationInfo?.sourceDir
                appInfo.apkPath = sourceDir
                val file = File(sourceDir)
                //获取到安装apk的大小
                val apkSize = file.length()
                //格式化apk的大小
                appInfo.apkSize = Formatter.formatFileSize(context, apkSize)
                val flags = pkg.applicationInfo?.flags?:0
                //判断当前是否是系统app
                if (flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                    //那么就是系统app
                    appInfo.isUserApp = false
                } else {
                    //那么就是用户app
                    appInfo.isUserApp = true
                }
                //那么就是手机内存 那么当前安装的就是sd卡
                appInfo.isSD = flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE != 0
                appInfos.add(appInfo)
            }
            return appInfos
        }
        val app2 = getApps(context)
        if (app2.size > 1) {
            appInfos.clear()
            for (app in app2) {
                val appInfo = AppInfo()
                appInfo.appName = app.loadLabel(packageManager).toString()
                appInfo.packageName = app.activityInfo.packageName
                appInfo.icon = app.loadIcon(packageManager)
                appInfo.isSD = false
                appInfo.isUserApp = true
                appInfo.apkPath = app.activityInfo.applicationInfo.sourceDir
                appInfos.add(appInfo)
            }
            return appInfos
        }
        val app3 = packageManager.getInstalledApplications(flag)
        if (app3.size > 1) {
            appInfos.clear()
            for (appInfo in app3) {
                val app = AppInfo()
                app.appName = appInfo.loadLabel(packageManager).toString()
                app.packageName = appInfo.packageName
                app.icon = appInfo.loadIcon(packageManager)
                app.isUserApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
                app.isSD = appInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE != 0
                app.apkPath = appInfo.sourceDir
                appInfos.add(app)
            }
            return appInfos
        }
        return appInfos
    }

    private fun getApps(context: Context): MutableList<ResolveInfo> {
        //首先获取到包的管理者
        val packageManager = context.packageManager
        var queryIntentActivities = mutableListOf<ResolveInfo>()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        queryIntentActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        } else {
            packageManager.queryIntentActivities(intent, 0)
        }
        return queryIntentActivities
    }

    class AppInfo {
        //应用图标
        var icon: Drawable? = null

        //应用的名字
        var appName: String? = null

        //应用程序的大小
        var apkSize: String? = null

        //应用路径
        var apkPath: String? = null

        //表示用户程序
        var isUserApp = false

        //存储的位置.
        var isSD = false

        var packageName: String? = null


        override fun toString(): String {
            return "AppInfo{appName='$appName', apkSize='$apkSize', isUserApp=$isUserApp, isSD=$isSD}"
        }
    }
}
