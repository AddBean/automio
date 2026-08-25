// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.utils

import android.Manifest
import android.os.Build
import com.hive.permissions.PermissionsChecker
import com.hive.utils.utils.CollectionUtil

/**
 *
 * @author jiadou
 * @date 5/14/21
 */
object StoragePermissionsCheck {
    private var mPermissionsChecker = PermissionsChecker()

    /**
     * 获取对应 Android 版本的存储权限
     * Android 13+ 使用细分媒体权限
     * Android 10-12 分区存储，MediaStore 不需要权限
     * Android 9 及以下使用传统存储权限
     */
    private fun getStoragePermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                arrayOf() // Android 10-12 分区存储，MediaStore 不需要权限
            else ->
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
        }
    }

    fun checkPermission(): Boolean {
        val permissions = getStoragePermissions()
        if (permissions.isEmpty()) {
            return true // Android 10-12 默认有权限
        }
        val lacksPermissions = mPermissionsChecker.getLacksPermissions(*permissions)
        return CollectionUtil.empty(lacksPermissions)
    }
}