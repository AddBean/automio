// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.agent

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.hive.extension.visibleOrGone
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.IAgentStateObserver
import com.hive.plugin.agent.IAgentTaskObserver
import com.hive.plugin.agent.ISkillStateObserver
import com.hive.plugin.agent.model.AgentInput
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.ExecutionStatus
import com.hive.plugin.agent.model.MessageRole
import com.hive.plugin.agent.model.SkillResult
import com.hive.plugin.agent.model.TaskResult
import com.hive.plugin.provider.IAgentProvider
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.GlobalApp
import com.hive.utils.extends.dp
import com.hive.utils.extends.isLandscape
import com.hive.utils.extends.string
import com.hive.utils.extends.takeLastWords
import com.hive.utils.extends.toDimension
import com.hive.utils.extends.toDimensionInt
import com.hive.utils.extends.toDrawable
import com.hive.utils.system.ClipboardUtil
import com.hive.utils.system.SystemProperty
import com.hive.views.list_view.ListRecyclerView
import com.hive.views.view_manager.HiveViewManagerOfAccessibility
import com.hive.views.widgets.CommonToast

class ScriptAgentTopView(context: Context) : BaseScriptDialog(context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: ScriptAgentTopView? = null

        fun show() {
            if (instance == null) {
                instance = ScriptAgentTopView(ScriptProvider.getViewContext())
            }
            instance?.show()
        }

        fun dismiss() {
            val dismissing = instance ?: return
            dismissing.dismiss {
                if (instance === dismissing) instance = null
            }
        }

        /**
         * 仅隐藏 View（不销毁实例），用于截屏前隐藏浮窗。
         * 与 dismiss() 不同，不会置空 instance，避免 pending Handler 回调 NPE。
         */
        fun hideForCapture() {
            instance?.post {
                instance?.hideMotionForCapture()
                instance?.setTouchPassthrough(true)
            }
        }

        /**
         * 恢复 View 可见（与 hideForCapture 配对使用）。
         */
        fun showForCapture() {
            instance?.post {
                instance?.showMotionAfterCapture()
                instance?.setTouchPassthrough(false)
            }
        }

        /** 与 TopView 浮窗一致的屏幕顶边 Y，供顶部 Tips 弹窗对齐并遮住 TopView。 */
        fun getWindowTopY(): Int =
            if (GlobalApp.isLandscape()) 8.dp else SystemProperty.getStatusBarHeight(GlobalApp.getContext())
    }

    // UI组件
    private lateinit var ivTaskIcon: ImageView
    private lateinit var tvTaskTitle: TextView
    private lateinit var vStatusIndicator: View
    private lateinit var tvStatusText: TextView
    private lateinit var btnStop: ImageButton
    private lateinit var btnPauseResume: ImageButton
    private lateinit var rvTimeline: ListRecyclerView
    private lateinit var btnCollapse: ImageButton
    private lateinit var tvTaskInfo: TextView


    // 收起状态相关组件
    private lateinit var llCollapsedStatus: LinearLayout

    //    private lateinit var ivCollapsedIcon: ImageView
    private lateinit var tvCollapsedStatus: TextView
    private lateinit var llExpandedContent: View
    private var motionController: AgentTopViewMotionController? = null
    private var viewsInitialized = false
    private var wasDetached = false

    enum class AgentStatus {
        THINKING, TOOL_CALLING, COMPLETED, FAILED, PAUSED, EXECUTING, COMPRESSING_MEMORY
    }

    private var currentStatus = AgentStatus.THINKING
    private var isPaused = false
    private var currentTaskGoal: AgentTaskGoal? = null

    // 收起状态相关变量
    private var isCollapsed = false

    // 当前任务信息的完整原文本，用于长按复制
    private var currentTaskInfoRawText: String = ""

    private val collapseManager = CollapseManager()

    private val statusManager = StatusManager()

    private var layoutParamsNotFull = WindowManager.LayoutParams().also { lp ->
        lp.width = getWidthByOrientation()
        lp.height = getHeightByOrientation()
        lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        lp.flags =
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        lp.format = PixelFormat.RGBA_8888
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.y =
            if (GlobalApp.isLandscape()) 8.dp else SystemProperty.getStatusBarHeight(GlobalApp.getContext())
    }

    /** 是否透出触摸事件到底层（FLAG_NOT_TOUCH_MODAL） */
    fun setTouchPassthrough(enabled: Boolean) {
        val newFlags = if (enabled) {
            layoutParamsNotFull.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } else {
            layoutParamsNotFull.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
        }
        if (newFlags != layoutParamsNotFull.flags) {
            layoutParamsNotFull.flags = newFlags
            getViewManager()?.updateLayoutParams(layoutParamsNotFull)
        }
    }

    interface OnAgentControlListener {
        fun onStop()
        fun onPause()
        fun onResume()
    }

    private var controlListener: OnAgentControlListener? = null

    // Agent Provider
    private val agentProvider: IAgentProvider?
        get() {
            return ComponentManager.getInstance()
                .getProvider(IAgentProvider::class.java) as? IAgentProvider
        }
    private var isAgentProviderInitialized = false

    // UI线程Handler
    private var uiHandler: Handler? = null

    // Agent状态观察者
    private val agentStateObserver = object : IAgentStateObserver {
        override fun onAgentExecuteStart(taskId: String) {
            uiHandler?.post {
                onAgentExecuteStartInternal(taskId)
            }
        }

        override fun onAgentExecuteEnd(taskId: String, taskResult: TaskResult?) {
            uiHandler?.post {
                onAgentExecuteEndInternal(taskId)
            }
        }

        override fun onAgentStateChanged(taskId: String, status: ExecutionStatus) {
            uiHandler?.post {
                onAgentStateChangedInternal(taskId, status)
            }
        }
    }

    private val commandTaskObserver = object : ScriptInterpreterObserver.CommandExecuteObserver {

        override fun onCommandExecuteBefore(cmd: ScriptCommand) {
            super.onCommandExecuteBefore(cmd)
            // 命令执行前：关闭透传
            ScriptHelper.blockUntilViewReady(this@ScriptAgentTopView) {
                setTouchPassthrough(false)
                this@ScriptAgentTopView.visibleOrGone(true)
            }
        }

        override fun onCommandExecuteAfter(cmd: ScriptCommand) {
            super.onCommandExecuteAfter(cmd)
            // 命令执行后：开启透传，让空白区域触摸传到底部按钮
            ScriptHelper.blockUntilViewReady(this@ScriptAgentTopView) {
                if (instance != null) {
                    setTouchPassthrough(true)
                    this@ScriptAgentTopView.visibleOrGone(true)
                }
            }
        }
    }


    private val skillStateObserver = object : ISkillStateObserver {
        override fun onSkillExecuteStart(taskId: String) {
            uiHandler?.post {
                onSkillExecuteStartInternal(taskId)
            }
        }

        override fun onSkillExecuteEnd(taskId: String, result: SkillResult?) {
            uiHandler?.post {
                onSkillExecuteEndInternal(taskId)
            }
        }
    }


    // Agent任务观察者
    private val agentTaskObserver = object : IAgentTaskObserver {
        override fun onTaskInfoUpdated(message: String) {
            uiHandler?.post {
                onTaskInfoUpdatedInternal(message)
            }
        }

        override fun onMemoryCompressing(taskId: String, isCompressing: Boolean) {
            uiHandler?.post {
                if (taskId == currentTaskGoal?.id) {
                    if (isCompressing) {
                        updateStatus(AgentStatus.COMPRESSING_MEMORY)
                    } else {
                        updateStatus(AgentStatus.THINKING)
                    }
                }
            }
        }

        override fun onTaskMessageUpdated(goal: AgentTaskGoal) {
            uiHandler?.post {
                onTaskMessageUpdatedInternal(goal)
            }
        }

        override fun onTaskMessageStreamUpdated(goal: AgentTaskGoal) {
            uiHandler?.post {
                currentTaskGoal = goal
                updateCurrentTaskInfo()
            }
        }
    }

    override fun initWindow() {
        uiHandler = Handler(Looper.getMainLooper())
        uiHandler?.post {
            this.let {
                ivTaskIcon = it.findViewById(R.id.iv_task_icon)
                tvTaskTitle = it.findViewById(R.id.tv_task_title)
                vStatusIndicator = it.findViewById(R.id.v_status_indicator)
                tvStatusText = it.findViewById(R.id.tv_status_text)
                btnStop = it.findViewById(R.id.btn_stop)
                btnPauseResume = it.findViewById(R.id.btn_pause_resume)
                rvTimeline = it.findViewById(R.id.rv_timeline)
                btnCollapse = it.findViewById(R.id.btn_collapse)
                tvTaskInfo = it.findViewById(R.id.tv_task_info)

                // 收起状态组件
                llCollapsedStatus = it.findViewById(R.id.ll_collapsed_status)
//                ivCollapsedIcon = it.findViewById(R.id.iv_collapsed_icon)
                tvCollapsedStatus = it.findViewById(R.id.tv_collapsed_status)
                tvCollapsedStatus = it.findViewById(R.id.tv_collapsed_status)
                llExpandedContent = it.findViewById(R.id.ll_expanded_content)

                setupTimeline()
                setupListeners()
                setupCollapseListeners()
                setupCopyListeners()
                updateStatus(AgentStatus.THINKING)
                setupAgentProvider()

                viewsInitialized = true
                initMotionController()

                // 启动自动收起定时器
                collapseManager.startAutoCollapseTimer()

                // 初始化收起按钮图标
                btnCollapse.setImageResource(R.drawable.ic_collapse_arrow)

                // 关闭 BaseScriptDialog 设置的 isClickable，让透明区域自然透传事件
                findViewById<FrameLayout>(R.id.layoutRoot)?.getChildAt(0)?.isClickable = false
            }
        }
    }

    override fun getWindowLayoutId() = R.layout.script_agent_view

    override fun getLayoutId() = R.layout.script_agent_view_main

    override fun isTouchOutsideDismissed(): Boolean = false

    override fun getMarginParams() =
        arrayOf(
            0,
            if (getViewManager() !is HiveViewManagerOfAccessibility) SystemProperty.getStatusBarHeight(
                GlobalApp.getContext()
            ) else 0,
            0,
            0
        )

    override fun getHeightByOrientation(): Int {
        return FrameLayout.LayoutParams.WRAP_CONTENT
    }

    override fun getWidthByOrientation(): Int {
        return FrameLayout.LayoutParams.WRAP_CONTENT
    }

    override fun getBgColor() = android.graphics.Color.TRANSPARENT

    override fun getBgView(): android.view.View? = null

    override fun enableFadeAnimation() = false

    override fun enableUpDownAnimation() = false

    override fun runCustomShowAnimation(onStart: () -> Unit, onEnd: () -> Unit): Boolean {
        val controller = motionController
        if (controller != null) {
            controller.enter(onStart, onEnd)
        } else {
            // Never fall back to BaseScriptDialog's opposite-direction legacy translation.
            onStart()
            onEnd()
        }
        return true
    }

    override fun runCustomDismissAnimation(onEnd: () -> Unit): Boolean {
        val controller = motionController
        if (controller != null) controller.exit(onEnd) else onEnd()
        return true
    }

    override fun canInterruptDismissWithShow(): Boolean = true

    private fun hideMotionForCapture() {
        motionController?.snapHiddenForCapture()
        isCollapsed = motionController?.isTargetCollapsed ?: isCollapsed
    }

    private fun showMotionAfterCapture() {
        motionController?.snapVisibleAfterCapture()
    }

    private fun initMotionController() {
        if (!viewsInitialized || motionController != null) return
        val motionSurface = findViewById<View>(R.id.agent_motion_surface)
        motionController = AgentTopViewMotionController(
            host = this,
            surface = motionSurface,
            collapsed = llCollapsedStatus,
            expanded = llExpandedContent
        ) { width, height ->
            if (layoutParamsNotFull.width != width || layoutParamsNotFull.height != height) {
                layoutParamsNotFull.width = width
                layoutParamsNotFull.height = height
                getViewManager()?.updateLayoutParams(layoutParamsNotFull)
            }
        }.also { controller ->
            controller.initialize(isCollapsed)
        }
    }

    private fun refreshMotionSize() {
        post { motionController?.refreshCurrentSize() }
    }

    private fun setupTimeline() {
        // 设置横向布局管理器
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvTimeline.layoutManager = layoutManager

        // 设置事件工厂
        rvTimeline.setItemViewFactory(TimelineEventFactory(context))

        // 禁用拖拽功能
        rvTimeline.setEnableDrag(false)

        // 设置内边距，确保时间线居中
        rvTimeline.setPadding(8, 0, 8, 0)
        rvTimeline.clipToPadding = false

        // 确保RecyclerView能正确显示
        rvTimeline.setHasFixedSize(false)
    }

    private fun setupListeners() {
        btnStop.setOnClickListener {
            animateButtonClick(btnStop) {
                try {
                    currentTaskGoal?.id?.let { taskId ->
                        agentProvider?.stopTask(taskId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                controlListener?.onStop()
            }
        }
        btnPauseResume.setOnClickListener {
            animateButtonClick(btnPauseResume) {
                try {
                    currentTaskGoal?.id?.let { taskId ->
                        if (isPaused) {
                            agentProvider?.resumeTask(taskId)
                            controlListener?.onResume()
                            setPaused(false)
                        } else {
                            agentProvider?.pauseTask(taskId)
                            controlListener?.onPause()
                            setPaused(true)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setupCopyListeners() {
        val onLongCopy = View.OnLongClickListener {
            if (currentTaskInfoRawText.isNotEmpty()) {
                ClipboardUtil.getInstance(context)
                    .copyText("agent_task_info", currentTaskInfoRawText)
                CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.agent_message_copied))
            }
            true
        }
        tvTaskInfo.setOnLongClickListener(onLongCopy)
        tvCollapsedStatus.setOnLongClickListener(onLongCopy)
    }

    private fun setupCollapseListeners() {
        // 点击收起状态小条展开
        llCollapsedStatus.setOnClickListener {
            collapseManager.expand()
        }

        // 点击文字区域也能展开（TextView 会消费点击，需单独设置）
        tvCollapsedStatus.setOnClickListener {
            collapseManager.expand()
        }

        // 点击展开内容区域重置自动收起定时器
        llExpandedContent.setOnClickListener {
            collapseManager.resetAutoCollapseTimer()
        }

        // 点击收起按钮
        btnCollapse.setOnClickListener {
            animateButtonClick(btnCollapse) {
                collapseManager.toggleCollapse()
            }
        }
    }

    private fun animateButtonClick(button: ImageButton, action: () -> Unit) {
        // 简单的缩放动画
        val scaleDown = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.9f)
        val scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.9f)
        val scaleUp = ObjectAnimator.ofFloat(button, "scaleX", 0.9f, 1f)
        val scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 0.9f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.play(scaleDown).with(scaleDownY)
        animatorSet.play(scaleUp).with(scaleUpY).after(scaleDown)
        animatorSet.duration = 150
        animatorSet.interpolator = AccelerateDecelerateInterpolator()

        animatorSet.start()

        // 延迟执行动作
        uiHandler?.postDelayed({
            action()
        }, 75)
    }

    private fun setupAgentProvider() {
        if (isAgentProviderInitialized) return

        agentProvider?.let { provider ->
            provider.registerAgentStateObserver(agentStateObserver)
            provider.registerAgentTaskObserver(agentTaskObserver)
            provider.registerSkillStateObserver(skillStateObserver)
            provider.registerSkillTaskObserver(agentTaskObserver)
            provider.currentAgentGoal?.let {
                uiHandler?.post {
                    onTaskMessageUpdatedInternal(it)
                }
            }

            isAgentProviderInitialized = true
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ScriptInterpreterObserver.registerCommandObserver(commandTaskObserver)
        if (wasDetached) {
            wasDetached = false
            uiHandler = Handler(Looper.getMainLooper())
            post {
                if (!isAttachedToWindow) return@post
                initMotionController()
                setupAgentProvider()
                collapseManager.startAutoCollapseTimer()
            }
        }
    }

    override fun onDetachedFromWindow() {
        isCollapsed = motionController?.isTargetCollapsed ?: isCollapsed
        motionController?.dispose()
        motionController = null
        wasDetached = true
        ScriptInterpreterObserver.unRegisterCommandObserver(commandTaskObserver)
        try {
            // 清理Handler
            uiHandler?.removeCallbacksAndMessages(null)
            uiHandler = null

            // 清理收起状态管理器
            collapseManager.cleanup()

            agentProvider?.let { provider ->
                provider.unregisterAgentStateObserver(agentStateObserver)
                provider.unregisterAgentTaskObserver(agentTaskObserver)
                provider.unregisterSkillStateObserver(skillStateObserver)
                provider.unregisterSkillTaskObserver(agentTaskObserver)
            }
            isAgentProviderInitialized = false

        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDetachedFromWindow()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        post {
            motionController?.cancelAndSnapToCurrentTarget()
            isCollapsed = motionController?.isTargetCollapsed ?: isCollapsed
        }
    }

    fun setOnAgentControlListener(listener: OnAgentControlListener) {
        this.controlListener = listener
    }

    private fun updateTaskInfo(title: String) {
        tvTaskTitle.text =
            if (!title.isEmpty()) title else com.hive.i8n.R.string.script_agent_skill_running.string()
    }

    private fun updateStatus(status: AgentStatus) {
        if (currentStatus == status) return

        currentStatus = status
        statusManager.updateStatus(status)
        refreshMotionSize()

        // 重置自动收起定时器
        collapseManager.resetAutoCollapseTimer()
    }

    private fun setPaused(paused: Boolean) {
        isPaused = paused

        // 按钮图标切换
        btnPauseResume.setImageResource(
            if (paused) R.drawable.ic_agent_play else R.drawable.ic_agent_pause
        )

        updateStatus(if (paused) AgentStatus.PAUSED else currentStatus)
    }

    private fun setButtonEnabled(enabled: Boolean) {
        btnStop.isEnabled = enabled
        btnPauseResume.isEnabled = enabled

        // 按钮启用/禁用
        val alpha = if (enabled) 1f else 0.5f
        btnStop.alpha = alpha
        btnPauseResume.alpha = alpha
    }

    private fun resetViewForNewTask(taskId: String, title: String? = null) {
        currentTaskGoal = AgentTaskGoal(
            id = taskId,
            userInput = title ?: "",
            input = AgentInput(mutableListOf())
        )
        currentTaskInfoRawText = ""
        rvTimeline.submitDataSets(emptyList<Any>())
        updateTaskInfo(title ?: "")
        tvTaskInfo.text = GlobalApp.getString(com.hive.i8n.R.string.script_agent_thinking)
        tvCollapsedStatus.text = GlobalApp.getString(com.hive.i8n.R.string.script_agent_thinking)
        setPaused(false)
        refreshMotionSize()
    }

    // Agent执行开始处理
    private fun onAgentExecuteStartInternal(taskId: String) {
        if (currentTaskGoal?.id != taskId) {
            resetViewForNewTask(taskId)
        }
        updateStatus(AgentStatus.THINKING)
        setButtonEnabled(true)
    }

    // Agent执行结束处理
    private fun onAgentExecuteEndInternal(taskId: String) {
        if (taskId == currentTaskGoal?.id) {
            updateStatus(AgentStatus.COMPLETED)
            setButtonEnabled(false)
        }
    }

    // Skill执行开始处理
    private fun onSkillExecuteStartInternal(taskId: String) {
        if (currentTaskGoal == null) {
            resetViewForNewTask(
                taskId,
                GlobalApp.getString(com.hive.i8n.R.string.script_agent_skill_running)
            )
        }
        updateStatus(AgentStatus.THINKING)
        setButtonEnabled(true)
    }

    // Skill执行结束处理（taskId 恒为 skill-xxx，currentTaskGoal 可能来自 skill 或 agent）
    private fun onSkillExecuteEndInternal(taskId: String) {
        if (taskId == currentTaskGoal?.id || taskId.startsWith("skill-")) {
            updateStatus(AgentStatus.COMPLETED)
            setButtonEnabled(false)
        }
    }

    // Agent状态变化处理
    private fun onAgentStateChangedInternal(taskId: String, status: ExecutionStatus) {
        if (taskId == currentTaskGoal?.id) {
            when (status) {
                ExecutionStatus.RUNNING -> updateStatus(AgentStatus.THINKING)
                ExecutionStatus.PAUSED -> {
                    setPaused(true)
                    updateStatus(AgentStatus.PAUSED)
                }

                ExecutionStatus.SUCCESS -> updateStatus(AgentStatus.COMPLETED)
                ExecutionStatus.FAILED -> updateStatus(AgentStatus.FAILED)
                ExecutionStatus.TIMEOUT -> updateStatus(AgentStatus.FAILED)
                ExecutionStatus.STOPPED -> {
                    updateStatus(AgentStatus.FAILED)
                    setButtonEnabled(false)
                }

                else -> {}
            }
        }
    }

    // 任务信息更新处理
    private fun onTaskInfoUpdatedInternal(message: String) {
        // 可以在这里处理任务信息更新
        // 比如更新任务描述等
    }

    // 任务消息更新处理
    private fun onTaskMessageUpdatedInternal(goal: AgentTaskGoal) {
        currentTaskGoal = goal
        // 更新任务信息
        updateTaskInfo(goal.userInput)
        uiHandler?.post {
            // 处理聊天消息
            val chatInput = goal.input
            chatInput?.messages?.toList()?.filter { it.role != MessageRole.SYSTEM }
                ?.let { messages ->
                    val timelineData = messages.mapIndexed { index, message ->
                        TimelineEventView.createFromMessage(message, index, messages.size)
                    }
                    rvTimeline.submitDataSets(timelineData)
                    rvTimeline.postDelayed({
                        rvTimeline.scrollToPosition(messages.size - 1)
                        motionController?.refreshCurrentSize()
                    }, 100)
                }
            updateCurrentTaskInfo()

        }
    }


    private fun updateCurrentTaskInfo() {
        // 压缩记忆中时，展开态和收起态都展示该信息
        if (currentStatus == AgentStatus.COMPRESSING_MEMORY) {
            val compressingText =
                GlobalApp.getString(com.hive.i8n.R.string.script_top_status_compressing_memory)
            tvTaskInfo.text = compressingText
            tvCollapsedStatus.text = compressingText
            refreshMotionSize()
            return
        }
        val chatInput = currentTaskGoal?.input ?: return
        val lastMsgBean = chatInput.messages.lastOrNull { it.role == MessageRole.ASSISTANT }
        val lastMsgStr = (lastMsgBean?.reasoningContent ?: "") + (lastMsgBean?.content ?: "")
        currentTaskInfoRawText = lastMsgStr.trim()
        val displayText =
            if (TextUtils.isEmpty(lastMsgStr)) GlobalApp.getString(com.hive.i8n.R.string.script_agent_thinking) else lastMsgStr.takeLastWords(
                100
            )
        tvTaskInfo.text = displayText
        tvCollapsedStatus.text = displayText
        refreshMotionSize()
    }

    // 状态管理器
    private inner class StatusManager {
        fun updateStatus(status: AgentStatus) {
            updateStatusColor(status)
            updateStatusText(status)
            updateCollapsedStatus(status)
            if (status == AgentStatus.COMPRESSING_MEMORY) {
                updateCurrentTaskInfo()
            }
        }

        private fun updateStatusColor(status: AgentStatus) {
            val colorRes = when (status) {
                AgentStatus.THINKING -> com.hive.i8n.R.color.color_orange
                AgentStatus.TOOL_CALLING -> com.hive.i8n.R.color.colorAccent
                AgentStatus.EXECUTING -> com.hive.i8n.R.color.colorPurple
                AgentStatus.COMPLETED -> com.hive.i8n.R.color.colorTextGreen
                AgentStatus.FAILED -> com.hive.i8n.R.color.colorRed
                AgentStatus.PAUSED -> com.hive.i8n.R.color.colorTextSecondary
                AgentStatus.COMPRESSING_MEMORY -> com.hive.i8n.R.color.colorAccent
            }
            vStatusIndicator.setBackgroundColor(getColor(colorRes))
        }

        private fun updateStatusText(status: AgentStatus) {

            tvStatusText.text = getStatusInfo(status)
            tvStatusText.setTextColor(getColor(getStatusColor(status)))

        }

        private fun updateCollapsedStatus(status: AgentStatus) {
            tvCollapsedStatus.text = getStatusInfo(status)
            if (status == AgentStatus.COMPRESSING_MEMORY) return
            val input = currentTaskGoal?.input
            if (input is AgentInput) {
                val msg = input.messages.last()
                if (msg.role == MessageRole.TOOL) {
                    val toolName = msg.toolCalls?.firstOrNull()?.function?.name
                        ?.let { if (it.contains(".")) it.substringAfter(".") else it }
                    tvCollapsedStatus.text =
                        toolName?.uppercase() ?: getStatusInfo(status)
                }
            }
//            // 设置图标
//            ivCollapsedIcon.setImageResource(
//                when (status) {
//                    AgentStatus.PAUSED -> R.drawable.ic_agent_play
//                    else -> R.drawable.ic_agent_pause
//                }
//            )
        }

        private fun getStatusInfo(currentStatus: AgentStatus): String {
            return when (currentStatus) {
                AgentStatus.THINKING -> GlobalApp.getString(com.hive.i8n.R.string.script_top_status_thinking)
                AgentStatus.TOOL_CALLING -> GlobalApp.getString(com.hive.i8n.R.string.script_top_status_calling)
                AgentStatus.EXECUTING -> GlobalApp.getString(com.hive.i8n.R.string.script_top_status_executing)
                AgentStatus.COMPLETED -> GlobalApp.getString(com.hive.i8n.R.string.script_top_status_completed)
                AgentStatus.FAILED -> GlobalApp.getString(com.hive.i8n.R.string.script_top_status_task_failed)
                AgentStatus.PAUSED -> GlobalApp.getString(com.hive.i8n.R.string.script_top_status_paused)
                AgentStatus.COMPRESSING_MEMORY -> GlobalApp.getString(com.hive.i8n.R.string.script_top_status_compressing_memory)
            }
        }

        private fun getStatusColor(currentStatus: AgentStatus): Int {
            // 设置指示器颜色
            return when (currentStatus) {
                AgentStatus.THINKING -> com.hive.i8n.R.color.color_orange
                AgentStatus.TOOL_CALLING -> com.hive.i8n.R.color.colorAccent
                AgentStatus.EXECUTING -> com.hive.i8n.R.color.colorPurple
                AgentStatus.COMPLETED -> com.hive.i8n.R.color.colorTextGreen
                AgentStatus.FAILED -> com.hive.i8n.R.color.colorRed
                AgentStatus.PAUSED -> com.hive.i8n.R.color.colorTextSecondary
                AgentStatus.COMPRESSING_MEMORY -> com.hive.i8n.R.color.colorAccent
            }
        }
    }

    // 收起状态管理器
    private inner class CollapseManager {
        private var autoCollapseTimer: Handler? = null
        private val AUTO_COLLAPSE_DELAY = 10000L

        fun startAutoCollapseTimer() {
            autoCollapseTimer = Handler(Looper.getMainLooper())
            resetAutoCollapseTimer()
        }

        fun resetAutoCollapseTimer() {
            autoCollapseTimer?.removeCallbacksAndMessages(null)
            autoCollapseTimer?.postDelayed({
                if (motionController?.isDismissing == false &&
                    motionController?.isTargetCollapsed == false
                ) {
                    collapse()
                }
            }, AUTO_COLLAPSE_DELAY)
        }

        fun toggleCollapse() {
            if (motionController?.isDismissing != false) return
            if (motionController?.isTargetCollapsed == true) {
                expand()
            } else {
                collapse()
            }
        }

        fun expand() {
            val controller = motionController ?: return
            if (controller.isDismissing || !controller.isTargetCollapsed) return
            btnCollapse.setImageResource(R.drawable.ic_collapse_arrow)
            controller.expand { isCollapsed = false }
            resetAutoCollapseTimer()
        }

        private fun collapse() {
            val controller = motionController ?: return
            if (controller.isDismissing || controller.isTargetCollapsed) return
            btnCollapse.setImageResource(R.drawable.ic_expand_arrow)
            controller.collapse { isCollapsed = true }
        }

        fun cleanup() {
            autoCollapseTimer?.removeCallbacksAndMessages(null)
            autoCollapseTimer = null
        }
    }
}
