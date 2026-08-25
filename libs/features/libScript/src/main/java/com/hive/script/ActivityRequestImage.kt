// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.hive.permissions.PermissionsCallback
import com.hive.permissions.PermissionsChecker
import com.hive.utils.GlobalApp
import com.hive.utils.file.ContentUriFileHelper
import com.hive.utils.utils.IntentUtils

/**
 *
 * @author jiadou
 * @date 6/18/21
 */
class ActivityRequestImage : Activity() {

    private val MEDIA_REQUEST_IMAGE_CODE = 12

    private var permissionsChecker: PermissionsChecker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsChecker = PermissionsChecker(this)
        requestGallery()
    }


    /**
     * 从相册选择图片
     * Android 10+ 分区存储，使用 MediaStore API 不需要权限
     * Android 13+ 使用 READ_MEDIA_IMAGES 权限
     */
    private fun requestGallery() {
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                arrayOf() // Android 10+ 分区存储，MediaStore 不需要权限
            else ->
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissions.isEmpty()) {
            // Android 10-12: 直接打开图库
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, MEDIA_REQUEST_IMAGE_CODE)
        } else {
            permissionsChecker?.startCheck(permissions,
                object : PermissionsCallback {
                    override fun onGranted() {
                        val intent =
                            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(intent, MEDIA_REQUEST_IMAGE_CODE)
                    }

                    override fun onDenied(lackedPermissions: MutableList<String>?) {
                        failureFun?.invoke()
                    }
                }
            )
        }
    }

    @SuppressLint("Range")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionsChecker?.onActivityResult(requestCode, resultCode, data)
        var path: String? = null
        when (requestCode) {
            MEDIA_REQUEST_IMAGE_CODE -> {
                data?.data?.let { uri ->
                    path = copyUriToCache(uri)
                }
            }
        }
        if (path != null) {
            successFun?.invoke(path)
        } else {
            failureFun?.invoke()
        }

        finish()

    }

    private fun copyUriToCache(uri: Uri): String? {
        return ContentUriFileHelper.copyToCache(this, uri, ".jpg")?.absolutePath
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsChecker?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        failureFun = null
        successFun = null
    }

    companion object {

        var successFun: ((path: String?) -> Unit)? = null

        var failureFun: (() -> Unit)? = null

        var showRecord = false

        fun start(
            context: Context?,
            success: ((path: String?) -> Unit)?,
            failure: (() -> Unit)?
        ) {
            val cxt = context ?: GlobalApp.getApp()
            failureFun = failure
            successFun = success
            IntentUtils.safeStartActivity(cxt, Intent(cxt, ActivityRequestImage::class.java))
        }

    }
}
