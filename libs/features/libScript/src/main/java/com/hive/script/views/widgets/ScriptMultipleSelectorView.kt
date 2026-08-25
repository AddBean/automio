// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView

class ScriptMultipleSelectorView(context: Context?, attrs: AttributeSet?) :
    BaseLayout(context, attrs), IListRecyclerViewFactory {

    var onSelectorItemClickListener: OnSelectorItemClickListener? = null

    private var dataSet = mutableListOf<Pair<String, String>>()

    private var listRecyclerView: ListRecyclerView? = null
    private var tvAdd: View? = null

    override fun initView(view: View?) {
        tvAdd = findViewById(R.id.tvAdd)
        listRecyclerView = findViewById(R.id.listRecyclerView)
        listRecyclerView?.setItemViewFactory(this)
        listRecyclerView?.layoutManager =
            GridLayoutManager(context, 3)
        tvAdd?.setOnClickListener {
            onSelectorItemClickListener?.onSelectorRequestAdd()
        }
    }

    fun loadDataSet(list: List<Pair<String, String>>) {
        dataSet.clear()
        dataSet.addAll(list)
        dataSet = dataSet.distinctBy { it.second }.toMutableList()
        listRecyclerView?.submitDataSets(dataSet)
        listRecyclerView?.notifyDataSetChanged()
    }

    fun reload() {
        listRecyclerView?.submitDataSets(dataSet)
        listRecyclerView?.notifyDataSetChanged()
        onSelectorItemClickListener?.onSelectorChanged()
    }

    fun getDataSet(): List<Pair<String, String>> {
        return dataSet
    }


    fun addData(data: Pair<String, String>) {
        dataSet.add(data)
        //去除重复
        dataSet = dataSet.distinctBy { it.second }.toMutableList()
        reload()
    }

    override fun createItemView(viewType: Int): ListRecyclerItemView =
        object : ListRecyclerItemView(context) {

            val itemView = LayoutInflater.from(context)
                .inflate(R.layout.script_multiple_selector_item_view, this)

            val ivDelete = itemView.findViewById<View>(R.id.tv_selector_name)

            var itemValue: Pair<String, String>? = null

            init {
                ivDelete?.setOnClickListener {
                    dataSet.remove(itemValue)
                    reload()
                }
            }

            override fun bindData(data: Any?) {
                val tvValue = itemView.findViewById<TextView>(R.id.tv_selector_name)
                itemValue = data as Pair<String, String>
                tvValue.text = itemValue?.first
            }
        }

    override fun getLayoutId() = R.layout.script_multiple_selector_view

    interface OnSelectorItemClickListener {

        fun onSelectorChanged()

        fun onSelectorRequestAdd() {

        }
    }
}