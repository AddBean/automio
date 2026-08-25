// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.mcp.server

import com.hive.mcp.protocol.McpClientContext
import com.hive.mcp.service.McpService
import com.hive.utils.debug.DLog
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
import java.util.concurrent.ConcurrentHashMap

private const val SSE_TAG = "SseServer"

/**
 * 通用的、轻量级的 SSE (Server-Sent Events) 服务器
 */
class SseServer(
    private val host: String = "0.0.0.0",
    private val port: Int,
    private val listener: SseServerListener
) {
    interface SseServerListener {
        fun onSseClientConnected(client: SseClient)
        fun onSseClientDisconnected(client: SseClient)
        fun onSseMessageReceived(client: SseClient, message: String)
    }

    private val serverSocket = ServerSocketChannel.open()
    private val selector = Selector.open()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val clients = ConcurrentHashMap<SocketChannel, SseClient>()

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
                    DLog.e(SSE_TAG, "Selector loop error", e)
                }
            }
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
            
            val client = clients[clientChannel]
            if (client != null && client.isSse) {
                // Already an SSE connection, treat as message
                listener.onSseMessageReceived(client, data.trim())
            } else {
                handleHttpRequest(key, clientChannel, data)
            }
        } catch (e: IOException) {
            disconnectClient(clientChannel)
        }
    }

    private fun handleHttpRequest(key: SelectionKey, clientChannel: SocketChannel, request: String) {
        val requestLine = request.lines().firstOrNull() ?: ""
        val parts = requestLine.split(" ")
        if (parts.size < 2) {
            sendHttpResponse(clientChannel, "400 Bad Request", "text/plain", "Bad Request")
            return
        }
        val method = parts[0]
        val path = parts[1]
        if (method == "GET" && (path == "/sse" || path == "/")) {
            establishSseConnection(clientChannel)
        } else if (method == "OPTIONS") {
            handleOptionsRequest(clientChannel)
        } else if (method == "POST" && (path == "/sse" || path == "/")) {
            key.cancel()
            handlePostRequestAsync(clientChannel, request)
        } else {
            sendHttpResponse(clientChannel, "404 Not Found", "text/plain", "Not Found")
        }
    }

    private fun handlePostRequestAsync(clientChannel: SocketChannel, request: String) {
        val delimiter = "\r\n\r\n"
        val idx = request.indexOf(delimiter)
        val body = if (idx != -1) request.substring(idx + delimiter.length) else ""
        val mcpService = McpService.getInstance()
        if (mcpService == null) {
            sendHttpResponse(clientChannel, "500 Internal Server Error", "text/plain", "MCP Service not available")
            return
        }
        val protocol = mcpService.mcpProtocol
        val toolRegistry = mcpService.getToolRegistry()
        val resourceManager = mcpService.getResourceManager()
        val promptManager = mcpService.getPromptManager()

        val lines = request.lines()
        val headerMap = mutableMapOf<String, String>()
        for (line in lines.drop(1)) {
            if (line.isBlank()) break
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                headerMap[line.substring(0, colonIndex).trim().lowercase()] = line.substring(colonIndex + 1).trim()
            }
        }
        val clientContext = McpClientContext(
            remoteAddress = try { clientChannel.remoteAddress?.toString() } catch (_: Exception) { null },
            headers = headerMap
        )

        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    protocol.processMessage(body, toolRegistry, resourceManager, promptManager, clientContext)
                }
                val headers = (
                    "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json; charset=utf-8\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                    "Access-Control-Allow-Headers: Content-Type, Accept\r\n" +
                    "Connection: keep-alive\r\n" +
                    "Content-Length: ${response.toByteArray().size}\r\n" +
                    "\r\n"
                )
                sendRaw(clientChannel, headers + response)
                clientChannel.close()
            } catch (e: Exception) {
                DLog.e(SSE_TAG, "handlePostRequest error: ${e.message}", e)
                sendHttpResponse(clientChannel, "500 Internal Server Error", "text/plain", "Internal error: ${e.message}")
            }
        }
    }

    private fun establishSseConnection(clientChannel: SocketChannel) {
        val headers = (
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/event-stream\r\n" +
            "Cache-Control: no-cache\r\n" +
            "Connection: keep-alive\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "\r\n"
        )
        try {
            clientChannel.write(ByteBuffer.wrap(headers.toByteArray()))
            val client = SseClient(clientChannel, this)
            clients[clientChannel] = client
            listener.onSseClientConnected(client)
            client.sendEvent("connected", "{\"status\":\"ok\"}")
        } catch (e: IOException) {
            disconnectClient(clientChannel)
        }
    }
    
    private fun handleOptionsRequest(clientChannel: SocketChannel) {
        val headers = (
            "HTTP/1.1 204 No Content\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
            "Access-Control-Allow-Headers: Content-Type, Accept\r\n" +
            "Access-Control-Max-Age: 86400\r\n" +
            "Connection: keep-alive\r\n" +
            "\r\n"
        )
        sendRaw(clientChannel, headers)
    }

    internal fun sendRaw(clientChannel: SocketChannel, data: String) {
        try {
            clientChannel.write(ByteBuffer.wrap(data.toByteArray()))
        } catch (e: IOException) {
            disconnectClient(clientChannel)
        }
    }

    private fun sendHttpResponse(clientChannel: SocketChannel, status: String, contentType: String, body: String) {
        val response = (
            "HTTP/1.1 $status\r\n" +
            "Content-Type: $contentType; charset=utf-8\r\n" +
            "Content-Length: ${body.toByteArray().size}\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "\r\n" +
            "$body"
        )
        sendRaw(clientChannel, response)
        clientChannel.close()
    }

    internal fun disconnectClient(clientChannel: SocketChannel) {
        clients.remove(clientChannel)?.let {
            it.disconnect()
            listener.onSseClientDisconnected(it)
        }
    }
}

class SseClient(
    private val channel: SocketChannel,
    private val server: SseServer
) {
    val id: String = channel.remoteAddress.toString()
    @Volatile var isSse: Boolean = true
    private val heartbeatScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        startHeartbeat()
    }

    fun sendEvent(event: String, data: String) {
        if (!isSse) return
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
        isSse = false
        heartbeatScope.cancel()
        try {
            channel.close()
        } catch (e: IOException) {
            // Ignore
        }
    }
    
    private fun startHeartbeat() {
        heartbeatScope.launch {
            while (isSse) {
                delay(30000) // 30-second heartbeat
                server.sendRaw(channel, ":heartbeat\r\n\r\n")
            }
        }
    }
} 