// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar.sprite

import android.content.Context
import android.text.TextUtils
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.utils.encrypt.Md5Utils
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.*
import java.util.concurrent.TimeUnit

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2022/9/21
 */
class SpriteIndexLoader {

    private val TAG = "SpriteIndexLoader"

    fun loadIndexFile(indexUrl: String): List<Int>? {
        val stream = getIndexFileStream(GlobalApp.getContext(), indexUrl)
        val list = parseIndexFile(stream)
        list?.run {
            DLog.e(TAG, printIndexList(list))
        }
        return list
    }


    @Throws(IOException::class)
    private fun getIndexFileStream(context: Context, pvdata: String): InputStream? {
        val cacheDir = getCacheDirectory(context)
        if (TextUtils.isEmpty(cacheDir)) {
            return getIndexFileRemoteStream(pvdata)
        }
        var result: InputStream? = null
        val key = Md5Utils.string2md5(pvdata)
        val cache = File(cacheDir)
        if (!cache.exists()) {
            cache.mkdir()
        }
        if (cache == null || cache.listFiles() == null) return null
        for (file in cache.listFiles()) {
            if (file.name == key) {
                result = FileInputStream(file)
                break
            }
        }
        if (result == null) {
            val connInputStream = getIndexFileRemoteStream(pvdata)
            if (connInputStream != null) {
                val name = cache.absolutePath + File.separator + key
                val fileOutputStream = FileOutputStream(name)
                copy(connInputStream, fileOutputStream,
                    ByteArray(1024 * 4))
                connInputStream.close()
                fileOutputStream.close()
                result = FileInputStream(name)
            }
        }
        return result
    }

    private fun getCacheDirectory(context: Context): String {
        var cache = context.applicationContext.externalCacheDir
        if (cache == null) {
            cache = context.applicationContext.cacheDir
        }
        val cacheDir = cache!!.absolutePath + "/index_bin"
        return cacheDir
    }

    private fun getIndexFileRemoteStream(url: String): InputStream? {


        val client = OkHttpClient.Builder()
            .readTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder().url(url)
            .get().build()
        val call = client.newCall(request)
        try {
            val response = call.execute()
            val body = response.body() ?: return null
            return body.byteStream()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    @Throws(IOException::class)
    private fun parseIndexFile(inputStream: InputStream?): List<Int>? {
        if (inputStream == null) {
            return null
        }
        val temp = ByteArray(2)
        val list = ArrayList<Int>()
        while (true) {
            val readCount = inputStream.read(temp)
            if (readCount == -1) {
                break
            }
            if (readCount == 0) {
                continue
            }
            if (readCount == 1) {
                DLog.e(TAG, "read count incorrect: $readCount")
                temp[1] = inputStream.read().toByte()
                if (temp[1].toInt() == -1) {
                    break
                }
            }
            list.add(bytesToInt(temp))
        }
        if (list.isNotEmpty()) {
            list.removeAt(0)
        }
        DLog.d(TAG, "index: " + printIndexList(list))
        return list
    }

    private fun printIndexList(list: List<Int>): String {
        val builder = StringBuilder()
        for (i in list) {
            builder.append(", ").append(i)
        }
        return builder.toString()
    }

    private fun bytesToInt(src: ByteArray): Int {
        return (src[1].toInt() and 0xFF) or ((src[0].toInt() shl 8) and 0xFF00)
    }

    @Throws(IOException::class)
    fun copy(input: InputStream, output: OutputStream, buffer: ByteArray?): Long {
        var count: Long = 0
        var n = 0
        while (-1 != input.read(buffer).also { n = it }) {
            output.write(buffer, 0, n)
            count += n.toLong()
        }
        return count
    }
}