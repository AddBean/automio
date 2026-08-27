// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.MessageStatus
import com.hive.utils.global.MMKVTools
import com.hive.utils.utils.GsonHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private const val SESSIONS_DIR_NAME = "agent_sessions"
private const val INDEX_FILE_NAME = "agent_sessions_index.json"
private const val MMKV_LAST_SESSION_KEY = "agent_last_session_key"
private const val TITLE_MAX_LEN = 30
private const val KEY_MAX_FILENAME_LEN = 200
private val INVALID_FILENAME_CHARS = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')

data class SessionMeta(
    val sessionKey: String,
    val title: String,
    val createTime: Long,
    val updateTime: Long
)

data class SessionIndex(
    val sessions: MutableList<SessionMeta> = mutableListOf(),
    var lastSessionKey: String? = null
)

/** 单会话文件结构（messages 用 JSON 字符串存储，避免跨模块序列化） */
data class SessionFileData(
    val sessionKey: String,
    val title: String,
    val createTime: Long,
    val updateTime: Long,
    val messagesJson: String,
    val commandsJson: String? = null  // JSON array，每行一个元素（含 for 块已展平）
)

/** 加载后的会话数据（messages 已反序列化） */
data class LoadedSession(
    val sessionKey: String,
    val title: String,
    val createTime: Long,
    val updateTime: Long,
    val messages: List<ChatMessage>,
    val commands: List<String> = emptyList()
)

/**
 * Agent 会话持久化：本地 JSON 文件存储，索引 + 按 sessionKey 命名的单会话文件。
 * 会话 key 以用户第一次 input 生成（截断、文件名安全、冲突时加时间戳）。
 */
class AgentSessionStorage(context: Context) {

    /** 统一使用 applicationContext 保证多入口（Fragment/BottomSheet）读写同一目录 */
    private val appContext = context.applicationContext

    private val sessionsDir: File by lazy {
        File(appContext.filesDir, SESSIONS_DIR_NAME).also { dir ->
            if (!dir.exists() && !dir.mkdirs()) {
                com.hive.utils.debug.DLog.w("AgentSessionStorage", "mkdirs failed: ${dir.absolutePath}")
            }
        }
    }

    private val indexFile: File by lazy {
        File(sessionsDir, INDEX_FILE_NAME)
    }

    /** 确保会话目录存在（写入前调用，避免异步时 lazy 未初始化或创建失败） */
    private fun ensureSessionsDirExists(): Boolean {
        return sessionsDir.exists() || sessionsDir.mkdirs()
    }

    private val gsonHelper get() = GsonHelper.getInstance()
    /** 消息序列化专用 Gson（FunctionCall.arguments 为 Gson JsonObject，可原生序列化） */
    private val messagesGson: Gson = GsonHelper.getInstance().getGson()
    private val messagesType = object : TypeToken<List<ChatMessage>>() {}.type
    private val commandsType = object : TypeToken<List<String>>() {}.type
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val saveScheduled = AtomicBoolean(false)

    /**
     * 根据首条用户 input 生成会话 key（用于文件名与唯一标识）。
     * 无文字时用 "new_chat"；长文本截断；非法字符替换；冲突时追加时间戳。
     */
    fun generateKeyFromFirstInput(firstInput: String?): String {
        val raw = firstInput?.trim()?.take(TITLE_MAX_LEN) ?: ""
        val base = if (raw.isEmpty()) {
            "new_chat"
        } else {
            raw.map { c -> if (c in INVALID_FILENAME_CHARS || c.code < 32) '_' else c }
                .joinToString("")
                .replace(' ', '_')
                .take(KEY_MAX_FILENAME_LEN)
                .ifEmpty { "new_chat" }
        }
        val safeBase = base.ifEmpty { "new_chat" }
        val fileBase = safeBase.take(KEY_MAX_FILENAME_LEN - 15)
        return if (sessionFileForKey(fileBase).exists()) {
            "${fileBase}_${System.currentTimeMillis()}"
        } else {
            fileBase
        }
    }

