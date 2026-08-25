// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.framework.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * @author：luck
 * @date：2019-11-20 19:07
 * @describe：权限检查
 */
object Permissions {
    /**
     * 检查是否有某个权限
     *
     * @param ctx
     * @param permission
     * @return
     */
    fun checkSelfPermission(ctx: Context, permission: String?): Boolean {
        return (ContextCompat.checkSelfPermission(ctx.applicationContext, permission!!)
                == PackageManager.PERMISSION_GRANTED)
    }

    fun getStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= 33) arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO
        )
        else arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    /**
     * Manifest.permission.READ_MEDIA_VIDEO,
     * Manifest.permission.READ_MEDIA_IMAGES,
     * Manifest.permission.READ_MEDIA_AUDIO
     * @param ctx
     * @return
     */
    fun checkStoragePermission(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_AUDIO)
                    && checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_VIDEO) &&
                    checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_IMAGES)
                else checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                    checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    /**
     * 动态申请多个权限
     * @param activity
     * @param code
     */
    fun requestStoragePermissions(activity: Activity?, code: Int) {
        ActivityCompat.requestPermissions(
            activity!!, if (Build.VERSION.SDK_INT >= 33) arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_AUDIO
            ) else arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), code
        )
    }

    /**
     * 动态申请多个权限
     *
     * @param activity
     * @param code
     */
    fun requestPermissions(activity: Activity?, permissions: Array<String?>, code: Int) {
        ActivityCompat.requestPermissions(activity!!, permissions, code)
    }

    /**
     * Launch the application's details settings.
     */
    fun launchAppDetailsSettings(context: Context) {
        val applicationContext = context.applicationContext
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:" + applicationContext.packageName)
        if (!isIntentAvailable(context, intent)) return
        applicationContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        return context.applicationContext
            .packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .size > 0
    }
}