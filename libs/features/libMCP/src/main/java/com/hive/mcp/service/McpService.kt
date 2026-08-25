// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.mcp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hive.mcp.blob.BlobStore
import com.hive.mcp.protocol.McpProtocol
import com.hive.mcp.registry.PromptManager
import com.hive.mcp.registry.ResourceManager
import com.hive.mcp.registry.ToolRegistry
import com.hive.mcp.server.SseClient
import com.hive.mcp.server.SseServer
import com.hive.mcp.server.StreamableHttpServer
import com.hive.mcp.server.HttpClient
import com.hive.plugin.provider.IMcpProvider
import com.hive.mcp.R
import com.hive.plugin.mcp.McpConst
import com.hive.utils.debug.DLog
import kotlinx.coroutines.*

class McpService : Service(), SseServer.SseServerListener,
    StreamableHttpServer.StreamableHttpServerListener {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var sseServer: SseServer? = null
    private var streamableHttpServer: StreamableHttpServer? = null

    // MCP Components
    private val toolRegistry = ToolRegistry()
    private val resourceManager = ResourceManager()
    private val promptManager = PromptManager()
    private val blobStore = BlobStore(this)
    lateinit var mcpProtocol: McpProtocol


    override fun onCreate() {
        super.onCreate()
        instance = this
        mcpProtocol = McpProtocol(this)
        DLog.i(TAG, "MCP Service creating...")
        blobStore.warmupCleanup()
        setupForegroundService()
        startServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        DLog.i(TAG, "MCP Service destroying...")
        stopServer()
        serviceJob.cancel()
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startServer() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                // 启动 SSE 服务器
                sseServer = SseServer(port = McpConst.SsePort, listener = this@McpService)
                sseServer?.start()
                DLog.i(TAG, "MCP SSE Server started on port ${McpConst.SsePort}")

                // 启动 Streamable HTTP 服务器
                streamableHttpServer =
                    StreamableHttpServer(port = McpConst.StreamablePort, listener = this@McpService, blobStore = blobStore)
                streamableHttpServer?.start()
                DLog.i(TAG, "MCP Streamable HTTP Server started on port ${McpConst.StreamablePort}")
                withContext(Dispatchers.Main) {
                    onServiceStatusCallback?.onMcpServiceReady()
                }
            } catch (e: Exception) {
                DLog.e(TAG, "Failed to start MCP Servers: ${e.message}")
                withContext(Dispatchers.Main) {
                    onServiceStatusCallback?.onMcpServiceReady()
                }
            }
        }
    }

    private fun stopServer() {
        sseServer?.stop()
        sseServer = null
        DLog.i(TAG, "MCP SSE Server stopped")

        streamableHttpServer?.stop()
        streamableHttpServer = null
        DLog.i(TAG, "MCP Streamable HTTP Server stopped")

        // 保留磁盘 blob，避免服务重启后历史图片立即失效；仅清理内存索引。
        blobStore.clearMemoryIndex()
    }

    // --- SseServer.SseServerListener Implementation ---

    override fun onSseClientConnected(client: SseClient) {
        DLog.i(TAG, "SSE Client connected: ${client.id}")
    }

    override fun onSseClientDisconnected(client: SseClient) {
        DLog.i(TAG, "SSE Client disconnected: ${client.id}")
    }

    override fun onSseMessageReceived(client: SseClient, message: String) {
        DLog.d(TAG, "SSE Message from ${client.id}: $message")
        serviceScope.launch {
            try {
                val response = mcpProtocol.processMessage(
                    message,
                    toolRegistry,
                    resourceManager,
                    promptManager
                )
                client.sendEvent("message", response)
            } catch (e: Exception) {
                DLog.e(TAG, "Error processing SSE message: ${e.message}")
                // Optionally send an error back to the client
            }
        }
    }

    // --- StreamableHttpServer.StreamableHttpServerListener Implementation ---

    override fun onClientConnected(client: HttpClient) {
        DLog.i(TAG, "HTTP Client connected: ${client.id}, session: ${client.sessionId}")
    }

    override fun onClientDisconnected(client: HttpClient) {
        DLog.i(TAG, "HTTP Client disconnected: ${client.id}, session: ${client.sessionId}")
    }

    override fun onMessageReceived(client: HttpClient, message: String) {
        DLog.d(TAG, "HTTP Message from ${client.id}: $message")
        serviceScope.launch {
            try {
                val response = mcpProtocol.processMessage(
                    message,
                    toolRegistry,
                    resourceManager,
                    promptManager
                )
                client.sendSseEvent("message", response)
            } catch (e: Exception) {
                DLog.e(TAG, "Error processing HTTP message: ${e.message}")
                // Optionally send an error back to the client
            }
        }
    }

    // --- Public API for Registration ---

    fun getToolRegistry(): ToolRegistry = toolRegistry

    fun getResourceManager(): ResourceManager = resourceManager

    fun getPromptManager(): PromptManager = promptManager

    fun getBlobStore(): BlobStore = blobStore

    // --- Foreground Service Management ---

    private fun setupForegroundService() {
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires specifying foreground service type
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "MCP Service running status"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(com.hive.i8n.R.string.mcp_service_title))
            .setContentText(getString(com.hive.i8n.R.string.mcp_service_running, McpConst.SsePort, McpConst.StreamablePort))
            .setSmallIcon(com.hive.i8n.R.drawable.logo)
            .setOngoing(true)
            .build()
    }


    companion object {
        private const val TAG = "McpService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "mcp_service_channel"
        private const val CHANNEL_NAME = "MCP Service"


        var onServiceStatusCallback: IMcpProvider.OnServiceStatusCallback? = null


        @Volatile
        private var instance: McpService? = null

        fun getInstance(): McpService? = instance

        fun isRunning(): Boolean = instance != null

        fun start(
            context: Context,
            ssePort: Int,
            streamablePort: Int,
            callback: IMcpProvider.OnServiceStatusCallback
        ) {
            ensureStop()
            onServiceStatusCallback = callback
            McpConst.SsePort = ssePort
            McpConst.StreamablePort = streamablePort
            val intent = Intent(context, McpService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, McpService::class.java)
            context.stopService(intent)
        }

        private fun ensureStop() {
            onServiceStatusCallback = null
            instance?.stopSelf()
            instance = null
        }
    }
} 