    private fun sessionFileForKey(sessionKey: String): File {
        val safeName = sessionKey.map { c ->
            if (c in INVALID_FILENAME_CHARS || c == File.separatorChar) '_' else c
        }.joinToString("").ifEmpty { "session" }
        return File(sessionsDir, "$safeName.json")
    }

    /**
     * 同步保存当前会话（首次发送时调用，保证在 sendChatMessage 返回前落盘）。
     * 失败仅打日志，不抛异常。
     * @param commands 可选，Agent 录制的 cmd 指令列表（含 for 块已展平），无则传 null
     */
    fun saveCurrentSessionSync(
        sessionKey: String,
        title: String,
        messages: List<ChatMessage>,
        commands: List<String>? = null
    ) {
        if (sessionKey.isBlank()) return
        try {
            if (!ensureSessionsDirExists()) {
                com.hive.utils.debug.DLog.w("AgentSessionStorage", "saveCurrentSessionSync: dir not ready")
                return
            }
            val now = System.currentTimeMillis()
            val index = readIndex()
            val existing = index.sessions.find { it.sessionKey == sessionKey }

            // 直接使用传入的 commands（录制器返回完整状态），避免累加重复
            val finalCommands = (commands ?: emptyList()).filter { it.isNotBlank() }

            val meta = SessionMeta(sessionKey, title, existing?.createTime ?: now, now)
            if (existing != null) {
                index.sessions.remove(existing)
            }
            index.sessions.add(0, meta)
            index.lastSessionKey = sessionKey
            writeIndex(index)

            val messagesJson = messagesGson.toJson(messages, messagesType)
            val commandsJson = finalCommands.takeIf { it.isNotEmpty() }?.let { messagesGson.toJson(it, commandsType) }
            val fileData = SessionFileData(
                sessionKey = sessionKey,
                title = title,
                createTime = existing?.createTime ?: now,
                updateTime = now,
                messagesJson = messagesJson,
                commandsJson = commandsJson
            )
            sessionFileForKey(sessionKey).writeText(gsonHelper.toJson(fileData))
        } catch (e: Exception) {
            com.hive.utils.debug.DLog.e("AgentSessionStorage", "saveCurrentSessionSync failed: ${e.message}", e)
        }
    }

    /**
     * 异步保存当前会话（每次 AI 有新消息或发送成功后由 Fragment 调用）。
     * 失败仅打日志，不抛异常。
     * @param commands 可选，Agent 录制的 cmd 指令列表（含 for 块已展平），无则传 null
     */
    fun saveCurrentSessionAsync(
        sessionKey: String,
        title: String,
        messages: List<ChatMessage>,
        commands: List<String>? = null
    ) {
        if (sessionKey.isBlank()) return
        scope.launch {
            saveScheduled.set(true)
            withContext(Dispatchers.IO) {
                try {
                    if (!ensureSessionsDirExists()) {
                        com.hive.utils.debug.DLog.w("AgentSessionStorage", "saveCurrentSessionAsync: dir not ready")
                        return@withContext
                    }
                    val now = System.currentTimeMillis()

                    // 直接使用传入的 commands（录制器返回完整状态），避免累加重复
                    val finalCommands = (commands ?: emptyList()).filter { it.isNotBlank() }

                    val index = readIndex()
                    val existing = index.sessions.find { it.sessionKey == sessionKey }
                    val meta = SessionMeta(sessionKey, title, now, now)
                    if (existing != null) {
                        index.sessions.remove(existing)
                        index.sessions.add(0, meta.copy(createTime = existing.createTime, updateTime = now))
                    } else {
                        index.sessions.add(0, meta)
                    }
                    index.lastSessionKey = sessionKey
                    writeIndex(index)

                    val messagesJson = messagesGson.toJson(messages, messagesType)
                    val commandsJson = finalCommands.takeIf { it.isNotEmpty() }?.let { messagesGson.toJson(it, commandsType) }
                    val fileData = SessionFileData(
                        sessionKey = sessionKey,
                        title = title,
                        createTime = existing?.createTime ?: now,
                        updateTime = now,
                        messagesJson = messagesJson,
                        commandsJson = commandsJson
                    )
                    val file = sessionFileForKey(sessionKey)
                    file.writeText(gsonHelper.toJson(fileData))
                } catch (e: Exception) {
                    com.hive.utils.debug.DLog.e("AgentSessionStorage", "saveCurrentSessionAsync failed: ${e.message}", e)
                } finally {
                    saveScheduled.set(false)
                }
            }
        }
    }

