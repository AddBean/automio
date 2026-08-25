// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar.sprite

import android.text.TextUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.hive.utils.GlobalApp
import com.hive.utils.encrypt.Md5Utils
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 *
 * @author jiadou
 * @date 2022/9/22
 */
class SpriteImageDownloader {


    suspend fun downloadImage(url: String): String? {
        return suspendCoroutine {
            download(url, object : DownloadCallback {
                override fun onFinished(what: Int, result: Any?) {
                    if (what == 1) {
                        val path = result as String
                        it.resume(path)
                    } else {
                        it.resume(null)
                    }
                }
            })
        }
    }

    private fun download(url: String, callback: DownloadCallback) {
        callback.onStart()
        Glide.with(GlobalApp.getContext())
            .downloadOnly()
            .load(url)
            .listener(object : RequestListener<File> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<File>?,
                    isFirstResource: Boolean
                ): Boolean {
                    callback.onFinished(0, null)
                    return false
                }

                override fun onResourceReady(
                    resource: File?,
                    model: Any?,
                    target: Target<File>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    callback.onFinished(1, resource)
                    return true
                }

            })
            .preload();
    }


    interface DownloadCallback {
        fun onStart() {}
        fun onFinished(what: Int, result: Any?)
    }

    fun isFileExists(dir: String?, fileName: String?): Boolean {
        return File(dir, fileName).exists()
    }

    fun getFileName(url: String): String {
        if (!TextUtils.isEmpty(url) && url.contains("/")) {
            val s = url.lastIndexOf("/")
            val e = url.indexOf("?")
            return if (e > 0) {
                url.substring(s + 1, e)
            } else {
                url.substring(s + 1)
            }
        }
        return if (!TextUtils.isEmpty(url)) {
            Md5Utils.string2md5(url)
        } else url
    }

    fun getFilePath(url: String): String? {
        return File(GlobalApp.getContext().cacheDir, getFileName(url)).absolutePath
    }
}