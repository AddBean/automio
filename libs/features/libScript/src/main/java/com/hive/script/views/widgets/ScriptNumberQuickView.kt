// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.hive.anim.AnimUtils
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView
import com.hive.views.widgets.CommonToast

class ScriptNumberQuickView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs),
    IListRecyclerViewFactory {
    private var dataSets: List<Pair<String, Any?>>? = null

    var onItemClickedListener: OnItemClickedListener? = null

    private var listRecyclerView: ListRecyclerView? = null

    override fun initView(view: View?) {
        listRecyclerView = findViewById(R.id.listRecyclerView)
        listRecyclerView?.setItemViewFactory(this)
        listRecyclerView?.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    fun setDataSets(dataSets: List<Pair<String, Any?>>) {
        this.dataSets = dataSets
        listRecyclerView?.submitDataSets(dataSets)
        listRecyclerView?.notifyDataSetChanged()
    }

    private fun onItemClicked(data: Pair<String, Any?>) {
        onItemClickedListener?.onItemClicked(data)
    }

    override fun createItemView(viewType: Int) = object : ListRecyclerItemView(context) {

        private var dataItem: Pair<String, Any?>? = null

        private val itemView =
            LayoutInflater.from(context).inflate(R.layout.script_number_quick_view_item, this)
                .apply {
                    setOnClickListener {
                        AnimUtils.scaleAnim(it)
                        CommonToast.show(com.hive.i8n.R.string.sc_already_fill)
                        onItemClicked(dataItem!!)
                    }
                }

        override fun bindData(data: Any?) {
            dataItem = data as Pair<String, Any?>
            val tvValue = itemView.findViewById<TextView>(R.id.tvValue)
            tvValue.text = dataItem?.first
        }

    }

    override fun getLayoutId() = R.layout.script_number_quick_view


    interface OnItemClickedListener {
        fun onItemClicked(data: Pair<String, Any?>)
    }
}