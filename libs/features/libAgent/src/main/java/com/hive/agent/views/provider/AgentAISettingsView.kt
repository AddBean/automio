// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import com.hive.agent.R
import com.hive.agent.XAgent
import com.hive.agent.ai.DefaultAIServiceManager
import com.hive.agent.ai.DynamicReasoningMetadata
import com.hive.agent.ai.ReasoningCapabilityResolver
import com.hive.agent.config.AIAgentConfig
import com.hive.agent.config.ReasoningRunPolicy
import com.hive.plugin.agent.InferenceType
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ReasoningCapabilities
import com.hive.plugin.agent.model.ReasoningEffort
import com.hive.utils.GlobalApp
import com.hive.views.widgets.CommonToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI 模型与能力设置控件（对话 BottomSheet / 设置页头部共用）。
 * 包含：对话模型、视觉模型、视觉识别开关、深度思考、思考强度、任务记忆摘要，以及可选的「模型与服务」入口。
 */
class AgentAISettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var tvNormalDes: TextView
    private lateinit var tvMultimodalDes: TextView
    private lateinit var tvVisionDesc: TextView
    private lateinit var tvReasoningDesc: TextView
    private lateinit var rowNormalInference: LinearLayout
    private lateinit var rowMultimodalInference: LinearLayout
    private lateinit var rowServiceSettings: LinearLayout
    private lateinit var rowReasoningEffort: LinearLayout
    private lateinit var dividerReasoningEffort: View
    private lateinit var btnReasoningEffortLow: TextView
    private lateinit var btnReasoningEffortMedium: TextView
    private lateinit var btnReasoningEffortHigh: TextView
    private var switchTaskMemory: SwitchCompat? = null
    private var switchVisionRecognition: SwitchCompat? = null
    private var switchReasoning: SwitchCompat? = null

    /** 覆盖默认跳转；为 null 时走 ActivityAgentSelector.start */
    var onSelectModelClick: ((InferenceType) -> Unit)? = null

    /** 「模型与服务设置」点击；仅 showServiceSettingsEntry=true 时可见 */
    var onOpenServiceSettingsClick: (() -> Unit)? = null

    /** 模型或开关变更后回调（便于宿主刷新入口文案） */
    var onSettingsChanged: (() -> Unit)? = null

    private var hasVisionModel: Boolean = false
    private var suppressVisionSwitchCallback: Boolean = false
    private var suppressMemorySwitchCallback: Boolean = false
    private var suppressReasoningSwitchCallback: Boolean = false
    private var currentReasoningUiState: ReasoningSettingsUiState? = null

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.ai_settings_header, this, true)

        tvNormalDes = findViewById(R.id.tvNormalDes)
        tvMultimodalDes = findViewById(R.id.tvMultimodalDes)
        tvVisionDesc = findViewById(R.id.tvVisionDesc)
        tvReasoningDesc = findViewById(R.id.tvReasoningDesc)
        rowNormalInference = findViewById(R.id.rowNormalInference)
        rowMultimodalInference = findViewById(R.id.rowMultimodalInference)
        rowServiceSettings = findViewById(R.id.rowServiceSettings)
        rowReasoningEffort = findViewById(R.id.rowReasoningEffort)
        dividerReasoningEffort = findViewById(R.id.dividerReasoningEffort)
        btnReasoningEffortLow = findViewById(R.id.btnReasoningEffortLow)
        btnReasoningEffortMedium = findViewById(R.id.btnReasoningEffortMedium)
        btnReasoningEffortHigh = findViewById(R.id.btnReasoningEffortHigh)

        rowNormalInference.setOnClickListener {
            dispatchSelectModel(InferenceType.TEXT)
        }
        rowMultimodalInference.setOnClickListener {
            dispatchSelectModel(InferenceType.IMAGE)
        }
        rowServiceSettings.setOnClickListener {
            onOpenServiceSettingsClick?.invoke()
                ?: ActivityAgentSetting.start(context)
        }

        switchTaskMemory = findViewById(R.id.switchTaskMemory)
        switchTaskMemory?.isChecked = AIAgentConfig.MemoryConfig.isTaskMemoryEnabled()
        switchTaskMemory?.setOnCheckedChangeListener { _, isChecked ->
            if (suppressMemorySwitchCallback) return@setOnCheckedChangeListener
            AIAgentConfig.MemoryConfig.setTaskMemoryEnabled(isChecked)
            onSettingsChanged?.invoke()
        }

        switchVisionRecognition = findViewById(R.id.switchVisionRecognition)
        switchVisionRecognition?.isChecked = AIAgentConfig.VisionConfig.isVisionRecognitionEnabled()
        switchVisionRecognition?.setOnCheckedChangeListener { _, isChecked ->
            if (suppressVisionSwitchCallback) return@setOnCheckedChangeListener
            if (!hasVisionModel) {
                suppressVisionSwitchCallback = true
                switchVisionRecognition?.isChecked =
                    AIAgentConfig.VisionConfig.isVisionRecognitionEnabled()
                suppressVisionSwitchCallback = false
                return@setOnCheckedChangeListener
            }
            AIAgentConfig.VisionConfig.setVisionRecognitionEnabled(isChecked)
            updateVisionSwitchUi()
            onSettingsChanged?.invoke()
        }

        switchReasoning = findViewById(R.id.switchReasoning)
        // 能力解析完成前先禁用，避免 state=null 时点击被立刻回滚，表现为「无响应」
        applyReasoningUiState(
            ReasoningSettingsUiStateFactory.create(
                savedEnabled = AIAgentConfig.ReasoningConfig.isEnabled(),
                savedEffort = AIAgentConfig.ReasoningConfig.effort(),
                capabilities = null,
                modelSelected = false
            )
        )
        switchReasoning?.setOnCheckedChangeListener { _, isChecked ->
            if (suppressReasoningSwitchCallback) return@setOnCheckedChangeListener
            val state = currentReasoningUiState
            if (state == null || !state.canPersistSwitch) {
                suppressReasoningSwitchCallback = true
                switchReasoning?.isChecked = state?.switchChecked ?: false
                suppressReasoningSwitchCallback = false
                showReasoningUnavailableFeedback(state)
                return@setOnCheckedChangeListener
            }
            AIAgentConfig.ReasoningConfig.setEnabled(isChecked)
            applyReasoningUiState(
                ReasoningSettingsUiStateFactory.create(
                    savedEnabled = AIAgentConfig.ReasoningConfig.isEnabled(),
                    savedEffort = AIAgentConfig.ReasoningConfig.effort(),
                    capabilities = lastResolvedCapabilities,
                    modelSelected = currentReasoningUiState?.switchHint != ReasoningSwitchHint.NO_MODEL
                )
            )
            onSettingsChanged?.invoke()
        }
        // 整行可点：可选时切换；禁用时 Toast 说明原因（避免「点了没反应」）
        // 开关本体在 enabled 时自行消费点击，不会与行点击双重 toggle
        findViewById<View>(R.id.rowReasoningSwitch).setOnClickListener {
            onReasoningRowClicked()
        }

        btnReasoningEffortLow.setOnClickListener { onEffortClicked(ReasoningEffort.LOW) }
        btnReasoningEffortMedium.setOnClickListener { onEffortClicked(ReasoningEffort.MEDIUM) }
        btnReasoningEffortHigh.setOnClickListener { onEffortClicked(ReasoningEffort.HIGH) }

        setShowServiceSettingsEntry(false)
        updateStatus()
    }

    private var lastResolvedCapabilities: ReasoningCapabilities? = null

    fun setShowServiceSettingsEntry(show: Boolean) {
        rowServiceSettings.isVisible = show
    }

    private fun dispatchSelectModel(type: InferenceType) {
        val custom = onSelectModelClick
        if (custom != null) {
            custom.invoke(type)
        } else {
            ActivityAgentSelector.start(context, type)
        }
    }

    fun updateStatus() {
        viewScope.launch {
            refreshSelectedModels()
        }
    }

    private fun bindModelStatus(target: TextView, status: InferenceModelSelectionStatus) {
        val name = status.displayName
        when (status.kind) {
            InferenceModelSelectionKind.NOT_SET -> {
                target.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_accent_rose))
                target.text = context.getString(com.hive.i8n.R.string.ai_model_not_set)
            }
            InferenceModelSelectionKind.READY -> {
                target.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_text_tertiary))
                target.text = name.orEmpty()
            }
            InferenceModelSelectionKind.NEEDS_CONFIG -> {
                target.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_accent_rose))
                target.text = context.getString(
                    com.hive.i8n.R.string.ai_model_needs_config,
                    name.orEmpty()
                )
            }
            InferenceModelSelectionKind.REFRESH_FAILED -> {
                target.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_accent_rose))
                target.text = context.getString(
                    com.hive.i8n.R.string.ai_model_refresh_failed,
                    name.orEmpty()
                )
            }
            InferenceModelSelectionKind.INVALID_FOR_TYPE -> {
                target.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_accent_rose))
                target.text = context.getString(
                    com.hive.i8n.R.string.ai_model_invalid_vision,
                    name.orEmpty()
                )
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                InferenceType.TEXT.type, InferenceType.IMAGE.type -> {
                    applySelectedModel(
                        InferenceType.parserType(requestCode),
                        data?.getSerializableExtra("data") as? ModelInfo
                    )
                }
            }
        }
        updateStatus()
    }

    /** 供 BottomSheet 的 ActivityResult 回调写入所选模型 */
    fun applySelectedModel(type: InferenceType, model: ModelInfo?): Boolean {
        if (model == null) return false
        val aiServiceManager =
            XAgent.getInstance().getAIServiceManager() as? DefaultAIServiceManager
        val ready = aiServiceManager?.getProvider(model.providerId)?.isProviderReady() == true
        return if (ready) {
            XAgent.getInstance().getAIServiceManager()?.setInferenceModel(type, model)
            onSettingsChanged?.invoke()
            true
        } else {
            CommonToast.getInstance().showToast(
                context.getString(com.hive.i8n.R.string.ai_set_api_key_first, model.providerId)
            )
            false
        }
    }

    private suspend fun refreshSelectedModels() {
        val aiServiceManager =
            XAgent.getInstance().getAIServiceManager() as? DefaultAIServiceManager

        // 先用本地已保存选择立刻铺 UI，避免闪「未设置」
        val savedText = aiServiceManager?.getInferenceModel(InferenceType.TEXT)
        val savedImage = aiServiceManager?.getInferenceModel(InferenceType.IMAGE)
        if (savedText != null) {
            bindModelStatus(
                tvNormalDes,
                InferenceModelSelectionStatus(InferenceModelSelectionKind.READY, savedText)
            )
        } else {
            bindModelStatus(
                tvNormalDes,
                InferenceModelSelectionStatus(InferenceModelSelectionKind.NOT_SET, null)
            )
        }
        if (savedImage != null) {
            bindModelStatus(
                tvMultimodalDes,
                InferenceModelSelectionStatus(InferenceModelSelectionKind.READY, savedImage)
            )
        } else {
            bindModelStatus(
                tvMultimodalDes,
                InferenceModelSelectionStatus(InferenceModelSelectionKind.NOT_SET, null)
            )
        }

        val normalStatus = resolveInferenceSelection(
            aiServiceManager,
            InferenceType.TEXT,
            requireVision = false
        )
        val multimodalStatus = resolveInferenceSelection(
            aiServiceManager,
            InferenceType.IMAGE,
            requireVision = true
        )

        // READY 时用目录刷新后的 ModelInfo 回写本地，保持能力字段最新；其它状态绝不清空
        if (normalStatus.kind == InferenceModelSelectionKind.READY) {
            normalStatus.model?.let { aiServiceManager?.setInferenceModel(InferenceType.TEXT, it) }
        }
        if (multimodalStatus.kind == InferenceModelSelectionKind.READY) {
            multimodalStatus.model?.let {
                aiServiceManager?.setInferenceModel(InferenceType.IMAGE, it)
            }
        }

        bindModelStatus(tvNormalDes, normalStatus)
        bindModelStatus(tvMultimodalDes, multimodalStatus)
        hasVisionModel = multimodalStatus.countsAsConfiguredVisionModel
        suppressMemorySwitchCallback = true
        switchTaskMemory?.isChecked = AIAgentConfig.MemoryConfig.isTaskMemoryEnabled()
        suppressMemorySwitchCallback = false
        updateVisionSwitchUi()
        updateReasoningUi(normalStatus.model, modelSelected = normalStatus.hasSelection)
    }

    private fun updateVisionSwitchUi() {
        val switch = switchVisionRecognition ?: return
        suppressVisionSwitchCallback = true
        switch.isEnabled = hasVisionModel
        switch.alpha = if (hasVisionModel) 1f else 0.45f
        switch.isChecked = AIAgentConfig.VisionConfig.isVisionRecognitionEnabled()
        suppressVisionSwitchCallback = false

        tvVisionDesc.text = if (!hasVisionModel) {
            context.getString(com.hive.i8n.R.string.agent_vision_recognition_need_model)
        } else {
            context.getString(com.hive.i8n.R.string.agent_vision_recognition_desc)
        }
    }

    private fun updateReasoningUi(textModel: ModelInfo?, modelSelected: Boolean) {
        val capabilities = resolveReasoningCapabilities(textModel)
        lastResolvedCapabilities = capabilities
        val uiState = ReasoningSettingsUiStateFactory.create(
            savedEnabled = AIAgentConfig.ReasoningConfig.isEnabled(),
            savedEffort = AIAgentConfig.ReasoningConfig.effort(),
            capabilities = capabilities,
            modelSelected = modelSelected
        )
        applyReasoningUiState(uiState)
    }

    /**
     * ModelInfo.reasoning 优先；为空时走 [ReasoningCapabilityResolver] / 内置 catalog。
     * 不改写全局 ReasoningConfig。
     */
    private fun resolveReasoningCapabilities(model: ModelInfo?): ReasoningCapabilities? {
        if (model == null) return null
        // 仅在模型显式声明 reasoning 时传入动态元数据；null 能力不得覆盖 catalog
        val dynamic = model.capabilities.reasoning?.let { DynamicReasoningMetadata(capabilities = it) }
        return ReasoningCapabilityResolver.resolve(
            providerId = model.providerId,
            modelId = model.modelId,
            dynamicMetadata = dynamic,
            requestedPolicy = ReasoningRunPolicy(
                enabled = AIAgentConfig.ReasoningConfig.isEnabled(),
                effort = AIAgentConfig.ReasoningConfig.effort()
            )
        ).capabilities
    }

    /** 整行可点：可选模型切换开关；禁用时 Toast 说明原因，避免「点了没反应」。 */
    private fun onReasoningRowClicked() {
        val state = currentReasoningUiState
        val switch = switchReasoning ?: return
        if (state != null && state.canPersistSwitch && state.switchEnabled) {
            switch.isChecked = !switch.isChecked
            return
        }
        showReasoningUnavailableFeedback(state)
    }

    private fun showReasoningUnavailableFeedback(state: ReasoningSettingsUiState?) {
        val hint = state?.switchHint ?: ReasoningSwitchHint.UNKNOWN
        val messageRes = when (hint) {
            ReasoningSwitchHint.OPTIONAL,
            ReasoningSwitchHint.NO_MODEL -> return
            ReasoningSwitchHint.REQUIRED -> com.hive.i8n.R.string.agent_reasoning_required_hint
            ReasoningSwitchHint.UNSUPPORTED -> com.hive.i8n.R.string.agent_reasoning_unsupported_hint
            ReasoningSwitchHint.UNKNOWN -> com.hive.i8n.R.string.agent_reasoning_unknown_hint
        }
        CommonToast.getInstance().showToast(context.getString(messageRes))
    }

    private fun applyReasoningUiState(state: ReasoningSettingsUiState) {
        currentReasoningUiState = state
        val switch = switchReasoning
        suppressReasoningSwitchCallback = true
        if (switch != null) {
            switch.isEnabled = state.switchEnabled
            switch.alpha = if (state.switchEnabled) 1f else 0.45f
            switch.isChecked = state.switchChecked
            switch.contentDescription = context.getString(com.hive.i8n.R.string.agent_reasoning_switch_cd)
        }
        suppressReasoningSwitchCallback = false

        tvReasoningDesc.text = context.getString(
            when (state.switchHint) {
                ReasoningSwitchHint.OPTIONAL -> com.hive.i8n.R.string.agent_reasoning_switch_desc
                ReasoningSwitchHint.REQUIRED -> com.hive.i8n.R.string.agent_reasoning_required_hint
                ReasoningSwitchHint.UNSUPPORTED -> com.hive.i8n.R.string.agent_reasoning_unsupported_hint
                ReasoningSwitchHint.UNKNOWN -> com.hive.i8n.R.string.agent_reasoning_unknown_hint
                ReasoningSwitchHint.NO_MODEL -> com.hive.i8n.R.string.agent_reasoning_no_model_hint
            }
        )

        rowReasoningEffort.isVisible = state.effortRowVisible
        dividerReasoningEffort.isVisible = state.effortRowVisible
        rowReasoningEffort.alpha = if (state.effortRowEnabled) 1f else 0.45f

        bindEffortChip(
            btnReasoningEffortLow,
            ReasoningEffort.LOW,
            state,
            com.hive.i8n.R.string.agent_reasoning_effort_low
        )
        bindEffortChip(
            btnReasoningEffortMedium,
            ReasoningEffort.MEDIUM,
            state,
            com.hive.i8n.R.string.agent_reasoning_effort_medium
        )
        bindEffortChip(
            btnReasoningEffortHigh,
            ReasoningEffort.HIGH,
            state,
            com.hive.i8n.R.string.agent_reasoning_effort_high
        )
    }

    private fun bindEffortChip(
        chip: TextView,
        effort: ReasoningEffort,
        state: ReasoningSettingsUiState,
        labelRes: Int
    ) {
        val supported = effort in state.supportedEfforts
        val selected = state.selectedEffort == effort
        chip.isVisible = state.effortRowVisible && (state.supportedEfforts.isEmpty() || supported)
        // When supportedEfforts empty row is hidden; keep chips for partial sets only.
        if (!supported && state.supportedEfforts.isNotEmpty()) {
            chip.isVisible = false
            return
        }
        chip.isEnabled = state.effortRowEnabled && supported
        chip.isSelected = selected
        chip.alpha = if (chip.isEnabled) 1f else 0.45f
        val label = context.getString(labelRes)
        chip.contentDescription = if (state.effortRowEnabled && supported) {
            context.getString(com.hive.i8n.R.string.agent_reasoning_effort_cd, label)
        } else {
            context.getString(com.hive.i8n.R.string.agent_reasoning_effort_disabled_cd)
        }
        chip.setTextColor(
            GlobalApp.getColor(
                if (selected) com.hive.i8n.R.color.design_accent_indigo_text
                else com.hive.i8n.R.color.design_text_secondary
            )
        )
    }

    private fun onEffortClicked(effort: ReasoningEffort) {
        val state = currentReasoningUiState ?: return
        if (!state.canPersistEffort || effort !in state.supportedEfforts) return
        AIAgentConfig.ReasoningConfig.setEffort(effort)
        applyReasoningUiState(
            ReasoningSettingsUiStateFactory.create(
                savedEnabled = AIAgentConfig.ReasoningConfig.isEnabled(),
                savedEffort = AIAgentConfig.ReasoningConfig.effort(),
                capabilities = lastResolvedCapabilities,
                modelSelected = state.switchHint != ReasoningSwitchHint.NO_MODEL
            )
        )
        onSettingsChanged?.invoke()
    }

    private suspend fun resolveInferenceSelection(
        manager: DefaultAIServiceManager?,
        type: InferenceType,
        requireVision: Boolean
    ): InferenceModelSelectionStatus {
        val selected = manager?.getInferenceModel(type)
        return InferenceModelSelectionResolver.resolve(
            selected = selected,
            requireVision = requireVision,
            providerExists = { id -> manager?.getProvider(id) != null },
            providerEnabled = { id -> manager?.isProviderEnabled(id) == true },
            providerReady = { id -> manager?.getProvider(id)?.isProviderReady() == true },
            findInCatalog = find@{ model ->
                val provider = manager?.getProvider(model.providerId) ?: return@find null
                withContext(Dispatchers.IO) {
                    provider.getModels().firstOrNull { it.modelId == model.modelId }
                }
            }
        )
    }

    override fun onDetachedFromWindow() {
        viewScope.cancel()
        super.onDetachedFromWindow()
    }
}
