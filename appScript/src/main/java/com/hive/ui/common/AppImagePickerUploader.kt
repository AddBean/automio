// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.common

import android.app.Activity
import com.hive.base.image.ImageUploadCompressor
import com.hive.exception.BaseException
import com.hive.net.BaseHttpService
import com.hive.net.resp.UploadResp
import com.hive.net.upload.FormFile
import com.hive.net.upload.IUploadListener
import com.hive.script.ActivityRequestImage
import com.hive.utils.debug.DLog
import com.hive.utils.system.CommonUtils
import com.hive.utils.utils.GsonHelper
import java.io.File

/**
 * 统一的图片选择+上传工具，供各页面复用。
 */
object AppImagePickerUploader {

    data class UploadResult(
        val localPath: String,
        val remoteUrl: String
    )

    fun pickAndUpload(
        activity: Activity,
        preset: ImageUploadCompressor.Preset? = null,
        compressSpec: ImageUploadCompressor.Spec? = preset?.let(ImageUploadCompressor::specFor),
        onStart: (() -> Unit)? = null,
        onSuccess: (UploadResult) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        ActivityRequestImage.start(
            activity,
            success = { path ->
                if (path.isNullOrBlank()) {
                    onFailure(BaseException("Image path is empty"))
                    return@start
                }
                uploadLocalFile(path, compressSpec, onStart, onSuccess, onFailure)
            },
            failure = {
                onFailure(BaseException("Image pick canceled"))
            }
        )
    }

    private fun uploadLocalFile(
        localPath: String,
        compressSpec: ImageUploadCompressor.Spec?,
        onStart: (() -> Unit)?,
        onSuccess: (UploadResult) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val sourceFile = File(localPath)
        if (!sourceFile.exists() || !sourceFile.isFile) {
            onFailure(BaseException("File not found"))
            return
        }

        val preparedFile: File
        val contentType: String
        try {
            if (compressSpec != null) {
                val compressed = ImageUploadCompressor.compress(sourceFile, compressSpec)
                preparedFile = compressed.file
                contentType = compressed.mimeType
                DLog.e(
                    "AppImagePickerUploader compress before=${sourceFile.length() / 1024}KB " +
                        "after=${preparedFile.length() / 1024}KB type=$contentType"
                )
            } else {
                preparedFile = sourceFile
                contentType = guessContentType(sourceFile)
            }
        } catch (t: Throwable) {
            onFailure(t)
            return
        }

        val suffix = if (preparedFile.name.contains(".")) {
            preparedFile.name.substringAfterLast(".", "").let { ".$it" }
        } else {
            ".jpg"
        }
        val uploadName = "${CommonUtils.getRandomName()}$suffix"
        val files = mutableListOf(FormFile(uploadName, preparedFile, "files", contentType))
        onStart?.invoke()
        BaseHttpService.postFiles("", mutableMapOf(), files, object : IUploadListener() {
            override fun onAllUploadSuccess(content: String?) {
                try {
                    deleteTempFileIfNeeded(sourceFile, preparedFile)
                    if (content.isNullOrBlank()) {
                        onFailure(BaseException("Upload response is empty"))
                        return
                    }
                    val resp = GsonHelper.getInstance().fromJson(content, UploadResp::class.java)
                    val url = resp.data?.firstOrNull()?.path
                    if (url.isNullOrBlank()) {
                        onFailure(BaseException("Upload response missing file url"))
                        return
                    }
                    onSuccess(UploadResult(localPath = localPath, remoteUrl = url))
                } catch (t: Throwable) {
                    onFailure(t)
                }
            }

            override fun onAllUploadFailed(msg: String?) {
                deleteTempFileIfNeeded(sourceFile, preparedFile)
                onFailure(BaseException(msg ?: "Upload failed"))
            }
        })
    }

    private fun deleteTempFileIfNeeded(sourceFile: File, preparedFile: File) {
        if (preparedFile.absolutePath != sourceFile.absolutePath) {
            preparedFile.delete()
        }
    }

    private fun guessContentType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            else -> "application/octet-stream"
        }
    }
}
