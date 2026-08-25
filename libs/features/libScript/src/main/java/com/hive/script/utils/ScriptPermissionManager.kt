// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.hive.permissions.PermissionsChecker
import com.hive.script.ActivityRequestPermission
import com.hive.script.ActivityRequestPermissionCapture
import com.hive.script.ScriptScreenShotService
import com.hive.script.driver.ScriptNotificationService
import com.hive.script.utils.ScriptHelper.PERMISSION_BIND_ACCESSIBILITY_SERVICE
import com.hive.script.utils.ScriptHelper.PERMISSION_CAPTURE
import com.hive.script.utils.ScriptHelper.PERMISSION_NOTIFICATION_LISTENER
import com.hive.script.views.manager.ScriptManager
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.views.widgets.CommonToast

object ScriptPermissionManager {

    fun isBatterySaveOpen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (GlobalApp.getApp().getSystemService(Context.POWER_SERVICE) as PowerManager)
                .isIgnoringBatteryOptimizations(GlobalApp.getPackageName())
        } else {
            false
        }
    }

    fun requestRemoveBatterySave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            GlobalApp.getApp()
                .startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${GlobalApp.getPackageName()}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
        }
    }

    /**
     * 是否允许从桌面正常启动入口 Activity（未被「停用」等系统项关闭）。
     *
     * 说明：小米/华为等「自启动 / 后台运行」开关**没有**统一系统 API，
     * 此处无法反映厂商后台白名单；仅能通过启动 Activity 的启用状态做粗判。
     * 从应用信息页返回后 [ActivityPermissionCenter] 会照常刷新，若仍不符预期多为 OEM 限制。
     */
    fun isAutoStartOpen(context: Context): Boolean {
        val pm = context.packageManager
        val launch =
            pm.getLaunchIntentForPackage(context.packageName)?.component
                ?: return false
        return when (pm.getComponentEnabledSetting(launch)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> true
            else -> false
        }
    }


    fun isNotificationOpen(context: Context): Boolean {
        val cn = ComponentName(context, ScriptNotificationService::class.java)
        val flat =
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }


    fun toOpenAutoPermission(context: Context) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        context.startActivity(intent)
    }

    /** 截屏/录屏（MediaProjection）是否已就绪，与 [ScriptScreenShotService.checkPermission] 一致 */
    fun isScreenRecordingPermissionGranted(@Suppress("UNUSED_PARAMETER") context: Context): Boolean {
        return ScriptScreenShotService.checkPermission()
    }

    /**
     * 请求屏幕录制权限
     */
    fun requestRecordingPermission(
        context: Activity?, success: (() -> Unit)?,
        failure: (() -> Unit)?
    ) {
        if (context == null) {
            failure?.invoke()
            return
        }
        ActivityRequestPermissionCapture.checkOrRequestPermission(context, true, success, failure)
//        val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//        context.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), requestCode)
    }

    /**
     * 请求权限
     */
    fun requestCommonPermission(
        context: Activity?, permission: String, success: (() -> Unit)?,
        failure: (() -> Unit)?
    ) {
        if (context == null) {
            failure?.invoke()
            return
        }
        ActivityRequestPermission.checkOrRequestPermission(context, permission, success, failure)
//        val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//        context.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), requestCode)
    }

    fun getGrandPermissionInfo(): String {
        val list = arrayOfNulls<String>(ScriptHelper.mPermissionMap.keys.size)
        ScriptHelper.mPermissionMap.keys.forEachIndexed { index, string ->
            list[index] = string
        }
        val permissionChecker = PermissionsChecker()
        val permissions: List<String> = permissionChecker.getLacksPermissions(*list)
        val info = StringBuilder()
        //已获取的权限
        info.append("Granted permissions: ")
        info.append("[" + list.filter { !permissions.contains(it) }.joinToString(",") + "]")
        info.append(",")
        //未获取的权限
        info.append("Missing permissions: ")
        info.append("[" + permissions.joinToString(",") + "]")
        return info.toString()
    }

    /**
     * 检查是否有指定的权限
     * @param permissions 权限列表（null 视为空列表）
     * @return 返回缺少的权限信息列表，包含权限名称和描述。空列表表示所有权限都已授予
     */
    fun checkMissedPermissions(permissions: List<String>?): List<Pair<String, String>> {
        if (permissions.isNullOrEmpty()) return emptyList()
        val context = GlobalApp.getApp()
        val permissionMap = ScriptHelper.mPermissionMap

        return permissions.mapNotNull { permission ->
            val description = permissionMap[permission] ?: run {
                // 记录未定义的权限描述
                DLog.w("PermissionCheck", "Undefined permission: $permission")
                return@mapNotNull null
            }

            val isGranted = when (permission) {
                PERMISSION_CAPTURE -> {
                    ScriptScreenShotService.checkPermission();
                }

                PERMISSION_BIND_ACCESSIBILITY_SERVICE -> {
                    ScriptManager.checkServerEnable()
                }

                PERMISSION_NOTIFICATION_LISTENER -> {
                    isNotificationOpen(GlobalApp.getContext())
                }

                else -> {
                    // 对于低版本系统，默认返回已授予
                    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
                }
            }

            if (!isGranted) {
                permission to description
            } else {
                null
            }
        }
    }

    fun toOpenNotificationPermission(context: Context?) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context?.startActivity(intent)
    }

    /**
     * 打开无障碍设置页（用于权限聚合弹窗「去打开」）
     */
    fun toOpenAccessibilitySetting(context: Context?) {
        if (context == null) return
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 根据权限类型调起申请或跳转设置，不依赖回调（刷新由权限聚合弹窗定时器负责）
     */
    fun openOrRequestPermission(permission: String, context: Context?) {
        if (permission !in ScriptHelper.mPermissionMap.keys || context == null) return
        when (permission) {
            PERMISSION_CAPTURE -> {
                ActivityRequestPermissionCapture.checkOrRequestPermission(
                    context,
                    false,
                    {},
                    { CommonToast.show(com.hive.i8n.R.string.sc_permission_snap_screen_failure) }
                )
            }
            PERMISSION_NOTIFICATION_LISTENER -> toOpenNotificationPermission(context)
            PERMISSION_BIND_ACCESSIBILITY_SERVICE -> toOpenAccessibilitySetting(context)
            else -> {
                ActivityRequestPermission.checkOrRequestPermission(
                    context,
                    permission,
                    null,
                    null
                )
            }
        }
    }

}