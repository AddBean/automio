// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.View
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
@Suppress("UNCHECKED_CAST")
class DialogCustomListSelector(context: Context) : BaseScriptDialog(context) {

    private var onSelectListener: OnSelectListener? = null

    private var itemList = mutableListOf<Pair<Int, Any>>()

    private var layout_content: ViewGroup? = null
    private var tv_btn_cancel: View? = null
    private var tv_title: TextView? = null

    override fun initWindow() {
        layout_content = findViewById(R.id.layout_content)
        tv_btn_cancel = findViewById(R.id.tv_btn_cancel)
        tv_title = findViewById(R.id.tv_title)
        tv_btn_cancel?.setOnClickListener {
            dismiss()
            onSelectListener?.onCancel()
        }
    }


    fun setTitle(title: String): DialogCustomListSelector {
        tv_title?.text = title
        return this
    }

    fun setTitle(id: Int): DialogCustomListSelector {
        tv_title?.setText(id)
        return this
    }

    fun getContainer() = layout_content

    fun setDataSet(
        dataSets: MutableList<Pair<Int, Any>>,
        onCreateView: (data: Pair<Int, Any>) -> View
    ): DialogCustomListSelector {
        itemList = dataSets
        layout_content?.removeAllViews()
        dataSets.forEachIndexed { index, it ->
            val itemView = onCreateView(it)
            itemView.setOnClickListener {
                dismiss()
                onSelectListener?.onSelected(this, index, it.tag as Pair<Int, Any>)
            }
            itemView.tag = it
            layout_content?.addView(itemView)
        }
        return this
    }

    override fun enableFadeAnimation() = true

    fun setSelectListener(listener: OnSelectListener): DialogCustomListSelector {
        onSelectListener = listener
        return this
    }

    abstract class OnSelectListener {

        abstract fun onSelected(dialog: DialogCustomListSelector, pos: Int, pair: Pair<Int, Any>)

        open fun onCancel() {}
    }

    override fun getWindowLayoutId() = R.layout.dialog_script_custom_list_selector
}