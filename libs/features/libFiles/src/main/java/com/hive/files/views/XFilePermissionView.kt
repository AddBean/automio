// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.views

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import com.hive.base.BaseLayout
import com.hive.libfiles.R
import com.hive.permissions.PermissionsCallback
import com.hive.permissions.PermissionsChecker
import com.hive.utils.utils.CollectionUtil

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/26/21
 */

class XFilePermissionView(context: Context, attrs: AttributeSet) : BaseLayout(context, attrs), View.OnClickListener, PermissionsCallback {
    private var lacksPermissions: MutableList<String>? = null
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

    private var btn_permission_jump: View? = null
    private var layout_permission: View? = null
    private var layout_empty: View? = null

    override fun initView(view: View?) {}

    override fun onFinishInflate() {
        super.onFinishInflate()
        btn_permission_jump = findViewById(R.id.btn_permission_jump)
        layout_permission = findViewById(R.id.layout_permission)
        layout_empty = findViewById(R.id.layout_empty)
        layout_empty = findViewById(R.id.layout_empty)
        checkPermission()
        btn_permission_jump?.setOnClickListener(this)
    }

    fun checkPermission() {
        val permissions = getStoragePermissions()
        if (permissions.isEmpty()) {
            // Android 10-12 默认有权限
            layout_permission?.visibility = View.GONE
            layout_empty?.visibility = View.VISIBLE
            return
        }

        lacksPermissions = mPermissionsChecker.getLacksPermissions(*permissions)
        if (!CollectionUtil.empty(lacksPermissions)) {
            layout_permission?.visibility = View.VISIBLE
            layout_empty?.visibility = View.GONE
        } else {
            layout_permission?.visibility = View.GONE
            layout_empty?.visibility = View.VISIBLE
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_permission_jump -> {
                val permissions = getStoragePermissions()
                if (permissions.isNotEmpty()) {
                    mPermissionsChecker.startCheck(permissions, this)
                }
            }
        }
    }


    override fun onGranted() {
        checkPermission()
    }

    override fun onDenied(lackedPermissions: MutableList<String>?) {
        checkPermission()
    }

    override fun getLayoutId() = R.layout.x_file_permission_view
}