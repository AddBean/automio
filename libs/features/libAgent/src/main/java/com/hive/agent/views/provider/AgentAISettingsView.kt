// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import com.hive.agent.R
import com.hive.agent.XAgent
import com.hive.agent.ai.DefaultAIServiceManager
import com.hive.agent.config.AIAgentConfig
import com.hive.plugin.agent.InferenceType
import com.hive.plugin.agent.ModelInfo
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
 * 包含：对话模型、视觉模型、视觉识别开关、任务记忆摘要，以及可选的「模型与服务」入口。
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
    private lateinit var rowNormalInference: LinearLayout
    private lateinit var rowMultimodalInference: LinearLayout
    private lateinit var rowServiceSettings: LinearLayout
    private var switchTaskMemory: SwitchCompat? = null
    private var switchVisionRecognition: SwitchCompat? = null

    /** 覆盖默认跳转；为 null 时走 ActivityAgentSelector.start */
    var onSelectModelClick: ((InferenceType) -> Unit)? = null

    /** 「模型与服务设置」点击；仅 showServiceSettingsEntry=true 时可见 */
    var onOpenServiceSettingsClick: (() -> Unit)? = null

    /** 模型或开关变更后回调（便于宿主刷新入口文案） */
    var onSettingsChanged: (() -> Unit)? = null

    private var hasVisionModel: Boolean = false
    private var suppressVisionSwitchCallback: Boolean = false
    private var suppressMemorySwitchCallback: Boolean = false

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.ai_settings_header, this, true)

        tvNormalDes = findViewById(R.id.tvNormalDes)
        tvMultimodalDes = findViewById(R.id.tvMultimodalDes)
        tvVisionDesc = findViewById(R.id.tvVisionDesc)
        rowNormalInference = findViewById(R.id.rowNormalInference)
        rowMultimodalInference = findViewById(R.id.rowMultimodalInference)
        rowServiceSettings = findViewById(R.id.rowServiceSettings)

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

        setShowServiceSettingsEntry(false)
        updateStatus()
    }

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

    private fun setNormalInferenceModel(modelName: String?) {
        if (modelName.isNullOrEmpty()) {
            tvNormalDes.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_accent_rose))
            tvNormalDes.text = context.getString(com.hive.i8n.R.string.ai_model_not_set)
        } else {
            tvNormalDes.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_text_tertiary))
            tvNormalDes.text = modelName
        }
    }

    private fun setMultimodalInferenceModel(modelName: String?) {
        if (modelName.isNullOrEmpty()) {
            tvMultimodalDes.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_accent_rose))
            tvMultimodalDes.text = context.getString(com.hive.i8n.R.string.ai_model_not_set)
        } else {
            tvMultimodalDes.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_text_tertiary))
            tvMultimodalDes.text = modelName
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
        val normalModel = validateInferenceModel(aiServiceManager, InferenceType.TEXT)
        val multimodalModel = validateInferenceModel(aiServiceManager, InferenceType.IMAGE)

        setNormalInferenceModel(normalModel?.displayName)
        setMultimodalInferenceModel(multimodalModel?.displayName)
        hasVisionModel = multimodalModel != null
        suppressMemorySwitchCallback = true
        switchTaskMemory?.isChecked = AIAgentConfig.MemoryConfig.isTaskMemoryEnabled()
        suppressMemorySwitchCallback = false
        updateVisionSwitchUi()
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

    private suspend fun validateInferenceModel(
        manager: DefaultAIServiceManager?,
        type: InferenceType
    ): ModelInfo? {
        val selectedModel = manager?.getInferenceModel(type) ?: return null
        val provider = manager.getProvider(selectedModel.providerId) ?: run {
            manager.setInferenceModel(type, null)
            return null
        }
        if (!manager.isProviderEnabled(selectedModel.providerId) || !provider.isProviderReady()) {
            manager.setInferenceModel(type, null)
            return null
        }
        val currentModel = withContext(Dispatchers.IO) {
            provider.getModels().firstOrNull { it.modelId == selectedModel.modelId }
        } ?: run {
            manager.setInferenceModel(type, null)
            return null
        }
        if (type == InferenceType.IMAGE && !currentModel.capabilities.supportsVision) {
            manager.setInferenceModel(type, null)
            return null
        }
        if (currentModel != selectedModel) {
            manager.setInferenceModel(type, currentModel)
        }
        return currentModel
    }

    override fun onDetachedFromWindow() {
        viewScope.cancel()
        super.onDetachedFromWindow()
    }
}
