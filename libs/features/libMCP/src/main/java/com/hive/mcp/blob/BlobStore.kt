// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.mcp.blob

import android.content.Context
import androidx.annotation.Keep
import com.hive.utils.debug.DLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * MCP Blob 持久缓存
 * 用于截图/拍照等大文件的 URL 引用，避免 Base64 内嵌 JSON
 * 数据落盘到 filesDir，允许服务重启后短期继续访问。
 *
 * @param context 用于 filesDir
 * @param ttlMs 单文件 TTL 毫秒，默认 48 小时
 * @param maxFileSizeBytes 单文件最大字节，默认 5MB
 * @param maxTotalBytes 总容量上限字节，默认 50MB
 */
@Keep
class BlobStore(
    private val context: Context,
    private val ttlMs: Long = 48 * 60 * 60 * 1000L,
    private val maxFileSizeBytes: Int = 5 * 1024 * 1024,
    private val maxTotalBytes: Long = 50 * 1024 * 1024L
) {
    private val blobDir: File by lazy {
        File(context.filesDir, "mcp_blobs").also { if (!it.exists()) it.mkdirs() }
    }

    private val metaDir: File by lazy {
        File(blobDir, "meta").also { if (!it.exists()) it.mkdirs() }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val blobMeta = ConcurrentHashMap<String, BlobMeta>()

    data class BlobMeta(
        val mimeType: String,
        val size: Long,
        val createdAt: Long
    )

    /**
     * 存储二进制数据，返回 blobId
     * @return blobId 或 null（超限/失败）
     */
    fun put(data: ByteArray, mimeType: String): String? {
        if (data.size > maxFileSizeBytes) {
            DLog.w(TAG, "Blob too large: ${data.size} > $maxFileSizeBytes")
            return null
        }
        val blobId = UUID.randomUUID().toString()
        val file = File(blobDir, blobId)
        val metaFile = File(metaDir, "$blobId.meta")
        return try {
            file.writeBytes(data)
            metaFile.writeText("$mimeType\n${data.size}\n${System.currentTimeMillis()}")
            blobMeta[blobId] = BlobMeta(mimeType, data.size.toLong(), System.currentTimeMillis())
            scope.launch { cleanupIfNeeded() }
            blobId
        } catch (e: Exception) {
            DLog.e(TAG, "BlobStore put failed: ${e.message}")
            file.delete()
            metaFile.delete()
            null
        }
    }

    /**
     * 获取 blob，返回 (bytes, mimeType) 或 null
     */
    fun get(blobId: String): Pair<ByteArray, String>? {
        if (blobId.isBlank()) return null
        val file = File(blobDir, blobId)
        if (!file.exists()) return null
        val memoryMeta = blobMeta[blobId]
        val meta = memoryMeta ?: readMeta(blobId)?.also {
            blobMeta[blobId] = it
            DLog.d(TAG, "Blob restored from disk meta: $blobId")
        } ?: return null
        if (memoryMeta != null) {
            DLog.d(TAG, "Blob hit memory meta: $blobId")
        }
        if (System.currentTimeMillis() - meta.createdAt > ttlMs) {
            DLog.i(TAG, "Blob expired: $blobId")
            remove(blobId)
            return null
        }
        return try {
            file.readBytes() to meta.mimeType
        } catch (e: Exception) {
            DLog.e(TAG, "BlobStore get failed: ${e.message}")
            null
        }
    }

    private fun readMeta(blobId: String): BlobMeta? {
        val metaFile = File(metaDir, "$blobId.meta")
        if (!metaFile.exists()) return null
        return try {
            val lines = metaFile.readLines()
            if (lines.size >= 3) {
                BlobMeta(lines[0], lines[1].toLongOrNull() ?: 0L, lines[2].toLongOrNull() ?: 0L)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun remove(blobId: String) {
        File(blobDir, blobId).delete()
        File(metaDir, "$blobId.meta").delete()
        blobMeta.remove(blobId)
    }

    private fun cleanupIfNeeded() {
        try {
            val now = System.currentTimeMillis()
            val files = blobDir.listFiles() ?: return
            var totalSize = 0L

            for (f in files) {
                if (f.isFile) {
                    val meta = blobMeta[f.name] ?: readMeta(f.name)
                    if (meta != null) {
                        if (now - meta.createdAt > ttlMs) {
                            DLog.d(TAG, "Blob evicted by ttl: ${f.name}")
                            remove(f.name)
                        } else {
                            totalSize += meta.size
                        }
                    }
                }
            }

            if (totalSize > maxTotalBytes) {
                val sorted = files
                    .filter { it.isFile }
                    .mapNotNull { f ->
                        val m = blobMeta[f.name] ?: readMeta(f.name)
                        if (m != null) Triple(f.name, m.createdAt, m.size) else null
                    }
                    .sortedBy { it.second }
                for ((id, _, size) in sorted) {
                    DLog.d(TAG, "Blob evicted by size: $id")
                    remove(id)
                    totalSize -= size
                    if (totalSize <= maxTotalBytes) break
                }
            }
        } catch (e: Exception) {
            DLog.e(TAG, "BlobStore cleanup failed: ${e.message}")
        }
    }

    fun clearMemoryIndex() {
        blobMeta.clear()
        DLog.i(TAG, "Blob memory index cleared")
    }

    fun warmupCleanup() {
        scope.launch { cleanupIfNeeded() }
    }

    fun cleanup() {
        try {
            blobDir.listFiles()?.forEach { if (it.isFile) it.delete() }
            metaDir.listFiles()?.forEach { it.delete() }
            blobMeta.clear()
        } catch (e: Exception) {
            DLog.e(TAG, "BlobStore cleanup all failed: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "BlobStore"
    }
}
