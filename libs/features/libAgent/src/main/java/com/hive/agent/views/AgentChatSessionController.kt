// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views

import com.hive.agent.storage.AgentSessionStorage
import com.hive.agent.storage.LoadedSession
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.MessageRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AgentChatSessionController(
    private val sessionStorage: AgentSessionStorage,
) {

    private var saveDebounceJob: Job? = null

    val currentConversationMessages = mutableListOf<ChatMessage>()

    var currentSessionKey: String? = null
        private set

    fun restoreLastSession(): LoadedSession? {
        val lastKey = sessionStorage.getLastSessionKey() ?: return null
        val loaded = sessionStorage.loadSession(lastKey) ?: return null
        applyLoadedSession(loaded)
        return loaded
    }

    fun appendUserMessage(userMessage: ChatMessage): List<ChatMessage> {
        currentConversationMessages.add(userMessage)
        return if (currentConversationMessages.size > 2) {
            currentConversationMessages.toList()
        } else {
            listOf(userMessage)
        }
    }

    fun resetConversation() {
        currentConversationMessages.clear()
        currentSessionKey = null
        sessionStorage.saveLastSessionKey(null)
    }

    fun applyLoadedSession(data: LoadedSession) {
        currentSessionKey = data.sessionKey
        sessionStorage.saveLastSessionKey(data.sessionKey)
        currentConversationMessages.clear()
        currentConversationMessages.addAll(data.messages)
    }

    fun loadSession(sessionKey: String): LoadedSession? {
        return sessionStorage.loadSession(sessionKey)
    }

    fun createSessionForFirstMessage(input: String, emptyTitle: String) {
        val firstInputForKey = input.ifEmpty { null }
        val key = sessionStorage.generateKeyFromFirstInput(firstInputForKey)
        val title = input.take(TITLE_MAX_LEN).ifEmpty { emptyTitle }
        sessionStorage.ensureSessionInIndex(key, title)
        currentSessionKey = key
        sessionStorage.saveLastSessionKey(key)
    }

    fun scheduleSave(
        scope: CoroutineScope,
        goal: AgentTaskGoal? = null,
        delayMs: Long,
        emptyTitle: String,
        commandsProvider: () -> List<String>?,
    ) {
        goal?.input?.messages?.let { messages ->
            currentConversationMessages.clear()
            currentConversationMessages.addAll(messages)
        }
        ensureSessionExists(goal, emptyTitle)
        currentSessionKey ?: return
        saveDebounceJob?.cancel()
        saveDebounceJob = scope.launch {
            if (delayMs > 0) delay(delayMs)
            saveDebounceJob = null
            persistCurrentSession(sync = false, emptyTitle = emptyTitle, commands = commandsProvider())
        }
    }

    fun flushSave(emptyTitle: String, commandsProvider: () -> List<String>?) {
        saveDebounceJob?.cancel()
        saveDebounceJob = null
        persistCurrentSession(sync = true, emptyTitle = emptyTitle, commands = commandsProvider())
    }

    private fun ensureSessionExists(goal: AgentTaskGoal?, emptyTitle: String) {
        if (currentSessionKey != null) return
        val messages = goal?.input?.messages ?: return
        if (messages.isEmpty()) return
        val firstUserContent = messages.firstOrNull { it.role == MessageRole.USER }?.content
        val key = sessionStorage.generateKeyFromFirstInput(firstUserContent)
        val title = messages.firstOrNull { it.role == MessageRole.USER }
            ?.content
            ?.take(TITLE_MAX_LEN)
            ?: emptyTitle
        sessionStorage.ensureSessionInIndex(key, title)
        currentSessionKey = key
        sessionStorage.saveLastSessionKey(key)
    }

    private fun persistCurrentSession(sync: Boolean, emptyTitle: String, commands: List<String>?) {
        val key = currentSessionKey ?: return
        val title = currentConversationMessages
            .firstOrNull { it.role == MessageRole.USER }
            ?.content
            ?.take(TITLE_MAX_LEN)
            ?.ifEmpty { null }
            ?: emptyTitle

        if (sync) {
            sessionStorage.saveCurrentSessionSync(
                key,
                title,
                currentConversationMessages.toList(),
                commands
            )
        } else {
            sessionStorage.saveCurrentSessionAsync(
                key,
                title,
                currentConversationMessages.toList(),
                commands
            )
        }
    }

    private companion object {
        private const val TITLE_MAX_LEN = 30
    }
}
