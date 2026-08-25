// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.update

import com.hive.utils.debug.DLog
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.security.cert.X509Certificate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * 多线程下载器
 * - 支持分块下载
 * - 支持超时配置
 * - 支持失败回调
 * - 线程安全
 */
class MultiThreadDownloader(
    private val url: String,
    private val savePath: String,
    private var threadCount: Int = 4,
    private val onSuccess: () -> Unit,
    private val onProgress: (totalBytesRead: Long, contentLength: Long) -> Unit,
    private val onFailure: ((Exception) -> Unit)? = null
) {

    companion object {
        private const val TAG = "MultiThreadDownloader"
    }

    private var totalBytesRead = AtomicLong(0)
    private var contentLength = 0L
    private var isDownloading = AtomicBoolean(false)
    private var currentDownloadedThread = AtomicInteger(0)
    private var failedThreads = AtomicInteger(0)
    private var executorService = Executors.newFixedThreadPool(threadCount)

    // 带超时的 OkHttpClient（支持 IP 地址 HTTPS）
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .apply {
            // 信任所有证书（用于 IP 地址 HTTPS 场景）
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, null)
            sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            hostnameVerifier { _, _ -> true }
        }
        .build()

    fun start() {
        currentDownloadedThread.set(0)
        failedThreads.set(0)
        isDownloading.set(true)

        // 先获取文件大小
        val headRequest = Request.Builder().url(url).head().build()
        val headResponse = try {
            client.newCall(headRequest).execute()
        } catch (e: Exception) {
            DLog.e(TAG, "HEAD 请求失败: ${e.message}")
            isDownloading.set(false)
            shutdown()
            onFailure?.invoke(e)
            return
        }

        // 从响应头直接读取 Content-Length（HEAD 请求的 body 为空）
        contentLength = headResponse.header("Content-Length")?.toLongOrNull() ?: -1
        headResponse.close()

        if (contentLength <= 0) {
            DLog.e(TAG, "无效的 Content-Length: $contentLength")
            isDownloading.set(false)
            shutdown()
            onFailure?.invoke(IOException("Invalid content length: $contentLength"))
            return
        }

        val file = targetFile()
        try {
            file.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    throw IOException("Create download directory failed: ${parent.absolutePath}")
                }
            }
            if (file.exists() && !file.delete()) {
                throw IOException("Delete old apk failed: ${file.absolutePath}")
            }
            if (!file.createNewFile() && !file.isFile) {
                throw IOException("Create download file failed: ${file.absolutePath}")
            }
            RandomAccessFile(file, "rw").use { it.setLength(contentLength) }
            DLog.d(TAG, "开始下载: url=$url, savePath=${file.absolutePath}, contentLength=$contentLength, 文件已创建=${file.exists()}, 文件大小=${file.length()}")
        } catch (e: Exception) {
            DLog.e(TAG, "准备下载文件失败: ${e.message}")
            isDownloading.set(false)
            shutdown()
            onFailure?.invoke(if (e is IOException) e else IOException(e))
            return
        }

        // 计算分块范围
        val blockSize = contentLength / threadCount
        val ranges = mutableListOf<Pair<Long, Long>>()
        for (i in 0 until threadCount) {
            val start = i * blockSize
            val end = if (i == threadCount - 1) (contentLength - 1) else ((i + 1) * blockSize - 1)
            ranges.add(Pair(start, end))
        }

        // 启动下载线程
        ranges.forEach { range ->
            executorService.execute(DownloadTask(range.first, range.second))
        }
    }

    fun stop() {
        isDownloading.set(false)
        shutdown()
    }

    private fun shutdown() {
        try {
            executorService.shutdown()
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: Exception) {
            executorService.shutdownNow()
        }
    }

    private fun targetFile(): File = File(savePath).absoluteFile

    private fun notifyComplete() {
        val file = targetFile()
        val fileSize = if (file.exists()) file.length() else -1L
        DLog.d(TAG, "下载完成: 文件大小=$fileSize, 成功线程=${currentDownloadedThread.get()}, 失败线程=${failedThreads.get()}")

        if (failedThreads.get() > 0) {
            // 有线程失败
            onFailure?.invoke(IOException("Download failed: ${failedThreads.get()} threads failed"))
        } else if (currentDownloadedThread.get() == threadCount && file.exists() && file.isFile && fileSize == contentLength) {
            // 所有线程完成
            onSuccess()
        } else {
            onFailure?.invoke(IOException("Download file invalid: exists=${file.exists()}, size=$fileSize, expected=$contentLength"))
        }
        shutdown()
    }

    inner class DownloadTask(
        private val start: Long,
        private val end: Long
    ) : Runnable {

        override fun run() {
            if (!isDownloading.get()) return

            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Range", "bytes=$start-$end")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    DLog.e(TAG, "下载块失败: code=${response.code}, range=$start-$end")
                    failedThreads.incrementAndGet()
                    response.close()
                    checkComplete()
                    return
                }

                response.body?.let { responseBody ->
                    val file = targetFile()
                    DLog.d(TAG, "开始写入块: range=$start-$end, path=${file.absolutePath}, 文件存在=${file.exists()}")
                    val randomAccessFile = RandomAccessFile(file, "rw")
                    randomAccessFile.seek(start)

                    val buffer = ByteArray(8192)  // 8KB buffer
                    var bytesRead: Int = 0
                    var bytesWritten: Long = 0
                    val expectedSize = end - start + 1

                    try {
                        while (isDownloading.get() && responseBody.byteStream().read(buffer)
                                .also { bytesRead = it } != -1
                        ) {
                            randomAccessFile.write(buffer, 0, bytesRead)
                            bytesWritten += bytesRead
                            totalBytesRead.addAndGet(bytesRead.toLong())
                            onProgress(totalBytesRead.get(), contentLength)
                        }
                        DLog.d(TAG, "写入完成: range=$start-$end, 写入=$bytesWritten, 文件存在=${file.exists()}, 文件大小=${file.length()}")
                    } finally {
                        responseBody.close()
                        randomAccessFile.close()
                        DLog.d(TAG, "关闭后检查: range=$start-$end, 文件存在=${file.exists()}, 文件大小=${file.length()}")
                    }

                    // 验证是否下载了完整的数据块
                    if (bytesWritten != expectedSize) {
                        DLog.e(TAG, "数据块不完整: range=$start-$end, 期望=$expectedSize, 实际=$bytesWritten")
                        failedThreads.incrementAndGet()
                    } else {
                        currentDownloadedThread.incrementAndGet()
                    }
                    checkComplete()
                } ?: run {
                    DLog.e(TAG, "响应 body 为空: range=$start-$end")
                    failedThreads.incrementAndGet()
                    checkComplete()
                }
            } catch (e: Exception) {
                DLog.e(TAG, "下载异常: range=$start-$end, error=${e.message}")
                failedThreads.incrementAndGet()
                checkComplete()
            }
        }
        
        private fun checkComplete() {
            val completed = currentDownloadedThread.get() + failedThreads.get()
            if (completed == threadCount) {
                notifyComplete()
            }
        }
    }
}
