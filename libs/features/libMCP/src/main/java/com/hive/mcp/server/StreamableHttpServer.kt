// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.mcp.server

import com.hive.mcp.blob.BlobStore
import com.hive.mcp.model.McpRpcRequest
import com.hive.mcp.protocol.McpClientContext
import com.hive.mcp.service.McpService
import com.hive.utils.debug.DLog
import com.hive.utils.utils.GsonHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** 会话 TTL：2 小时无活动后过期（Cursor 等客户端可能长时间空闲） */
private const val SESSION_TTL_MS = 2 * 60 * 60 * 1000L

/** 会话清理间隔：每 5 分钟检查一次 */
private const val SESSION_CLEANUP_INTERVAL_MS = 5 * 60 * 1000L

private const val TAG = "StreamableHttpServer"

/**
 * Streamable HTTP MCP 服务器
 * 实现 MCP 2025-03-26 规范的 Streamable HTTP 传输协议
 */
class StreamableHttpServer(
    private val host: String = "0.0.0.0",
    private val port: Int,
    private val listener: StreamableHttpServerListener,
    private val blobStore: BlobStore? = null
) {
    interface StreamableHttpServerListener {
        fun onClientConnected(client: HttpClient)
        fun onClientDisconnected(client: HttpClient)
        fun onMessageReceived(client: HttpClient, message: String)
    }

    private val serverSocket = ServerSocketChannel.open()
    private val selector = Selector.open()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val clients = ConcurrentHashMap<SocketChannel, HttpClient>()
    private val sessions = ConcurrentHashMap<String, HttpSession>()

    @Volatile
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true

        serverSocket.bind(InetSocketAddress(host, port))
        serverSocket.configureBlocking(false)
        serverSocket.register(selector, SelectionKey.OP_ACCEPT)

        scope.launch {
            while (isRunning) {
                try {
                    if (selector.select(1000) > 0) {
                        val keys = selector.selectedKeys().iterator()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            keys.remove()
                            when {
                                key.isValid && key.isAcceptable -> handleAccept()
                                key.isValid && key.isReadable -> handleRead(key)
                            }
                        }
                    }
                } catch (e: IOException) {
                    stop()
                } catch (e: Exception) {
                    DLog.e(TAG, "Selector loop error", e)
                }
            }
        }
        scope.launch {
            while (isRunning) {
                delay(SESSION_CLEANUP_INTERVAL_MS)
                cleanupExpiredSessions()
            }
        }
    }

    private fun cleanupExpiredSessions() {
        val now = System.currentTimeMillis()
        sessions.entries.removeIf { (_, session) ->
            now - session.lastActivityAt > SESSION_TTL_MS
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        
        try {
            selector.close()
            serverSocket.close()
        } catch (e: IOException) {
            // Ignore
        }
        
        clients.values.forEach { it.disconnect() }
        clients.clear()
        sessions.clear()
        scope.cancel()
    }

    private fun handleAccept() {
        val clientChannel = serverSocket.accept()
        clientChannel?.configureBlocking(false)
        clientChannel?.register(selector, SelectionKey.OP_READ)
    }

    private fun handleRead(key: SelectionKey) {
        val clientChannel = key.channel() as SocketChannel
        val buffer = ByteBuffer.allocate(8192)
        
        try {
            if (clientChannel.read(buffer) == -1) {
                disconnectClient(clientChannel)
                return
            }
            
            buffer.flip()
            val data = StandardCharsets.UTF_8.decode(buffer).toString()
            handleHttpRequest(key, clientChannel, data)
        } catch (e: IOException) {
            disconnectClient(clientChannel)
        }
    }

    private fun handleHttpRequest(key: SelectionKey, clientChannel: SocketChannel, request: String) {
        val lines = request.lines()
        val requestLine = lines.firstOrNull() ?: ""
        val parts = requestLine.split(" ")
        if (parts.size < 2) {
            sendHttpResponse(clientChannel, "400 Bad Request", "text/plain", "Bad Request")
            return
        }
        val method = parts[0]
        val path = parts[1]
        val headers = parseHeaders(lines)
        val acceptHeader = headers["accept"] ?: ""
        val sessionId = headers["mcp-session-id"]
        val acceptsEventStream = acceptHeader.contains("text/event-stream", ignoreCase = true)
        val acceptsJson = acceptHeader.contains("application/json", ignoreCase = true)

        when {
            // GET /mcp - 建立 SSE 连接或初始化请求
            method == "GET" && (path == "/mcp" || path == "/") -> {
                if (acceptsEventStream) {
                    handleGetSseStream(clientChannel, sessionId)
                } else {
                    sendHttpResponse(clientChannel, "400 Bad Request", "text/plain", "Accept header must include text/event-stream")
                }
            }
            
            // POST /mcp - 发送 JSON-RPC 消息（异步处理，避免阻塞 Selector）
            method == "POST" && (path == "/mcp" || path == "/") -> {
                key.cancel()
                handlePostMessageAsync(clientChannel, request, headers, acceptsEventStream)
            }
            
            // DELETE /mcp - 终止会话
            method == "DELETE" && (path == "/mcp" || path == "/") -> {
                handleDeleteSession(clientChannel, sessionId)
            }
            
            // OPTIONS 请求 (CORS)
            method == "OPTIONS" -> {
                handleOptionsRequest(clientChannel)
            }

            // GET /blob/{id} - 返回二进制图片
            method == "GET" && path.startsWith("/blob/") -> {
                val blobId = path.removePrefix("/blob/").takeIf { it.isNotBlank() }
                handleGetBlob(clientChannel, blobId)
            }

            else -> {
                sendHttpResponse(clientChannel, "404 Not Found", "text/plain", "Endpoint not found")
            }
        }
    }

    private fun handleGetBlob(clientChannel: SocketChannel, blobId: String?) {
        if (blobId == null || blobStore == null) {
            sendHttpResponse(clientChannel, "404 Not Found", "text/plain", "Blob not found")
            return
        }
        val result = blobStore.get(blobId)
        if (result == null) {
            sendHttpResponse(clientChannel, "404 Not Found", "text/plain", "Blob not found")
            return
        }
        val (bytes, mimeType) = result
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: $mimeType\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }
        try {
            clientChannel.write(ByteBuffer.wrap(headers.toByteArray()))
            clientChannel.write(ByteBuffer.wrap(bytes))
        } catch (e: IOException) {
            // Ignore
        } finally {
            try {
                clientChannel.close()
            } catch (e: IOException) {
                // Ignore
            }
        }
    }

    private fun parseHeaders(lines: List<String>): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        for (line in lines.drop(1)) {
            if (line.isBlank()) break
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim().lowercase()
                val value = line.substring(colonIndex + 1).trim()
                headers[key] = value
            }
        }
        return headers
    }

    private fun extractBody(request: String): String {
        val delimiter = "\r\n\r\n"
        val idx = request.indexOf(delimiter)
        return if (idx != -1) request.substring(idx + delimiter.length) else ""
    }

    private fun handleGetSseStream(clientChannel: SocketChannel, sessionId: String?) {
        // 建立 SSE 连接
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: text/event-stream\r\n")
            append("Cache-Control: no-cache\r\n")
            append("Connection: keep-alive\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Access-Control-Allow-Methods: GET, POST, DELETE, OPTIONS\r\n")
            append("Access-Control-Allow-Headers: Content-Type, Accept, Mcp-Session-Id\r\n")
            append("\r\n")
        }
        
        try {
            clientChannel.write(ByteBuffer.wrap(headers.toByteArray()))
            val client = HttpClient(clientChannel, this, sessionId)
            clients[clientChannel] = client
            listener.onClientConnected(client)
            
            // 发送连接确认事件
            client.sendSseEvent("connected", "{\"status\":\"connected\"}")
        } catch (e: IOException) {
            disconnectClient(clientChannel)
        }
    }

    private fun handlePostMessageAsync(clientChannel: SocketChannel, request: String, headers: Map<String, String>, acceptsEventStream: Boolean) {
        val body = extractBody(request)
        val sessionId = headers["mcp-session-id"]
        if (body.isBlank()) {
            sendHttpResponse(clientChannel, "400 Bad Request", "text/plain", "Empty request body")
            return
        }
        val isInitializeRequest = try {
            GsonHelper.getInstance().fromJson(body, McpRpcRequest::class.java).method == "initialize"
        } catch (_: Exception) {
            false
        }
        if (sessionId != null && !sessions.containsKey(sessionId) && !isInitializeRequest) {
            sendHttpResponse(clientChannel, "404 Not Found", "text/plain", "Session not found")
            return
        }
        val mcpService = McpService.getInstance()
        if (mcpService == null) {
            sendHttpResponse(clientChannel, "500 Internal Server Error", "text/plain", "MCP Service not available")
            return
        }
        val protocol = mcpService.mcpProtocol
        val toolRegistry = mcpService.getToolRegistry()
        val resourceManager = mcpService.getResourceManager()
        val promptManager = mcpService.getPromptManager()

        val clientContext = McpClientContext(
            remoteAddress = try { clientChannel.remoteAddress?.toString() } catch (_: Exception) { null },
            headers = headers
        )

        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    protocol.processMessage(body, toolRegistry, resourceManager, promptManager, clientContext)
                }
                var newSessionId = sessionId
                if (isInitializeRequest && (sessionId == null || !sessions.containsKey(sessionId))) {
                    newSessionId = UUID.randomUUID().toString()
                    sessions[newSessionId] = HttpSession(newSessionId)
                } else if (sessionId != null && sessions.containsKey(sessionId)) {
                    sessions[sessionId]?.refreshActivity()
                }
                if (acceptsEventStream) {
                    sendStreamableHttpResponse(clientChannel, response, newSessionId)
                } else {
                    sendJsonResponse(clientChannel, response, newSessionId)
                }
            } catch (e: Exception) {
                DLog.e(TAG, "handlePostMessage error: ${e.message}", e)
                sendHttpResponse(clientChannel, "500 Internal Server Error", "text/plain", "Internal error: ${e.message}")
            }
        }
    }

    private fun handleDeleteSession(clientChannel: SocketChannel, sessionId: String?) {
        if (sessionId != null) {
            sessions.remove(sessionId)
            sendHttpResponse(clientChannel, "200 OK", "text/plain", "Session terminated")
        } else {
            sendHttpResponse(clientChannel, "400 Bad Request", "text/plain", "Session ID required")
        }
    }

    private fun handleOptionsRequest(clientChannel: SocketChannel) {
        val headers = buildString {
            append("HTTP/1.1 204 No Content\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Access-Control-Allow-Methods: GET, POST, DELETE, OPTIONS\r\n")
            append("Access-Control-Allow-Headers: Content-Type, Accept, Mcp-Session-Id\r\n")
            append("Access-Control-Max-Age: 86400\r\n")
            append("Connection: keep-alive\r\n")
            append("\r\n")
        }
        sendRaw(clientChannel, headers)
        clientChannel.close()
    }

    private fun sendStreamableHttpResponse(clientChannel: SocketChannel, response: String, sessionId: String?) {
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: text/event-stream\r\n")
            append("Cache-Control: no-cache\r\n")
            append("Connection: keep-alive\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Access-Control-Allow-Methods: GET, POST, DELETE, OPTIONS\r\n")
            append("Access-Control-Allow-Headers: Content-Type, Accept, Mcp-Session-Id\r\n")
            if (sessionId != null) {
                append("Mcp-Session-Id: $sessionId\r\n")
            }
            append("\r\n")
        }
        
        val sseMessage = buildString {
            append("data: $response\r\n")
            append("\r\n")
        }
        
        sendRaw(clientChannel, headers + sseMessage)
        clientChannel.close()
    }
    
    private fun sendJsonResponse(clientChannel: SocketChannel, response: String, sessionId: String?) {
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: application/json; charset=utf-8\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Access-Control-Allow-Methods: GET, POST, DELETE, OPTIONS\r\n")
            append("Access-Control-Allow-Headers: Content-Type, Accept, Mcp-Session-Id\r\n")
            append("Connection: keep-alive\r\n")
            append("Content-Length: ${response.toByteArray().size}\r\n")
            if (sessionId != null) {
                append("Mcp-Session-Id: $sessionId\r\n")
            }
            append("\r\n")
        }
        sendRaw(clientChannel, headers + response)
        clientChannel.close()
    }

    private fun sendHttpResponse(clientChannel: SocketChannel, status: String, contentType: String, body: String) {
        val response = buildString {
            append("HTTP/1.1 $status\r\n")
            append("Content-Type: $contentType; charset=utf-8\r\n")
            append("Content-Length: ${body.toByteArray().size}\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Connection: close\r\n")
            append("\r\n")
            append(body)
        }
        sendRaw(clientChannel, response)
        clientChannel.close()
    }

    internal fun sendRaw(clientChannel: SocketChannel, data: String) {
        try {
            clientChannel.write(ByteBuffer.wrap(data.toByteArray()))
        } catch (e: IOException) {
            disconnectClient(clientChannel)
        }
    }

    internal fun disconnectClient(clientChannel: SocketChannel) {
        clients.remove(clientChannel)?.let {
            it.disconnect()
            listener.onClientDisconnected(it)
        }
    }

    // 向指定会话的所有客户端发送消息
    fun sendToSession(sessionId: String, message: String) {
        clients.values.filter { it.sessionId == sessionId }.forEach { client ->
            client.sendSseEvent("message", message)
        }
    }
}

class HttpClient(
    private val channel: SocketChannel,
    private val server: StreamableHttpServer,
    val sessionId: String?
) {
    val id: String = channel.remoteAddress.toString()
    @Volatile var isConnected: Boolean = true

    fun sendSseEvent(event: String, data: String) {
        if (!isConnected) return
        val sseMessage = buildString {
            append("event: $event\r\n")
            data.lines().forEach { line ->
                append("data: $line\r\n")
            }
            append("\r\n")
        }
        server.sendRaw(channel, sseMessage)
    }

    fun disconnect() {
        isConnected = false
        try {
            channel.close()
        } catch (e: IOException) {
            // Ignore
        }
    }
}

data class HttpSession(
    val id: String,
    val createdAt: Long = System.currentTimeMillis(),
    var lastActivityAt: Long = System.currentTimeMillis()
) {
    fun refreshActivity() {
        lastActivityAt = System.currentTimeMillis()
    }
} 