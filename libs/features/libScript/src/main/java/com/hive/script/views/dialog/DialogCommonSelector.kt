// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/19/21
 */
class DialogCommonSelector(context: Context) : BaseScriptDialog(context) {

    private var onSelectListener: OnSelectListener? = null

    private var itemList = mutableListOf<Pair<Int, String>>()

    private var layout_content: ViewGroup?=null
    private var  tv_btn_cancel: TextView?=null
    private var   tv_title: TextView?=null

    override fun initWindow() {
        layout_content = findViewById(R.id.layout_content)
        tv_btn_cancel = findViewById(R.id.tv_btn_cancel)
        tv_title = findViewById(R.id.tv_title)
        tv_btn_cancel?.setOnClickListener {
            dismiss()
            onSelectListener?.onCancel()
        }
    }


    fun setTitle(title: String): DialogCommonSelector {
        tv_title?.text = title
        return this
    }

    fun setTitle(id: Int): DialogCommonSelector {
        tv_title?.setText(id)
        return this
    }

    fun setDataSet(ls: MutableList<Pair<Int, String>>): DialogCommonSelector {
        itemList = ls
        layout_content?.removeAllViews()
        var pos = 0
        ls.forEach {
            if (it.first == -1) {
                var item = ItemTitleView()
                item.bindData(pos, it.second)
                layout_content?.addView(item.itemView)
            } else {
                var item = ItemView()
                item.bindData(pos, it.second)
                layout_content?.addView(item.itemView)
            }
            pos++
        }
        return this
    }

    override fun enableFadeAnimation() = true

    fun setSelectListener(listener: OnSelectListener): DialogCommonSelector {
        onSelectListener = listener
        return this
    }

    interface OnSelectListener {

        fun onSelected(dialog: DialogCommonSelector, pos: Int, pair: Pair<Int, String>)

        fun onCancel()
    }

    inner class ItemTitleView {

        var pos = 0

        var itemView =
            LayoutInflater.from(context).inflate(R.layout.dialog_common_selector_item_title, null)

        fun bindData(pos: Int, txt: String) {
            this.pos = pos
            itemView.findViewById<TextView>(R.id.btn_tv).text = txt
        }
    }

    inner class ItemView {
        var itemMsg: String? = null

        var pos = 0

        var itemView =
            LayoutInflater.from(context).inflate(R.layout.dialog_common_selector_item, null).apply {
                setOnClickListener {
                    onSelectListener?.onSelected(this@DialogCommonSelector, pos, itemList[pos])
                }
            }

        fun bindData(pos: Int, txt: String) {
            this.pos = pos
            itemMsg = txt
            itemView.findViewById<TextView>(R.id.btn_tv).text = txt
        }
    }

    override fun getWindowLayoutId() = R.layout.dialog_script_cmd_selector
}