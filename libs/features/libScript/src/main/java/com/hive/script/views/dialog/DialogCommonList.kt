// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView

/**
 *
 * @author jiadou
 * @date 6/19/21
 */
class DialogCommonList(context: Context) : BaseScriptDialog(context), IListRecyclerViewFactory {

    private var onSelectListener: OnSelectListener? = null

    private var itemList = mutableListOf<Pair<Int, String>>()

    private var listRecyclerView: ListRecyclerView? = null
    private var tvBtnConfirm: TextView? = null
    private var tvTitle: TextView? = null


    override fun initWindow() {
        listRecyclerView = findViewById(R.id.listRecyclerView)
        tvBtnConfirm = findViewById(R.id.tvBtnConfirm)
        tvTitle = findViewById(R.id.tvTitle)
        tvBtnConfirm?.setOnClickListener {
            dismiss()
            val sortList = mutableListOf<Pair<Int, String>>()
            listRecyclerView?.getDataSets()?.forEach {
                sortList.add(Pair(it.first, it.second.toString()))
            }
            onSelectListener?.onConfirm(this, sortList)
        }

    }


    fun setTitle(title: String): DialogCommonList {
        tvTitle?.text = title
        return this
    }

    fun setTitle(id: Int): DialogCommonList {
        tvTitle?.setText(id)
        return this
    }

    fun setDataSet(
        ls: MutableList<Pair<Int, String>>
    ): DialogCommonList {
        itemList = ls
        setDataSet(ls, false, this)
        return this
    }

    fun setDataSet(
        ls: MutableList<Pair<Int, String>>,
        enableDrag: Boolean,
        factory: IListRecyclerViewFactory?
    ): DialogCommonList {
        itemList = ls
        listRecyclerView?.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listRecyclerView?.setEnableDrag(enableDrag)
        listRecyclerView?.setItemViewFactory(factory ?: this)
        listRecyclerView?.submitDataSetsWithType(ls.map { android.util.Pair(it.first, it.second) })
        listRecyclerView?.notifyDataSetChanged()
        return this
    }

    override fun enableFadeAnimation() = true

    fun setSelectListener(listener: OnSelectListener): DialogCommonList {
        onSelectListener = listener
        return this
    }

    abstract class OnSelectListener {
        open fun onSelected(dialog: DialogCommonList, pair: Pair<Int, String>) {}

        open fun onConfirm(dialog: DialogCommonList, sortList: MutableList<Pair<Int, String>>) {}
    }


    inner class ItemView : ListRecyclerItemView(context) {

        private var itemView =
            LayoutInflater.from(context).inflate(R.layout.dialog_common_selector_item, this).apply {
                setOnClickListener {
                    onSelectListener?.onSelected(
                        this@DialogCommonList,
                        Pair(0, itemData.toString())
                    )
                }
            }

        override fun bindData(data: Any?) {
            itemView.findViewById<TextView>(R.id.btn_tv).text =
                (itemData as String)
        }
    }

    override fun createItemView(viewType: Int) = ItemView()

    override fun getWindowLayoutId() = R.layout.dialog_script_common_list

}