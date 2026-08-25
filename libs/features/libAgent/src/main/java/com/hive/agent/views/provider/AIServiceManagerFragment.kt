// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import android.app.Dialog
import android.content.Intent
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.util.Pair
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.carlos.ui.header.CommonHeader
import com.hive.agent.R
import com.hive.agent.XAgent
import com.hive.agent.ai.DefaultAIServiceManager
import com.hive.agent.ai.providers.AbstractBaseProvider
import com.hive.agent.ai.providers.OllamaProvider
import com.hive.base.BaseFragment
import com.hive.plugin.agent.ModelCapabilities
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ModelType
import com.hive.views.StatefulLayout
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.ListRecyclerView
import com.hive.views.widgets.CommonToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AIServiceManagerFragment : BaseFragment() {

    private var listRecyclerView: ListRecyclerView? = null
    private var aiServiceManager: DefaultAIServiceManager? = null
    private var aiSettingsHeader: AgentAISettingsView? = null
    private var statefulLayout: StatefulLayout? = null
    private var commonHeader: CommonHeader? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    // 跟踪展开状态的Provider
    private val expandedProviders = mutableSetOf<String>()

    override fun initView() {
        listRecyclerView = view?.findViewById(R.id.listRecyclerView)
        aiSettingsHeader = view?.findViewById(R.id.aiSettingsHeader)
        statefulLayout = view?.findViewById(R.id.statefulLayout)
        commonHeader = view?.findViewById(R.id.header)
        // 获取AI服务管理器
        aiServiceManager = XAgent.getInstance().getAIServiceManager() as? DefaultAIServiceManager

        commonHeader?.getRightImageView()?.setOnClickListener {
            refreshData()
        }
        // 初始化列表
        setupListView()

        // 加载Provider数据
        loadProviderData()
    }

    private fun refreshData() {
        OllamaProvider.cacheTimestamp = 0
        OllamaProvider.cachedModels = null
        loadProviderData()
    }


    private fun setupListView() {
        listRecyclerView?.let { recyclerView ->
            // 设置视图工厂
            recyclerView.setItemViewFactory(AIProviderViewFactory(requireContext()))

            // 设置事件监听器
            recyclerView.setOnItemEventListener(object : ListRecyclerItemView.OnItemEventListener {
                override fun onItemEvent(itemData: Any?, eventData: Any?) {
                    handleItemEvent(eventData)
                }
            })
        }
    }

    private fun loadProviderData() {
        lifecycleScope.launch(Dispatchers.Main) {
            statefulLayout?.showProgress()
            // 确保获取最新的AI服务管理器（若 Agent 未初始化则可能为 null）
            aiServiceManager =
                XAgent.getInstance().getAIServiceManager() as? DefaultAIServiceManager
            if (aiServiceManager == null) {
                listRecyclerView?.submitDataSetsWithType(emptyList())
                listRecyclerView?.notifyDataSetChanged()
                statefulLayout?.showContent()
                return@launch
            }
            // 从AI服务管理器获取所有Provider信息
            val providers = aiServiceManager?.getProviderList() ?: return@launch
            val providerDataList =
                providers.sortedByDescending { it.getProviderInfo().sortIndex }.map { p ->
                    val pid = p.getProviderInfo().name
                    AIProviderItemData(
                        provider = p,
                        isEnabled = aiServiceManager?.isProviderEnabled(pid) == true,
                        hasValidApiKey = p.hasValidApiKey(),
                        providerId = pid,
                        providerInfo = p.getProviderInfo(),
                        providerName = p.getProviderInfo().displayName,
                        providerDescription = p.getProviderInfo().description,
                        providerTags = p.getTags(),
                        models = withContext(Dispatchers.IO) { p.getModels() },
                        isExpanded = expandedProviders.contains(pid)
                    )
                } ?: return@launch

            // 构建完整的数据列表（包括Provider和模型）
            val dataWithType = mutableListOf<Pair<Int, Any?>>()

            providerDataList.forEach { providerData ->
                // 添加Provider项
                dataWithType.add(Pair(AIProviderViewFactory.TYPE_PROVIDER, providerData))
            }

            // 提交数据到列表
            val androidPairList = dataWithType.map { Pair(it.first, it.second) }
            listRecyclerView?.submitDataSetsWithType(androidPairList)
            listRecyclerView?.notifyDataSetChanged()
            statefulLayout?.showContent()
        }
    }

    private fun handleItemEvent(eventData: Any?) {
        when (eventData) {
            is Map<*, *> -> {
                val eventType = eventData["eventType"] as? String
                when (eventType) {
                    "toggle_enabled" -> {
                        val providerId = eventData["providerId"] as? String
                        val enabled = eventData["enabled"] as? Boolean
                        if (providerId != null && enabled != null) {
                            toggleProviderEnabled(providerId, enabled)
                        }
                    }

                    "show_api_key_dialog" -> {
                        val providerId = eventData["providerId"] as? String
                        val providerName = eventData["providerName"] as? String
                        if (providerId != null && providerName != null) {
                            showApiKeyInputDialog(providerId, providerName)
                        }
                    }

                    "show_settings_dialog" -> {
                        val providerId = eventData["providerId"] as? String
                        val providerName = eventData["providerName"] as? String
                        if (providerId != null && providerName != null) {
                            showApiKeyInputDialog(providerId, providerName)
                        }
                    }

                    "toggle_models_expanded" -> {
                        val providerId = eventData["providerId"] as? String
                        if (providerId != null) {
                            toggleModelsExpanded(providerId)
                        }
                    }

                    "show_add_model_dialog" -> {
                        val providerId = eventData["providerId"] as? String
                        val providerName = eventData["providerName"] as? String
                        if (providerId != null && providerName != null) {
                            showAddModelDialog(providerId, providerName)
                        }
                    }

                    "toggle_model_enabled" -> {
                        val providerId = eventData["providerId"] as? String
                        val modelId = eventData["modelId"] as? String
                        val enabled = eventData["enabled"] as? Boolean
                        if (providerId != null && modelId != null && enabled != null) {
                            toggleModelEnabled(providerId, modelId, enabled)
                        }
                    }
                }
            }
        }
    }

    private fun toggleModelsExpanded(providerId: String) {
        if (expandedProviders.contains(providerId)) {
            expandedProviders.remove(providerId)
        } else {
            expandedProviders.add(providerId)
        }
        loadProviderData()
    }

    private fun showAddModelDialog(providerId: String, providerName: String) {
        val dialog = Dialog(requireContext(), com.hive.views.R.style.base_dialog)
        val view = LayoutInflater.from(context).inflate(R.layout.ai_dialog_input, null)
        dialog.setContentView(view)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)
        val inputContainer = view.findViewById<View>(R.id.inputContainer)
        val modelNameWrapper = view.findViewById<View>(R.id.modelNameWrapper)
        val modelIdWrapper = view.findViewById<View>(R.id.modelIdWrapper)
        val capabilitiesContainer = view.findViewById<View>(R.id.capabilitiesContainer)
        val functionCallContainer = view.findViewById<View>(R.id.functionCallContainer)
        val etModelName = view.findViewById<EditText>(R.id.etModelName)
        val etModelId = view.findViewById<EditText>(R.id.etModelId)
        val switchVision = view.findViewById<SwitchCompat>(R.id.switchVision)
        val switchFunctionCall = view.findViewById<SwitchCompat>(R.id.switchFunctionCall)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        val btnConfirm = view.findViewById<TextView>(R.id.btnConfirm)
        val btnClear = view.findViewById<TextView>(R.id.btnClear)

        tvTitle.text = getString(com.hive.i8n.R.string.ai_add_model_to_provider, providerName)
        tvSubtitle.visibility = View.GONE

        // 隐藏 API Key 输入，显示模型输入
        inputContainer.visibility = View.GONE
        modelNameWrapper.visibility = View.VISIBLE
        modelIdWrapper.visibility = View.VISIBLE
        capabilitiesContainer.visibility = View.VISIBLE
        functionCallContainer.visibility = View.VISIBLE
        btnClear.visibility = View.GONE
        btnConfirm.text = getString(com.hive.i8n.R.string.ai_add)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            val modelName = etModelName.text.toString().trim()
            val modelId = etModelId.text.toString().trim()

            if (modelName.isEmpty() || modelId.isEmpty()) {
                CommonToast.getInstance().showToast(
                    getString(com.hive.i8n.R.string.ai_model_name_id_required)
                )
                return@setOnClickListener
            }

            val capabilities = ModelCapabilities(
                supportsFunctionCall = switchFunctionCall.isChecked,
                supportsVision = switchVision.isChecked,
                contextWindow = 128000,
                modelType = ModelType.CHAT
            )

            val modelInfo = ModelInfo(
                modelId = modelId,
                displayName = modelName,
                providerId = providerId,
                capabilities = capabilities,
                buildIn = false
            )

            val manager = XAgent.getInstance().getAIServiceManager() as? DefaultAIServiceManager
            manager?.addProviderCustomModel(providerId, modelInfo)
            manager?.enableProviderModel(providerId, modelId)

            loadProviderData()
            CommonToast.getInstance().showToast(
                getString(com.hive.i8n.R.string.ai_model_added)
            )
            dialog.dismiss()
        }

        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.show()
    }

    private fun toggleModelEnabled(providerId: String, modelId: String, enabled: Boolean) {
        val manager = XAgent.getInstance().getAIServiceManager() as? DefaultAIServiceManager
        if (enabled) {
            manager?.enableProviderModel(providerId, modelId)
        } else {
            manager?.disableProviderModel(providerId, modelId)
        }

        // 刷新列表
        scope.launch {
            loadProviderData()
        }
    }

    private fun showApiKeyInputDialog(providerId: String, providerName: String) {
        val manager = XAgent.getInstance().getAIServiceManager() as? DefaultAIServiceManager
        val provider = manager?.getProvider(providerId) as? AbstractBaseProvider
        val supportsBaseUrl = provider?.supportsEditableBaseUrl() == true
        val requiresBaseUrl = provider?.requiresBaseUrl() == true
        val currentApiKey = manager?.getProviderApiKey(providerId) ?: ""
        val currentBaseUrl = manager?.getProviderBaseUrl(providerId)
            ?: manager?.getProviderInfo(providerId)?.apiUrl.orEmpty()

        val dialog = Dialog(requireContext(), com.hive.views.R.style.base_dialog)
        val view = LayoutInflater.from(context).inflate(R.layout.ai_dialog_input, null)
        dialog.setContentView(view)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)
        val inputContainer = view.findViewById<View>(R.id.inputContainer)
        val etInput = view.findViewById<EditText>(R.id.etInput)
        val btnToggleVisibility = view.findViewById<TextView>(R.id.btnToggleVisibility)
        val modelNameWrapper = view.findViewById<View>(R.id.modelNameWrapper)
        val etModelName = view.findViewById<EditText>(R.id.etModelName)
        val modelIdWrapper = view.findViewById<View>(R.id.modelIdWrapper)
        val capabilitiesContainer = view.findViewById<View>(R.id.capabilitiesContainer)
        val functionCallContainer = view.findViewById<View>(R.id.functionCallContainer)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        val btnConfirm = view.findViewById<TextView>(R.id.btnConfirm)
        val btnClear = view.findViewById<TextView>(R.id.btnClear)

        tvTitle.text = getString(com.hive.i8n.R.string.ai_set_api_key, providerName)
        tvSubtitle.visibility = View.VISIBLE
        tvSubtitle.text = if (supportsBaseUrl) {
            getString(com.hive.i8n.R.string.ai_enter_api_key_and_base_url, providerName)
        } else {
            getString(com.hive.i8n.R.string.ai_enter_api_key, providerName)
        }

        inputContainer.visibility = View.VISIBLE
        modelNameWrapper.visibility = if (supportsBaseUrl) View.VISIBLE else View.GONE
        modelIdWrapper.visibility = View.GONE
        capabilitiesContainer.visibility = View.GONE
        functionCallContainer.visibility = View.GONE
        btnClear.visibility = View.VISIBLE

        etInput.setText(currentApiKey)
        etInput.hint = getString(com.hive.i8n.R.string.ai_enter_api_key, providerName)
        etModelName.setText(currentBaseUrl)
        etModelName.hint = getString(com.hive.i8n.R.string.ai_base_url_hint)
        etModelName.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI

        var isPasswordVisible = false
        btnToggleVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etInput.inputType = if (isPasswordVisible) {
                InputType.TYPE_CLASS_TEXT
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etInput.setSelection(etInput.text?.length ?: 0)
            btnToggleVisibility.text = getString(
                if (isPasswordVisible) com.hive.i8n.R.string.ai_hide_password
                else com.hive.i8n.R.string.ai_show_password
            )
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            val apiKey = etInput.text.toString().trim()
            val baseUrl = etModelName.text.toString().trim().trimEnd('/')
            if (apiKey.isEmpty()) {
                CommonToast.getInstance().showToast(
                    getString(com.hive.i8n.R.string.ai_api_key_required)
                )
                return@setOnClickListener
            }
            if (supportsBaseUrl && (requiresBaseUrl || baseUrl.isNotEmpty()) &&
                !(baseUrl.startsWith("https://") || baseUrl.startsWith("http://"))
            ) {
                CommonToast.getInstance().showToast(com.hive.i8n.R.string.ai_base_url_invalid)
                return@setOnClickListener
            }
            if (requiresBaseUrl && baseUrl.isEmpty()) {
                CommonToast.getInstance().showToast(com.hive.i8n.R.string.ai_base_url_required)
                return@setOnClickListener
            }

            manager?.setProviderApiKey(providerId, apiKey)
            if (supportsBaseUrl) {
                if (baseUrl.isNotEmpty()) {
                    manager?.setProviderBaseUrl(providerId, baseUrl)
                } else {
                    manager?.clearProviderBaseUrl(providerId)
                }
            }

            val providerInfo = manager?.getProviderInfo(providerId)
            val apiKeyPrefix = providerInfo?.apiKeyPrefix ?: ""

            if (manager?.hasValidApiKey(providerId, apiKeyPrefix) != true) {
                manager?.clearProviderApiKey(providerId)
                val errorMessage = providerInfo?.apiKeyValidateMsg
                    ?: getString(com.hive.i8n.R.string.ai_api_key_format_error)
                CommonToast.getInstance().showToast(errorMessage)
                return@setOnClickListener
            }

            manager.enableProvider(providerId)

            aiServiceManager =
                XAgent.getInstance().getAIServiceManager() as? DefaultAIServiceManager
            GlobalScope.launch(Dispatchers.IO) {
                aiServiceManager?.loadDefaultInferenceModelIfNeeded(providerId)
                withContext(Dispatchers.Main) {
                    aiSettingsHeader?.updateStatus()
                }
            }

            loadProviderData()

            CommonToast.getInstance().showToastLong(
                if (supportsBaseUrl && baseUrl.startsWith("http://")) {
                    getString(com.hive.i8n.R.string.ai_base_url_http_warning)
                } else {
                    getString(com.hive.i8n.R.string.ai_api_key_saved, providerName)
                }
            )
            dialog.dismiss()
        }

        btnClear.setOnClickListener {
            manager?.clearProviderApiKey(providerId)
            if (supportsBaseUrl) manager?.clearProviderBaseUrl(providerId)

            manager?.let { serviceManager ->
                if (serviceManager.isProviderEnabled(providerId)) {
                    serviceManager.disableProvider(providerId)
                }
            }

            aiServiceManager =
                XAgent.getInstance().getAIServiceManager() as? DefaultAIServiceManager
            loadProviderData()

            CommonToast.getInstance().showToast(
                getString(com.hive.i8n.R.string.ai_api_key_cleared, providerName)
            )
            dialog.dismiss()
        }

        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.show()
    }

    private fun toggleProviderEnabled(providerId: String, enabled: Boolean) {
        aiServiceManager?.let { manager ->
            if (enabled) {

                manager.enableProvider(providerId)
            } else {
                manager.disableProvider(providerId)
            }

            // 刷新列表数据
            scope.launch {
                loadProviderData()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        // 更新设置头部显示的模型信息
        aiSettingsHeader?.updateStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        aiSettingsHeader?.onActivityResult(requestCode, resultCode, data)
    }

    override fun getLayoutId(): Int = R.layout.agent_ai_provider_manager
}