    /**
     * 仅更新索引中该会话的 updateTime（会话文件已存在且仅需刷新顺序时可用）。
     */
    fun touchSessionInIndex(sessionKey: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val index = readIndex()
                    val idx = index.sessions.indexOfFirst { it.sessionKey == sessionKey }
                    if (idx >= 0) {
                        val m = index.sessions[idx]
                        index.sessions.removeAt(idx)
                        index.sessions.add(0, m.copy(updateTime = System.currentTimeMillis()))
                        index.lastSessionKey = sessionKey
                        writeIndex(index)
                    }
                } catch (e: Exception) {
                    com.hive.utils.debug.DLog.w("AgentSessionStorage", "touchSessionInIndex failed: ${e.message}")
                }
            }
        }
    }

    fun readIndex(): SessionIndex {
        if (!indexFile.exists()) return SessionIndex()
        return try {
            gsonHelper.fromJson(indexFile.readText(), SessionIndex::class.java) ?: SessionIndex()
        } catch (e: Exception) {
            com.hive.utils.debug.DLog.w("AgentSessionStorage", "readIndex failed: ${e.message}", e)
            SessionIndex()
        }
    }

    private fun writeIndex(index: SessionIndex) {
        if (!ensureSessionsDirExists()) return
        try {
            indexFile.writeText(gsonHelper.toJson(index))
        } catch (e: Exception) {
            com.hive.utils.debug.DLog.w("AgentSessionStorage", "writeIndex failed: ${e.message}", e)
        }
    }

    /**
     * 加载指定会话的消息列表；文件不存在或解析失败返回 null。
     */
    fun loadSession(sessionKey: String): LoadedSession? {
        if (sessionKey.isBlank()) return null
        val file = sessionFileForKey(sessionKey)
        if (!file.exists()) return null
        return try {
            val data = gsonHelper.fromJson(file.readText(), SessionFileData::class.java) ?: return null
            val messages = sanitizeLoadedMessages(
                messagesGson.fromJson<List<ChatMessage>>(data.messagesJson, messagesType) ?: emptyList()
            )
            val commands = data.commandsJson?.let { messagesGson.fromJson<List<String>>(it, commandsType) }
                ?: emptyList<String>()
            LoadedSession(
                sessionKey = data.sessionKey,
                title = data.title,
                createTime = data.createTime,
                updateTime = data.updateTime,
                messages = messages,
                commands = commands
            )
        } catch (e: Exception) {
            com.hive.utils.debug.DLog.w("AgentSessionStorage", "loadSession failed: ${e.message}", e)
            null
        }
    }

    /**
     * 历史会话不应保留“推理中”占位。
     * 如果应用在推理过程中退出，磁盘里可能会留下末尾的 WAITING 空 assistant 消息，
     * 这里在恢复时统一清理，避免聊天页默认显示一个假 loading。
     */
    private fun sanitizeLoadedMessages(messages: List<ChatMessage>): List<ChatMessage> {
        val cleaned = messages.mapNotNull { message ->
            val isEmptyAssistantPlaceholder =
                message.role == MessageRole.ASSISTANT &&
                    message.status == MessageStatus.WAITING &&
                    message.content.isNullOrBlank() &&
                    message.reasoningContent.isNullOrBlank() &&
                    message.attachments.isEmpty() &&
                    message.toolCalls.isNullOrEmpty()

            if (isEmptyAssistantPlaceholder) {
                null
            } else {
                message.copy(
                    status = when (message.status) {
                        MessageStatus.WAITING, MessageStatus.TOOL_RUNNING -> MessageStatus.FINISH
                        MessageStatus.FINISH -> MessageStatus.FINISH
                    },
                    attachments = message.attachments.toMutableList()
                )
            }
        }
        // 进程被杀时可能留下「有 tool_calls 但缺紧邻 tool 响应」的尾巴，恢复时剥离以免下次推理 400
        return sanitizeIncompleteToolCallPairs(cleaned)
    }

    private fun sanitizeIncompleteToolCallPairs(messages: List<ChatMessage>): List<ChatMessage> {
        if (messages.isEmpty()) return messages
        val result = mutableListOf<ChatMessage>()
        var index = 0
        while (index < messages.size) {
            val message = messages[index]
            if (message.role == MessageRole.ASSISTANT && !message.toolCalls.isNullOrEmpty()) {
                val expectedIds = message.toolCalls!!
                    .map { it.id }
                    .filter { it.isNotEmpty() }
                    .toSet()
                val toolResponses = mutableListOf<ChatMessage>()
                var cursor = index + 1
                while (cursor < messages.size && messages[cursor].role == MessageRole.TOOL) {
                    toolResponses.add(messages[cursor])
                    cursor++
                }
                val responseIds = toolResponses.mapNotNull { it.toolCallId?.takeIf { id -> id.isNotEmpty() } }.toSet()
                if (expectedIds.isNotEmpty() && expectedIds == responseIds) {
                    result.add(message)
                    result.addAll(toolResponses)
                } else {
                    result.add(message.copy(toolCalls = null))
                }
                index = cursor
                continue
            }
            if (message.role == MessageRole.TOOL) {
                index++
                continue
            }
            result.add(message)
            index++
        }
        return result
    }

    /**
     * 会话列表，按 updateTime 降序。
     */
    fun listSessions(): List<SessionMeta> {
        return readIndex().sessions.sortedByDescending { it.updateTime }
    }

    /**
     * 删除指定会话文件并从索引中移除。
     */
    fun deleteSession(sessionKey: String) {
        try {
            sessionFileForKey(sessionKey).takeIf { it.exists() }?.delete()
            val index = readIndex()
            index.sessions.removeAll { it.sessionKey == sessionKey }
            if (index.lastSessionKey == sessionKey) index.lastSessionKey = index.sessions.firstOrNull()?.sessionKey
            writeIndex(index)
        } catch (e: Exception) {
            com.hive.utils.debug.DLog.w("AgentSessionStorage", "deleteSession failed: ${e.message}")
        }
    }

    fun saveLastSessionKey(sessionKey: String?) {
        if (sessionKey != null) MMKVTools.getInstance().putString(MMKV_LAST_SESSION_KEY, sessionKey)
        else MMKVTools.getInstance().putString(MMKV_LAST_SESSION_KEY, "")
    }

    fun getLastSessionKey(): String? {
        val s = MMKVTools.getInstance().getString(MMKV_LAST_SESSION_KEY, "")
        return s?.takeIf { it.isNotEmpty() }
    }

    /**
     * 将会话加入索引（新建会话时调用，仅写索引；实际消息由 saveCurrentSessionAsync 写入）。
     * 同步执行，保证目录和索引文件在发送消息后立即落盘。
     */
    fun ensureSessionInIndex(sessionKey: String, title: String) {
        try {
            if (!ensureSessionsDirExists()) {
                com.hive.utils.debug.DLog.w("AgentSessionStorage", "ensureSessionInIndex: dir not ready")
                return
            }
            val index = readIndex()
            if (index.sessions.any { it.sessionKey == sessionKey }) return
            val now = System.currentTimeMillis()
            index.sessions.add(0, SessionMeta(sessionKey, title, now, now))
            index.lastSessionKey = sessionKey
            writeIndex(index)
        } catch (e: Exception) {
            com.hive.utils.debug.DLog.w("AgentSessionStorage", "ensureSessionInIndex failed: ${e.message}", e)
        }
    }
}
