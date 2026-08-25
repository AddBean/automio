// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.utils.ScriptColorHelper
import com.hive.script.views.cards.ScriptColorCard
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView

class ScriptColorRecentView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs) {

    private var onColorSelectedListener: OnColorSelectedListener? = null

    var selectColor = 0

    private var recycler_view: ListRecyclerView? = null

    override fun initView(view: View?) {
        recycler_view = findViewById(R.id.recycler_view)
        recycler_view?.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recycler_view?.setItemViewFactory(object : IListRecyclerViewFactory {
            override fun createItemView(viewType: Int) =
                ScriptColorCard(context!!, this@ScriptColorRecentView).apply {
                    setOnClickListener {
                        selectColor = this.itemData as Int
                        onColorSelectedListener?.onSelected(selectColor)
                        setSelectedColor(selectColor)
                    }
                }
        })
        updateImageList()
    }

    private fun updateImageList() {
        recycler_view?.submitDataSets(
            ScriptColorHelper.getColorList()
        )
        recycler_view?.notifyDataSetChanged()
    }

    fun setOnColorSelectedListener(listener: OnColorSelectedListener?) {
        onColorSelectedListener = listener
    }

    fun setSelectedColor(color: Int) {
        selectColor = color
        recycler_view?.notifyDataSetChanged()
    }

    interface OnColorSelectedListener {
        fun onSelected(path: Int)
    }


    override fun getLayoutId() = R.layout.script_color_recent_view
}