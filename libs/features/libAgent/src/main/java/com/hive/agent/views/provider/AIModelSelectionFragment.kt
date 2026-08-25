// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.hive.agent.R
import com.hive.agent.XAgent
import com.hive.agent.ai.DefaultAIServiceManager
import com.hive.base.BaseActivity.RESULT_OK
import com.hive.base.BaseFragment
import com.hive.plugin.agent.InferenceType
import com.hive.plugin.agent.ModelInfo
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.ListRecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AIModelSelectionFragment : BaseFragment() {

    private var listRecyclerView: ListRecyclerView? = null
    private var layoutLoading: LinearLayout? = null
    private var layoutError: LinearLayout? = null
    private var tvLoadingMessage: TextView? = null
    private var tvErrorMessage: TextView? = null
    private var btnErrorRetry: Button? = null
    private var etModelSearch: EditText? = null
    private var aiServiceManager: DefaultAIServiceManager? = null

    private var inferenceType: InferenceType = InferenceType.TEXT
    /** 完整模型列表缓存，用于本地搜索过滤 */
    private var fullModelDataList: List<Pair<Int, Any?>> = emptyList()

    companion object {
        private const val ARG_INFERENCE_TYPE = "inference_type"

        fun newInstance(inferenceType: InferenceType): AIModelSelectionFragment {
            return AIModelSelectionFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_INFERENCE_TYPE, inferenceType.type)
                }
            }
        }
    }

    override fun initView() {
        inferenceType = InferenceType.parserType(arguments?.getInt(ARG_INFERENCE_TYPE, 0) ?: 0)

        listRecyclerView = view?.findViewById(R.id.listRecyclerView)
        layoutLoading = view?.findViewById(R.id.layoutLoading)
        layoutError = view?.findViewById(R.id.layoutError)
        tvLoadingMessage = view?.findViewById(R.id.tvLoadingMessage)
        tvErrorMessage = view?.findViewById(R.id.tvErrorMessage)
        btnErrorRetry = view?.findViewById(R.id.btnErrorRetry)
        etModelSearch = view?.findViewById(R.id.etModelSearch)

        tvLoadingMessage?.text = getString(com.hive.i8n.R.string.agent_model_loading)
        tvErrorMessage?.text = getString(com.hive.i8n.R.string.agent_model_error)
        btnErrorRetry?.text = getString(com.hive.i8n.R.string.agent_model_refresh)
        btnErrorRetry?.setOnClickListener { startLoadModelData() }

        setupListView()
        setupSearchBox()
        startLoadModelData()
    }

    private fun setupSearchBox() {
        etModelSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applySearchFilter(s?.toString()?.trim() ?: "")
            }
        })
    }

    private fun applySearchFilter(query: String) {
        if (fullModelDataList.isEmpty()) return
        val filtered = if (query.isEmpty()) {
            fullModelDataList
        } else {
            val lower = query.lowercase()
            val result = mutableListOf<Pair<Int, Any?>>()
            var i = 0
            while (i < fullModelDataList.size) {
                val (type, data) = fullModelDataList[i]
                if (type == AIModelSelectionViewFactory.TYPE_PROVIDER_HEADER) {
                    val pid = data as? String ?: ""
                    val sectionItems = mutableListOf<Pair<Int, Any?>>()
                    sectionItems.add(Pair(type, data))
                    i++
                    while (i < fullModelDataList.size && fullModelDataList[i].first == AIModelSelectionViewFactory.TYPE_MODEL) {
                        val model = fullModelDataList[i].second as? ModelInfo
                        if (model != null && modelMatchesQuery(model, lower)) {
                            sectionItems.add(fullModelDataList[i])
                        }
                        i++
                    }
                    if (sectionItems.size > 1) result.addAll(sectionItems)
                } else {
                    i++
                }
            }
            result
        }
        val androidPairList = filtered.map { android.util.Pair(it.first, it.second) }
        listRecyclerView?.submitDataSetsWithType(androidPairList)
        listRecyclerView?.notifyDataSetChanged()
    }

    private fun modelMatchesQuery(model: ModelInfo, lowerQuery: String): Boolean {
        return model.displayName.lowercase().contains(lowerQuery) ||
            model.modelId.lowercase().contains(lowerQuery)
    }

    private fun startLoadModelData() {
        GlobalScope.launch(Dispatchers.Main) {
            loadModelData()
        }
    }

    private fun showLoading() {
        listRecyclerView?.visibility = View.GONE
        layoutLoading?.visibility = View.VISIBLE
        layoutError?.visibility = View.GONE
    }

    private fun showContent() {
        listRecyclerView?.visibility = View.VISIBLE
        layoutLoading?.visibility = View.GONE
        layoutError?.visibility = View.GONE
    }

    private fun showError(detailMessage: String? = null) {
        listRecyclerView?.visibility = View.GONE
        layoutLoading?.visibility = View.GONE
        layoutError?.visibility = View.VISIBLE
        val mainMessage = getString(com.hive.i8n.R.string.agent_model_error)
        tvErrorMessage?.text = if (!detailMessage.isNullOrBlank()) {
            "$mainMessage\n$detailMessage"
        } else {
            mainMessage
        }
    }

    private fun setupListView() {
        listRecyclerView?.let { recyclerView ->
            recyclerView.setItemViewFactory(AIModelSelectionViewFactory(requireContext()))
            recyclerView.setOnItemEventListener(object : ListRecyclerItemView.OnItemEventListener {
                override fun onItemEvent(itemData: Any?, eventData: Any?) {
                    handleItemEvent(eventData)
                }
            })
        }
    }

    private suspend fun loadModelData() {
        withContext(Dispatchers.Main) { showLoading() }

        try {
            aiServiceManager = XAgent.getInstance().getAIServiceManager() as? DefaultAIServiceManager
            val manager = aiServiceManager
            if (manager == null) {
                withContext(Dispatchers.Main) {
                    showError(getString(com.hive.i8n.R.string.agent_unknown_error))
                }
                return
            }

            val providers = manager.getEnabledProviders()
            val modelDataList = mutableListOf<Pair<Int, Any?>>()

            providers.sortedByDescending { it.getProviderInfo().sortIndex }
                .filter { it.isProviderReady() }
                .forEach { provider ->
                    val pid = provider.getProviderInfo().name
                    val models = withContext(Dispatchers.IO) { provider.getModels() }

                    val filteredModels = if (inferenceType == InferenceType.IMAGE) {
                        models.filter { it.capabilities.supportsVision }
                    } else {
                        models
                    }

                    if (filteredModels.isNotEmpty()) {
                        modelDataList.add(Pair(AIModelSelectionViewFactory.TYPE_PROVIDER_HEADER, pid))
                        filteredModels.forEach { model ->
                            modelDataList.add(Pair(AIModelSelectionViewFactory.TYPE_MODEL, model))
                        }
                    }
                }

            withContext(Dispatchers.Main) {
                fullModelDataList = modelDataList
                showContent()
                applySearchFilter(etModelSearch?.text?.toString()?.trim() ?: "")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showError(e.message ?: getString(com.hive.i8n.R.string.agent_unknown_error))
            }
        }
    }

    private fun handleItemEvent(eventData: Any?) {
        when (eventData) {
            is Map<*, *> -> {
                val eventType = eventData["eventType"] as? String
                when (eventType) {
                    "select_model" -> {
                        val modelInfo = eventData["data"] as? ModelInfo
                        selectModel(modelInfo)
                    }
                }
            }
        }
    }

    private fun selectModel(modelInfo: ModelInfo?) {
        activity?.setResult(RESULT_OK, Intent().apply { putExtra("data", modelInfo) })
        activity?.finish()
    }

    override fun getLayoutId(): Int = R.layout.agent_ai_model_selection
} 