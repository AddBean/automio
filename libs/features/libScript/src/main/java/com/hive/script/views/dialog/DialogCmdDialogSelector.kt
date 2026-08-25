// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/19/21
 */
class DialogCmdDialogSelector(context: Context) : BaseScriptDialog(context) {

    private var onSelectListener: OnSelectListener? = null

    private var itemList = mutableListOf<Pair<Int, String>>()
    private var selectedItems = mutableSetOf<Int>()
    private var isMultiSelectMode = false

    private var layout_content: ViewGroup? = null
    private var tv_btn_cancel: TextView? = null
    private var tv_btn_confirm: TextView? = null
    private var tv_title: TextView? = null
    private var tv_countdown: TextView? = null
    private var countDown = -1
    private var hasCallbacked = false

    override fun initWindow() {
        layout_content = findViewById(R.id.layout_content)
        tv_btn_cancel = findViewById(R.id.tv_btn_cancel)
        tv_btn_confirm = findViewById(R.id.tv_btn_confirm)
        tv_title = findViewById(R.id.tv_title)
        tv_countdown = findViewById(R.id.tv_countdown)
        
        // 设置按钮点击效果
        tv_btn_cancel?.setOnClickListener {
            dismiss()
            onSelectListener?.onCancel()
        }
        
        tv_btn_confirm?.setOnClickListener {
            dismiss()
            if (isMultiSelectMode) {
                onSelectListener?.onMultiSelected(this@DialogCmdDialogSelector, selectedItems.toList(), selectedItems.map { itemList[it] })
            } else {
                // 单选模式下确认按钮不生效
                onSelectListener?.onCancel()
            }
        }
        
        // 根据模式更新UI
        updateUIForMode()
    }


    fun setTitle(title: String): DialogCmdDialogSelector {
        tv_title?.text = title
        return this
    }

    fun setTitle(id: Int): DialogCmdDialogSelector {
        tv_title?.setText(id)
        return this
    }

    fun setMultiSelectMode(multiSelect: Boolean): DialogCmdDialogSelector {
        isMultiSelectMode = multiSelect
        updateUIForMode()
        return this
    }
    
    private fun updateUIForMode() {
        if (isMultiSelectMode) {
            tv_btn_confirm?.visibility = android.view.View.VISIBLE
            val currentText = tv_title?.text?.toString() ?: ""
            val multiSelectSuffix = context.getString(com.hive.i8n.R.string.script_dialog_multi_select)
            if (!currentText.contains(multiSelectSuffix)) {
                tv_title?.text = currentText + multiSelectSuffix
            }
        } else {
            tv_btn_confirm?.visibility = android.view.View.GONE
            val currentText = tv_title?.text?.toString() ?: ""
            val multiSelectSuffix = context.getString(com.hive.i8n.R.string.script_dialog_multi_select)
            tv_title?.text = currentText.replace(multiSelectSuffix, "")
        }
    }

    fun setDataSet(ls: MutableList<Pair<Int, String>>): DialogCmdDialogSelector {
        itemList = ls
        selectedItems.clear()
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

    fun setCountDown(seconds: Int): DialogCmdDialogSelector {
        this.countDown = seconds
        tv_countdown?.text = "${seconds}S"
        tv_countdown?.visibility = if (seconds > 0) android.view.View.VISIBLE else android.view.View.GONE
        post {
            if (countDown > 0) {
                val c = countDown - 1
                if (c == 0) {
                    if (isShown && !hasCallbacked) {
                        hasCallbacked = true
                        onSelectListener?.onCancel()
                        dismiss()
                    }
                } else {
                    postDelayed({
                        setCountDown(c)
                    }, 1000)
                }
            }
        }
        return this
    }

    override fun onDismiss() {
        super.onDismiss()
        if (!hasCallbacked) {
            onSelectListener?.onCancel()
        }
    }

    fun setSelectListener(listener: OnSelectListener): DialogCmdDialogSelector {
        onSelectListener = listener
        return this
    }

    interface OnSelectListener {

        fun onSelected(dialog: DialogCmdDialogSelector, pos: Int, pair: Pair<Int, String>)

        fun onMultiSelected(dialog: DialogCmdDialogSelector, selectedPositions: List<Int>, selectedItems: List<Pair<Int, String>>)

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
        private var checkBox: CheckBox? = null

        var itemView =
            LayoutInflater.from(context).inflate(R.layout.dialog_cmd_dialog_selector_item, null).apply {
                checkBox = findViewById(R.id.checkbox)
                setOnClickListener {
                    if (isMultiSelectMode) {
                        // 多选模式：切换复选框状态
                        checkBox?.isChecked = !(checkBox?.isChecked ?: false)
                        if (checkBox?.isChecked == true) {
                            selectedItems.add(pos)
                        } else {
                            selectedItems.remove(pos)
                        }
                        // 添加触觉反馈
                        performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    } else {
                        // 单选模式：直接选中并关闭对话框
                        performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        dismiss()
                        onSelectListener?.onSelected(this@DialogCmdDialogSelector, pos, itemList[pos])
                    }
                }
            }

        fun bindData(pos: Int, txt: String) {
            this.pos = pos
            itemMsg = txt
            itemView.findViewById<TextView>(R.id.btn_tv).text = txt
            checkBox?.isChecked = false
            
            // 根据模式设置复选框可见性
            checkBox?.visibility = if (isMultiSelectMode) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    override fun getWindowLayoutId() = R.layout.dialog_script_cmd_dialog_selector
}