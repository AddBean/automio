// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.list

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.hive.base.databinding.ViewLoadMoreDefaultFooterBinding
import com.hive.i8n.R
import com.hive.utils.extends.gone
import com.hive.utils.extends.string
import com.hive.utils.extends.visible

/**
 * @Desc: 默认的 LoadMoreView 实现
 * @Author: jiadou
 */
class DefaultLoadMoreFooterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseLoadMoreFooterView(context, attrs, defStyleAttr) {

    private var binding: ViewLoadMoreDefaultFooterBinding? = null

    private var loadingText: String = ""
    private var noMoreText: String = ""
    private var errorText: String = ""
    private var prepareText: String = ""

    init {
        loadingText = R.string.list_status_loading.string()
        noMoreText = R.string.list_no_more_data.string()
        prepareText = R.string.list_pull_up_to_load_more.string()
        errorText = R.string.list_status_load_failed.string()
        val inflater = LayoutInflater.from(context)
        binding = ViewLoadMoreDefaultFooterBinding.inflate(inflater, this, true)
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        updateTexts()
    }

    override fun updateState(state: LoadMoreState) {
        binding?.apply {
            when (state) {
                LoadMoreState.LOADING -> {
                    loadingView.visible()
                    loadingView.getChildAt(0).visible()
                    loadingTextView.text = loadingText
                    errorView.gone()
                    noMoreView.gone()
                }

                LoadMoreState.NO_MORE -> {
                    loadingView.gone()
                    errorView.gone()
                    noMoreView.visible()
                }

                LoadMoreState.ERROR -> {
                    loadingView.gone()
                    errorView.visible()
                    noMoreView.gone()
                }

                LoadMoreState.PREPARE -> {
                    loadingView.visible()
                    loadingView.getChildAt(0).gone()
                    loadingTextView.text = prepareText
                    errorView.gone()
                    noMoreView.gone()
                }

                LoadMoreState.IDLE -> {
                    loadingView.gone()
                    errorView.gone()
                    noMoreView.gone()
                }
            }
        }
    }

    /**
     * 设置加载中文案
     */
    fun setLoadingText(text: String) {
        loadingText = text
        updateTexts()
    }

    /**
     * 设置没有更多数据文案
     */
    fun setNoMoreText(text: String) {
        noMoreText = text
        updateTexts()
    }

    /**
     * 设置加载失败文案
     */
    fun setErrorText(text: String) {
        errorText = text
        updateTexts()
    }

    /**
     * 批量设置文案
     */
    fun setTexts(
        loading: String? = null,
        noMore: String? = null,
        error: String? = null,
    ) {
        loading?.let { loadingText = it }
        noMore?.let { noMoreText = it }
        error?.let { errorText = it }
        updateTexts()
    }

    /**
     * 更新所有文案
     */
    private fun updateTexts() {
        binding?.apply {
            loadingTextView.text = loadingText
            noMoreTextView.text = noMoreText
            errorTextView.text = errorText
        }
    }
}