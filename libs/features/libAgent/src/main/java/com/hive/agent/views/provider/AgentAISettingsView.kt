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
 * AI设置头部控件
 * 包含普通推理和多模态推理两个设置入口
 */
class AgentAISettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var tvNormalInference: TextView
    private lateinit var tvMultimodalInference: TextView
    private lateinit var tvNormalDes: TextView
    private lateinit var tvMultimodalDes: TextView
    private var switchTaskMemory: SwitchCompat? = null

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.ai_settings_header, this, true)

        tvNormalInference = findViewById(R.id.tvNormalInference)
        tvMultimodalInference = findViewById(R.id.tvMultimodalInference)
        tvNormalDes = findViewById(R.id.tvNormalDes)
        tvMultimodalDes = findViewById(R.id.tvMultimodalDes)

        // 设置点击事件
        tvNormalInference.setOnClickListener {
            ActivityAgentSelector.start(context, InferenceType.TEXT)
        }
        tvNormalDes.setOnClickListener {
            ActivityAgentSelector.start(context, InferenceType.TEXT)
        }

        tvMultimodalInference.setOnClickListener {
            ActivityAgentSelector.start(context, InferenceType.IMAGE)
        }
        tvMultimodalDes.setOnClickListener {
            ActivityAgentSelector.start(context, InferenceType.IMAGE)
        }

        switchTaskMemory = findViewById(R.id.switchTaskMemory)
        switchTaskMemory?.isChecked = AIAgentConfig.MemoryConfig.isTaskMemoryEnabled()
        switchTaskMemory?.setOnCheckedChangeListener { _, isChecked ->
            AIAgentConfig.MemoryConfig.setTaskMemoryEnabled(isChecked)
        }

        updateStatus()
    }


    fun updateStatus() {
        viewScope.launch {
            refreshSelectedModels()
        }
    }

    /**
     * 设置普通推理的模型信息
     */
    private fun setNormalInferenceModel(modelName: String?) {
        if (modelName.isNullOrEmpty()) {
            tvNormalDes.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_accent_rose))
            tvNormalDes.text = context.getString(com.hive.i8n.R.string.ai_model_not_set)
        } else {
            tvNormalDes.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_text_tertiary))
            tvNormalDes.text = "$modelName"
        }
    }

    /**
     * 设置多模态推理的模型信息
     */
    private fun setMultimodalInferenceModel(modelName: String?) {
        if (modelName.isNullOrEmpty()) {
            tvMultimodalDes.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_accent_rose))
            tvMultimodalDes.text = context.getString(com.hive.i8n.R.string.ai_model_not_set)
        } else {
            tvMultimodalDes.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_text_tertiary))
            tvMultimodalDes.text = "$modelName"
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                InferenceType.TEXT.type, InferenceType.IMAGE.type -> {
                    val model = data?.getSerializableExtra("data") as ModelInfo
                    val aiServiceManager =
                        XAgent.getInstance().getAIServiceManager() as? DefaultAIServiceManager
                    if (model.providerId.let {
                            aiServiceManager?.getProvider(it)?.hasValidApiKey()
                        } == true) {
                        XAgent.getInstance().getAIServiceManager()
                            ?.setInferenceModel(InferenceType.parserType(requestCode), model)
                    } else {
                        CommonToast.getInstance().showToast(
                            context.getString(com.hive.i8n.R.string.ai_set_api_key_first, model.providerId)
                        )
                    }
                }
            }
        }
        updateStatus()
    }

    private suspend fun refreshSelectedModels() {
        val aiServiceManager =
            XAgent.getInstance().getAIServiceManager() as? DefaultAIServiceManager
        val normalModel = validateInferenceModel(aiServiceManager, InferenceType.TEXT)
        val multimodalModel = validateInferenceModel(aiServiceManager, InferenceType.IMAGE)

        setNormalInferenceModel(normalModel?.displayName)
        setMultimodalInferenceModel(multimodalModel?.displayName)
        switchTaskMemory?.isChecked = AIAgentConfig.MemoryConfig.isTaskMemoryEnabled()
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
