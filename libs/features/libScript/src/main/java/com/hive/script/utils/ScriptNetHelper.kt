// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException

object ScriptNetHelper {

    private var ipAddress = ""

    private val client = OkHttpClient()

    /**
     * curl请求
     * @param url 请求地址
     * @param method 请求方法
     * @param headers 请求头
     * @param form 表单
     * @param body 请求体
     * @return 返回请求结果
     */
    fun curl(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = mapOf(),
        form: Map<String, String> = mapOf(),
        body: String = ""
    ): String {
        return try {
            connect(url, method, headers, form, body) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 使用OkHttp库实现一个简单的curl功能，包含get和post请求，并且支持自定义header，body，form表单等
     */
    fun connect(
        url: String,
        method: String,
        headers: Map<String, String>,
        form: Map<String, String> = mapOf(),
        body: String
    ): String? {
        val requestBuilder = Request.Builder().url(url)

        // Add headers
        for ((key, value) in headers) {
            requestBuilder.addHeader(key, value)
        }

        // Create request body
        val requestBody: RequestBody = when {
            form.isNotEmpty() -> {
                val formBodyBuilder = FormBody.Builder()
                for ((key, value) in form) {
                    formBodyBuilder.add(key, value)
                }
                formBodyBuilder.build()
            }

            body.isNotEmpty() -> {
                RequestBody.create("application/json; charset=utf-8".toMediaType(), body)
            }

            else -> {
                RequestBody.create(null, ByteArray(0))
            }
        }

        // Set method and body
        when (method.uppercase()) {
            "POST" -> requestBuilder.post(requestBody)
            "PUT" -> requestBuilder.put(requestBody)
            "DELETE" -> requestBuilder.delete(requestBody)
            else -> requestBuilder.get()
        }

        val request = requestBuilder.build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                response.body?.string()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun getLocationSync(): String {
        if (ipAddress.isNotEmpty()) return ipAddress
        return ipAddress
    }
}
