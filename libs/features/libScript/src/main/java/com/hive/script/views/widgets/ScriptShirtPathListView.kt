// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.hive.adapter.core.AbsCardItemView
import com.hive.adapter.core.CardItemData
import com.hive.adapter.core.ICardItemFactory
import com.hive.base.BaseListLayout
import com.hive.script.R

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2021/9/7
 */
class ScriptShirtPathListView(context: Context?, attrs: AttributeSet?) :
    BaseListLayout(context, attrs) {

    private val layoutManager = GridLayoutManager(context, 4)

    override fun doInitialize() {

    }

    override fun parseData(data: String?): MutableList<CardItemData> {
        return mutableListOf()
    }

    override fun getCardFactory() = object : ICardItemFactory<CardItemData, AbsCardItemView> {
        override fun createItemView(context: Context?, type: Int) =
            object : AbsCardItemView(context) {

                override fun initView(view: View?) {

                }

                override fun bindData(data: CardItemData?) {

                }

                override fun getLayoutId() = R.layout.script_shirt_path_list_view_item

            }

        override fun offerTypeCount() = 1

    }

    override fun getLayoutManager() = layoutManager

    override fun getRequestUrl() = null

    override fun getLayoutId() = R.layout.script_shirt_path_list_view

    override fun isLoadMoreEnable() = false

    override fun isRefreshEnable() = false

}