// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.hive.agent.R
import com.hive.agent.XAgent
import com.hive.agent.config.AIAgentConfig
import com.hive.agent.config.AgentChatUiConfig
import com.hive.agent.utils.AgentCheckHelper
import com.hive.agent.storage.AgentSessionStorage
import com.hive.agent.storage.LoadedSession
import com.hive.agent.views.chat.AgentChatEmptyStateView
import com.hive.agent.views.chat.AgentChatView
import com.hive.agent.views.chat.AgentToolDetailBottomSheet
import com.hive.agent.views.chat.ChatInputAsrProviderImpl
import com.hive.agent.views.provider.ActivityAgentSetting
import com.hive.agent.views.provider.AgentModelSettingsBottomSheet
import com.hive.agent.views.session.AgentSessionDrawerDialog
import com.hive.event.AgentEvent
import com.hive.event.AgentEventType
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.ErrorContext
import com.hive.plugin.agent.InferenceType
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.model.AgentError
import com.hive.plugin.agent.model.AgentInput
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.AttachmentType
import com.hive.plugin.agent.model.ChatAttachment
import com.hive.plugin.agent.model.ChatMessage
import com.hive.plugin.agent.model.ExecutionStatus
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.TaskPriority
import com.hive.plugin.provider.IScriptProvider
import com.hive.views.widgets.ChatInputContainer
import com.hive.views.fragment.PagerFragment
import com.hive.script.utils.ScriptPermissionManager
import com.hive.utils.GlobalApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class AgentChatFragment : PagerFragment() {

    companion object {
        private const val SAVE_DEBOUNCE_MS = 400L
    }

    private val xAgent = XAgent.getInstance()
    private var agentChatView: AgentChatView? = null
    private var chatInputContainer: ChatInputContainer? = null
    private var emptyStateView: AgentChatEmptyStateView? = null
    private var layoutSessionLoading: View? = null
    private var inputArea: View? = null
    private var chatInputBaseBottomMargin: Int = 0
    private var keyboardLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var lastKeyboardOverlap: Int = -1
    private val chatUiConfig: AgentChatUiConfig by lazy { AgentChatUiConfig.read() }

    // 选择的图片（用于展示和网络发送；选图入口已隐藏，保留附件发送兼容）
    private var selectedImageContentUri: Uri? = null
    private var selectedImageDataUrl: String? = null

    private val sessionStorage: AgentSessionStorage by lazy { AgentSessionStorage(GlobalApp.getContext()) }
    private val sessionController: AgentChatSessionController by lazy {
        AgentChatSessionController(sessionStorage)
    }
    private val taskBridge: AgentChatTaskBridge by lazy {
        AgentChatTaskBridge(xAgent)
    }

    override fun initView() {
        initializeViews()
        setupInputHandlers()
        setupAnimations()
        setupKeyboardAdjustment()

        // 尝试从 MMKV 恢复上次会话；若无则保持现有行为（欢迎消息 + 空对话）
        val loaded = sessionController.restoreLastSession()
        if (loaded != null && loaded.messages.isNotEmpty()) {
            applySessionToUi(loaded)
        } else {
            addWelcomeMessage()
        }

        // 通知父Fragment我们已经准备好了
        view?.post {
            EventBus.getDefault().post(AgentEvent(AgentEventType.AGENT_CHAT_FRAGMENT_READY))
        }
    }

    fun setupAgentObservers() {
        taskBridge.register(object : AgentChatTaskBridge.Callbacks {
            override fun lifecycleScope() =
                if (view != null) viewLifecycleOwner.lifecycleScope else this@AgentChatFragment.lifecycleScope

            override fun onTaskMessagesUpdated(goal: AgentTaskGoal) {
                agentChatView?.updateMessages(goal)
            }

            override fun onTaskMessagesStreamUpdated(goal: AgentTaskGoal) {
                agentChatView?.updateMessages(goal)
            }

            override fun onTaskStatusChanged(taskId: String, status: ExecutionStatus) {
                agentChatView?.updateTaskStatus(taskId, status)
                notifyToolbarActionsVisibility()
            }

            override fun onTaskMemoryCompressing(isCompressing: Boolean) {
                agentChatView?.setCompressingMemory(isCompressing)
            }

            override fun onTaskError(error: AgentError, context: ErrorContext) {
                // 402 余额不足等错误已通过 EventBus 统一处理，这里不做额外处理
            }

            override fun onPrepareFreshSessionForExternalTask() {
                prepareFreshSessionForExternalTask()
            }

            override fun onRequestSessionSave(goal: AgentTaskGoal?, delayMs: Long) {
                scheduleSessionSave(goal, delayMs)
            }
        })
    }

    private fun initializeViews() {
        agentChatView = view?.findViewById(R.id.agentChatView) ?: return
        chatInputContainer = view?.findViewById(R.id.chatInputContainer) ?: return
        emptyStateView = view?.findViewById(R.id.emptyStateView)
        layoutSessionLoading = view?.findViewById(R.id.layoutSessionLoading)
        inputArea = view?.findViewById(R.id.inputArea)
        chatInputBaseBottomMargin =
            (chatInputContainer?.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0

        // 配置并集控件：语音(左) + 文本 + 模型选择+发送(右)
        chatInputContainer?.setAgentStyle(true)
        chatInputContainer?.setShowImagePicker(false)
        chatInputContainer?.setShowModelSelector(true)
        chatInputContainer?.setShowVoiceInput(true)
        chatInputContainer?.setInputHint(getString(com.hive.i8n.R.string.agent_chat_input_hint))
        // 语音"发送"通过回调参数传递识别结果
        chatInputContainer?.setOnSendClickListener { text -> sendChatMessage(text) }
        chatInputContainer?.setOnModelSelectClickListener { onModelSelectClick() }
        updateModelSelectorState()
        chatInputContainer?.onRequestRecordPermission = { activity, onGranted ->
            ScriptPermissionManager.requestCommonPermission(
                activity,
                android.Manifest.permission.RECORD_AUDIO,
                onGranted,
                { com.hive.utils.debug.DLog.w("AgentChatFragment", "RECORD_AUDIO denied") }
            )
        }
        chatInputContainer?.setAsrProvider(ChatInputAsrProviderImpl(requireContext()))
        emptyStateView?.render(chatUiConfig.emptyState)
        emptyStateView?.onExampleClick = ::handleEmptyStateExampleClick
        agentChatView?.setOnVisibleMessageStateChangedListener(::updateEmptyStateVisibility)
        agentChatView?.setOnToolMessageClickListener { message ->
            AgentToolDetailBottomSheet.show(childFragmentManager, message)
        }
        notifyToolbarActionsVisibility()
    }

    /** 根据当前对话模型更新入口文案；未设置时显示「去设置」 */
    private fun updateModelSelectorState(
        selectedModel: ModelInfo? = xAgent.getAIServiceManager()?.getInferenceModel(InferenceType.TEXT)
    ) {
        val model = selectedModel
        val name = model?.displayName?.takeIf { it.isNotBlank() }
        if (name == null) {
            chatInputContainer?.setModelSelectorText(
                getString(com.hive.i8n.R.string.agent_settings_not_set),
                isUnset = true
            )
        } else {
            chatInputContainer?.setModelSelectorText(name.take(4), isUnset = false)
        }
    }

    private fun onModelSelectClick() {
        val manager = xAgent.getAIServiceManager()
        val hasReadyProvider = manager?.getEnabledProviders()?.any { it.isProviderReady() } == true
        if (!hasReadyProvider) {
            showElegantToast(getString(com.hive.i8n.R.string.agent_model_go_configure))
            ActivityAgentSetting.start(requireContext())
            return
        }
        AgentModelSettingsBottomSheet.show(childFragmentManager) {
            updateModelSelectorState()
        }
    }

    private fun syncRunningTaskState() {
        taskBridge.syncCurrentTaskId(xAgent.getTasksByState(
            listOf(ExecutionStatus.RUNNING, ExecutionStatus.PAUSED)
        )?.firstOrNull())
        notifyToolbarActionsVisibility()
    }

    private fun isTaskRunning(): Boolean = taskBridge.currentTaskId != null

    private fun notifyToolbarActionsVisibility() {
        EventBus.getDefault().post(
            AgentEvent(
                AgentEventType.AGENT_CHAT_TOOLBAR_ACTIONS_VISIBILITY,
                !isTaskRunning()
            )
        )
    }

    fun syncToolbarActions() {
        syncRunningTaskState()
    }

    private fun handleEmptyStateExampleClick(example: AgentChatUiConfig.Example) {
        val prompt = example.prompt.trim()
        if (prompt.isEmpty()) return
        chatInputContainer?.setText(prompt)
        if (example.autoSubmit) {
            sendChatMessage(prompt)
        } else {
            chatInputContainer?.getInputEdit()?.requestFocus()
            showKeyboard()
        }
    }

    private fun updateEmptyStateVisibility(hasVisibleMessages: Boolean) {
        emptyStateView?.visibility = if (hasVisibleMessages) View.GONE else View.VISIBLE
    }

    private var hasPromptedAgentEnv = false

    override fun onResume() {
        super.onResume()
        if (chatInputContainer != null) updateModelSelectorState()
        syncRunningTaskState()
        maybePromptAgentEnv()
    }

    /** Agent 服务异步初始化完成后，恢复冷启动前已保存的模型状态。 */
    fun onAgentServiceReady() {
        if (chatInputContainer == null) return
        updateModelSelectorState()
        maybePromptAgentEnv()
    }

    /** 进入对话页时，若未配置可用大模型（或无障碍），主动弹出设置提醒 */
    private fun maybePromptAgentEnv() {
        if (hasPromptedAgentEnv || view == null) return
        val manager = xAgent.getAIServiceManager() ?: return
        // 冷启动阶段 Provider 尚未注册不等于用户未配置，等待服务启动事件后再检查。
        if (manager.getProviderList().isEmpty()) return
        view?.post {
            if (!isAdded || view == null || hasPromptedAgentEnv) return@post
            try {
                if (AgentCheckHelper.checkAgentEnv()) return@post
                hasPromptedAgentEnv = true
                AgentCheckHelper.showAgentEnvDialog()
            } catch (_: Throwable) {
                // 环境检查失败时不打断页面
            }
        }
    }

    override fun onDestroyView() {
        flushSessionSave()
        taskBridge.unregister()
        clearKeyboardAdjustment()
        super.onDestroyView()
    }

    private fun setupInputHandlers() {
        val inputEdit = chatInputContainer?.getInputEdit()?:return
        inputEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendChatMessage(null)
                true
            } else {
                false
            }
        }
        inputEdit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showKeyboard()
                animateInputFocus(true)
            } else {
                animateInputFocus(false)
            }
        }
    }

    fun updateCurrentTask() {
        taskBridge.refreshCurrentTask(object : AgentChatTaskBridge.Callbacks {
            override fun lifecycleScope() =
                if (view != null) viewLifecycleOwner.lifecycleScope else this@AgentChatFragment.lifecycleScope

            override fun onTaskMessagesUpdated(goal: AgentTaskGoal) {
                agentChatView?.updateMessages(goal)
            }

            override fun onTaskMessagesStreamUpdated(goal: AgentTaskGoal) = Unit

            override fun onTaskStatusChanged(taskId: String, status: ExecutionStatus) = Unit

            override fun onTaskMemoryCompressing(isCompressing: Boolean) = Unit

            override fun onTaskError(error: AgentError, context: ErrorContext) = Unit

            override fun onPrepareFreshSessionForExternalTask() = Unit

            override fun onRequestSessionSave(goal: AgentTaskGoal?, delayMs: Long) {
                scheduleSessionSave(goal, delayMs)
            }
        })
        syncRunningTaskState()
    }

    private fun setupKeyboardAdjustment() {
        val rootView = requireView()
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            updateKeyboardAdjustment(calculateKeyboardOverlap(rootView))
        }
        keyboardLayoutListener = listener
        rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)
        rootView.post { listener.onGlobalLayout() }
    }

    private fun updateKeyboardAdjustment(overlap: Int) {
        if (overlap == lastKeyboardOverlap) return
        lastKeyboardOverlap = overlap
        applyInputAreaOffset(overlap)
        updateChatInputBottomMargin(if (overlap > 0) 0 else chatInputBaseBottomMargin)
    }

    private fun calculateKeyboardOverlap(rootView: View): Int {
        val visibleRect = Rect()
        rootView.getWindowVisibleDisplayFrame(visibleRect)
        val location = IntArray(2)
        rootView.getLocationOnScreen(location)
        val rootBottomOnScreen = location[1] + rootView.height
        return (rootBottomOnScreen - visibleRect.bottom).coerceAtLeast(0)
    }

    private fun applyInputAreaOffset(overlap: Int) {
        inputArea?.translationY = -overlap.toFloat()
    }

    private fun updateChatInputBottomMargin(bottomMargin: Int) {
        val params = chatInputContainer?.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        if (params.bottomMargin == bottomMargin) return
        params.bottomMargin = bottomMargin
        chatInputContainer?.layoutParams = params
    }

    private fun clearKeyboardAdjustment() {
        keyboardLayoutListener?.let { listener ->
            view?.viewTreeObserver?.removeOnGlobalLayoutListener(listener)
        }
        keyboardLayoutListener = null
        inputArea?.translationY = 0f
        lastKeyboardOverlap = -1
        updateChatInputBottomMargin(chatInputBaseBottomMargin)
    }

    private fun setupAnimations() {
        // 初始进入动画
        view?.let { rootView ->
            rootView.alpha = 0f
            rootView.translationY = 50f
            rootView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(OvershootInterpolator(0.8f))
                .start()
        }

    }

    private fun animateInputFocus(hasFocus: Boolean) {
        val scale = if (hasFocus) 1.01f else 1f
        val elevation = if (hasFocus) 12f else 8f
        chatInputContainer?.getInputEdit()?.animate()
            ?.scaleX(scale)
            ?.scaleY(scale)
            ?.setDuration(200)
            ?.start()
        chatInputContainer?.animate()
            ?.translationZ(elevation)
            ?.setDuration(200)
            ?.start()
    }

    private fun addWelcomeMessage() {
        agentChatView?.addMessage(
            ChatMessage(
                role = MessageRole.SYSTEM,
                content = getString(com.hive.i8n.R.string.agent_welcome_message)
            )
        )
    }

    /**
     * Start a new conversation - exposed as public to allow parent fragment to call
     */
    fun startNewConversation() {
        flushSessionSave()
        agentChatView?.animate()
            ?.alpha(0.5f)
            ?.setDuration(200)
            ?.withEndAction {
                resetConversationState()
                addWelcomeMessage()
                agentChatView?.animate()
                    ?.alpha(1f)
                    ?.setDuration(300)
                    ?.start()
                hideKeyboard()
                showElegantToast(getString(com.hive.i8n.R.string.agent_new_conversation))
            }
            ?.start()
    }

    private fun sendChatMessage(inputText: String?) {
        if (!AgentCheckHelper.checkAgentEnv()) {
            AgentCheckHelper.showAgentEnvDialog()
            return
        }

        if (selectedImageContentUri != null) {
            val visionModel = xAgent.getAIServiceManager()?.getInferenceModel(InferenceType.IMAGE)
            val visionEnabled = AIAgentConfig.VisionConfig.isVisionRecognitionEnabled()
            if (visionModel == null || !visionEnabled) {
                clearAttachmentSelection()
                showElegantToast(
                    getString(
                        if (!visionEnabled) {
                            com.hive.i8n.R.string.agent_vision_recognition_disabled
                        } else {
                            com.hive.i8n.R.string.agent_multimodal_required
                        }
                    )
                )
                return
            }
        }

        val input = inputText?.trim() ?: chatInputContainer?.getInputEdit()?.text.toString().trim()
        if (input.isEmpty() && selectedImageContentUri == null) {
            showElegantToast(getString(com.hive.i8n.R.string.agent_input_required))
            return
        }

        val userMessage = buildUserMessage(input)
        agentChatView?.addMessage(userMessage)

        val conversationInput = sessionController.appendUserMessage(userMessage)

        // 首次发送：生成 key、写索引、同步写会话（保证落盘）
        if (sessionController.currentSessionKey == null) {
            sessionController.createSessionForFirstMessage(
                input,
                getString(com.hive.i8n.R.string.agent_session_empty_title)
            )
            persistCurrentSession(sync = true)
        } else {
            scheduleSessionSave(null)
        }

        chatInputContainer?.getInputEdit()?.setText("")

        try {
            val taskId = "agent_${System.currentTimeMillis()}"
            taskBridge.markPendingChatLaunch(taskId)
            taskBridge.syncCurrentTaskId(taskId)
            notifyToolbarActionsVisibility()
            val networkMessages = buildNetworkMessages(conversationInput, userMessage)

            val taskGoal = AgentTaskGoal(
                id = taskId,
                userInput = input,
                priority = TaskPriority.NORMAL,
                requiredCapabilities = listOf(AIAgentConfig.BaseConfig.AIAssistantToolName),
                input = AgentInput(networkMessages)
            )

            xAgent.executeTask(taskGoal, null)

        } catch (e: Exception) {
            taskBridge.syncCurrentTaskId(null)
            notifyToolbarActionsVisibility()
            // 添加错误消息
            agentChatView?.addMessage(
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = getString(com.hive.i8n.R.string.agent_error_occurred, e.message)
                )
            )
        }

        clearAttachmentSelection()
    }

    private fun buildUserMessage(input: String): ChatMessage {
        val attachments = selectedImageContentUri?.let { uri ->
            listOf(
                ChatAttachment(
                    type = AttachmentType.IMAGE,
                    url = uri.toString(),
                    mimeType = context?.contentResolver?.getType(uri)
                )
            )
        }.orEmpty()

        return ChatMessage(
            role = MessageRole.USER,
            content = when {
                attachments.isEmpty() -> input
                input.isNotEmpty() -> input
                else -> null
            },
        ).also { it.attachments.addAll(attachments) }
    }

    private fun buildNetworkMessages(
        conversationInput: List<ChatMessage>,
        userMessage: ChatMessage
    ): List<ChatMessage> {
        val imageDataUrl = selectedImageDataUrl ?: return conversationInput
        return conversationInput.map { message ->
            if (message !== userMessage) {
                message
            } else {
                message.copy(
                    attachments = message.attachments.map { attachment ->
                        if (attachment.type == AttachmentType.IMAGE) {
                            attachment.copy(url = imageDataUrl)
                        } else {
                            attachment
                        }
                    }.toMutableList()
                )
            }
        }
    }

    /**
     * 外部入口触发新任务时：先保存当前会话，再切到空会话上下文。
     * 后续由任务消息回调自动创建并写入新的 session。
     */
    private fun prepareFreshSessionForExternalTask() {
        flushSessionSave()
        resetConversationState()
    }

    private fun clearAttachmentSelection() {
        selectedImageContentUri = null
        selectedImageDataUrl = null
        chatInputContainer?.hideAttachmentPreview()
    }

    /** 统一保存入口：有更新即刷新记录，流式防抖，非流式可立即保存 */
    private fun scheduleSessionSave(goal: AgentTaskGoal? = null, delayMs: Long = SAVE_DEBOUNCE_MS) {
        val scope = if (view != null) viewLifecycleOwner.lifecycleScope else lifecycleScope
        sessionController.scheduleSave(
            scope = scope,
            goal = goal,
            delayMs = delayMs,
            emptyTitle = getString(com.hive.i8n.R.string.agent_session_empty_title),
            commandsProvider = ::getLastRecordedCommands
        )
    }

    /** 立即落盘（打开历史、切换会话、新建对话前） */
    private fun flushSessionSave() {
        sessionController.flushSave(
            emptyTitle = getString(com.hive.i8n.R.string.agent_session_empty_title),
            commandsProvider = ::getLastRecordedCommands
        )
    }

    private fun persistCurrentSession(sync: Boolean) {
        if (sync) {
            flushSessionSave()
        } else {
            scheduleSessionSave()
        }
    }

    private fun getLastRecordedCommands() =
        (ComponentManager.getInstance().getProvider(IScriptProvider::class.java) as? IScriptProvider)
            ?.getLastRecordedCommands()

    /** 显示/隐藏切换会话时的 loading 遮罩 */
    private fun setSessionLoading(loading: Boolean) {
        layoutSessionLoading?.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            emptyStateView?.visibility = View.GONE
        } else {
            updateEmptyStateVisibility(agentChatView?.hasVisibleMessages() == true)
        }
    }

    /**
     * 将已加载的会话数据应用到 UI（主线程调用）。
     * 用于初始化恢复、以及协程加载完成后的刷新。
     */
    private fun applySessionToUi(data: LoadedSession) {
        sessionController.applyLoadedSession(data)
        agentChatView?.setMessages(data.messages)
    }

    /**
     * 切换会话：先显示 loading，在 IO 协程中加载，加载完成后在主线程刷新 UI 并关闭 loading。
     * 由历史记录 BottomSheet 选择项后调用（BottomSheet 已 dismiss）。
     */
    fun switchToSessionAsync(sessionKey: String) {
        flushSessionSave()
        if (view == null) return
        viewLifecycleOwner.lifecycleScope.launch {
            setSessionLoading(true)
            val data = withContext(Dispatchers.IO) {
                sessionController.loadSession(sessionKey)
            }
            withContext(Dispatchers.Main) {
                if (data != null && isAdded) {
                    applySessionToUi(data)
                }
                setSessionLoading(false)
            }
        }
    }

    private fun showKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(chatInputContainer?.getInputEdit(), InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(chatInputContainer?.getInputEdit()?.windowToken, 0)
    }

    private fun showElegantToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun resetConversationState() {
        sessionController.resetConversation()
        agentChatView?.clearMessages()
    }

    fun showSessionHistory() {
        flushSessionSave()
        val drawer = AgentSessionDrawerDialog()
        drawer.apply {
            isAgentRunning = taskBridge.currentTaskId != null
            currentSessionKey = this@AgentChatFragment.sessionController.currentSessionKey
            onSessionSelected = { sessionKey ->
                if (sessionKey == null) {
                    startNewConversation()
                } else {
                    // 先关闭抽屉，再在协程中 loading → 加载 → 主线程刷新
                    switchToSessionAsync(sessionKey)
                }
            }
            onSessionDeleted = { sessionKey ->
                if (sessionKey == this@AgentChatFragment.sessionController.currentSessionKey) {
                    startNewConversation()
                }
            }
            onConvertToWorkflow = { sessionKey ->
                val loaded = sessionStorage.loadSession(sessionKey)
                if (loaded != null) {
                    // 优先从录制器 commands 获取（快速）
                    val recordedCommands = loaded.commands

                    // 如果录制 commands 不完整或为空，从 messages 提取完整历史
                    val commands = if (recordedCommands.isNotEmpty()) {
                        recordedCommands
                    } else {
                        com.hive.agent.command.AgentCommandExtractor.extractCommandsFromSession(sessionKey)
                    }

                    if (commands.isNotEmpty()) {
                        val scriptProvider = ComponentManager.getInstance()
                            .getProvider(IScriptProvider::class.java) as? IScriptProvider
                        scriptProvider?.saveCommandsToWorkflow(
                            commands,
                            loaded.title.ifEmpty { null }
                        ) { success ->
                            if (success) {
                                Toast.makeText(
                                    context,
                                    getString(com.hive.i8n.R.string.agent_session_convert_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                                drawer.dismiss()
                                navigateToWorkflowTab()
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            getString(com.hive.i8n.R.string.agent_session_no_commands),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        getString(com.hive.i8n.R.string.agent_session_load_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        drawer.show(parentFragmentManager, AgentSessionDrawerDialog::class.java.simpleName)
    }

    /**
     * Retry last user message
     */
    private fun retryLastMessage() {
        // Get last user message and resend
        val lastUserMessage = sessionController.currentConversationMessages.lastOrNull {
            it.role == MessageRole.USER
        }

        if (lastUserMessage != null) {
            sendChatMessage(lastUserMessage.content ?: "")
        }
    }

    private fun navigateToWorkflowTab() {
        EventBus.getDefault().post(
            AgentEvent(
                AgentEventType.AGENT_NAVIGATE_TO_RESOURCE_TYPE,
                "workflow"
            )
        )
    }

    override fun getLayoutId() = R.layout.fragment_agent_chat

} 
