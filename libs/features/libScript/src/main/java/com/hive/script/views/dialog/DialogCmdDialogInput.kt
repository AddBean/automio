// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.base.params.ScriptParam
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptValueView

/**
 * 多输入项对话框
 * 支持动态指定多输入项，且每项支持是否必填
 *
 * @author jiadou
 * @date 2024/12/19
 */
class DialogCmdDialogInput(context: Context) : BaseScriptDialog(context) {

    private var onInputListener: OnInputListener? = null

    private var inputItems = mutableListOf<InputItem>()
    private var inputViews = mutableListOf<InputItemView>()

    private var layout_content: ViewGroup? = null
    private var tv_btn_cancel: TextView? = null
    private var tv_btn_confirm: TextView? = null
    private var tv_title: TextView? = null
    private var tv_msg: TextView? = null
    private var tv_countdown: TextView? = null
    private var cb_remember: CheckBox? = null
    private var countDown = -1
    private var hasCallbacked = false

    private var rememberKey: String? = null
    private var onRememberSave: ((key: String, values: Map<String, String>) -> Unit)? = null
    private var rememberOptionVisible = false

    override fun initWindow() {
        layout_content = findViewById(R.id.layout_content)
        tv_btn_cancel = findViewById(R.id.tv_btn_cancel)
        tv_btn_confirm = findViewById(R.id.tv_btn_confirm)
        tv_title = findViewById(R.id.tv_title)
        tv_msg = findViewById(R.id.tv_msg)
        tv_countdown = findViewById(R.id.tv_countdown)
        cb_remember = findViewById(R.id.cb_remember)
        cb_remember?.visibility = if (rememberOptionVisible) View.VISIBLE else View.GONE
        cb_remember?.isChecked = true

        // 设置按钮点击效果
        tv_btn_cancel?.setOnClickListener {
            hasCallbacked = true
            dismiss()
            onInputListener?.onCancel()
        }

        tv_btn_confirm?.setOnClickListener {
            if (validateInputs()) {
                val inputs = inputItems.mapIndexed { index, item ->
                    val view = inputViews[index]
                    item.value = view.getInputValue()
                    item
                }
                val key = rememberKey
                if (key != null && (cb_remember?.isChecked == true)) {
                    val values = inputs
                        .filter { !it.id.isNullOrEmpty() && it.value.isNotEmpty() }
                        .associate { ScriptParamEnv.parseParamsId(it.id!!) to it.value }
                    if (values.isNotEmpty()) {
                        onRememberSave?.invoke(key, values)
                    }
                }
                hasCallbacked = true
                dismiss()
                onInputListener?.onConfirmed(this@DialogCmdDialogInput, inputs)
            }
        }
    }

    /**
     * 验证所有必填项
     */
    @SuppressLint("SetTextI18n")
    private fun validateInputs(): Boolean {
        for (i in inputItems.indices) {
            val item = inputItems[i]
            val view = inputViews[i]
            val value = view.getInputValue()

            if (item.required && TextUtils.isEmpty(value)) {
                tv_msg?.visibleOrGone(true)
                tv_msg?.text = "\"${item.label}\" cannot be empty"
                view.focusInput()
                return false
            }
        }
        return true
    }

    fun setTitle(title: String): DialogCmdDialogInput {
        tv_title?.text = title
        return this
    }

    fun setTitle(id: Int): DialogCmdDialogInput {
        tv_title?.setText(id)
        return this
    }

    /**
     * 设置输入项
     */
    fun setInputItems(items: List<InputItem>): DialogCmdDialogInput {
        inputItems.clear()
        inputItems.addAll(items)
        inputViews.clear()
        layout_content?.removeAllViews()

        items.forEach { item ->
            val itemView = InputItemView()
            itemView.bindData(item)
            layout_content?.addView(itemView.itemView)
            inputViews.add(itemView)
        }

        return this
    }

    /**
     * 添加单个输入项
     */
    fun addInputItem(item: InputItem): DialogCmdDialogInput {
        inputItems.add(item)
        val itemView = InputItemView()
        itemView.bindData(item)
        layout_content?.addView(itemView.itemView)
        inputViews.add(itemView)
        return this
    }

    /**
     * 获取所有输入值
     */
    fun getInputValues(): List<String> {
        return inputViews.map { it.getInputValue() }
    }

    /**
     * 设置输入监听器
     */
    fun setInputListener(listener: OnInputListener): DialogCmdDialogInput {
        onInputListener = listener
        return this
    }

    /**
     * 设置「是否记住」选项，用于 CmdScriptStart 场景
     * @param key 记忆存储的 key，为 null 时不显示 checkbox
     * @param onSave 用户确认且勾选记住时的保存回调
     */
    fun setRememberOption(
        key: String?,
        onSave: ((key: String, values: Map<String, String>) -> Unit)?
    ): DialogCmdDialogInput {
        rememberKey = key
        onRememberSave = onSave
        rememberOptionVisible = key != null
        cb_remember?.visibility = if (rememberOptionVisible) View.VISIBLE else View.GONE
        return this
    }

    fun setCountDown(seconds: Int): DialogCmdDialogInput {
        this.countDown = seconds
        tv_countdown?.text = "${seconds}S"
        tv_countdown?.visibility = if (seconds > 0) View.VISIBLE else View.GONE
        post {
            if (countDown > 0) {
                val c = countDown - 1
                if (c == 0) {
                    if (isShown && !hasCallbacked) {
                        hasCallbacked = true
                        onInputListener?.onCancel()
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
            onInputListener?.onCancel()
        }
    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.dialog_script_cmd_dialog_input

    /**
     * 输入项数据类
     */
    data class InputItem(
        val id: String? = null,
        val label: String,
        val hint: String = "Please enter $label",
        val required: Boolean = false,
        val inputType: Int = TYPE_TEXT,
        var defaultValue: String = "",
        var value: String = ""
    )

    companion object InputItemTypes {
        const val TYPE_TEXT = android.text.InputType.TYPE_CLASS_TEXT
        const val TYPE_NUMBER = android.text.InputType.TYPE_CLASS_NUMBER
        const val TYPE_PHONE = android.text.InputType.TYPE_CLASS_PHONE
        const val TYPE_PASSWORD =
            android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        /**
         * 参数类型输入，弹出参数选择对话框
         */
        const val TYPE_PARAM = -1

        /**
         * 开关类型输入，弹出开关选择对话框
         */
        const val TYPE_SWITCH = -2
    }

    /**
     * 输入监听器接口
     */
    interface OnInputListener {
        fun onConfirmed(dialog: DialogCmdDialogInput, inputs: List<InputItem>)
        fun onCancel()
    }

    /**
     * 输入项视图
     */
    inner class InputItemView {
        var item: InputItem? = null
        private var tv_label: TextView? = null
        private var tv_required: TextView? = null
        private var et_input: EditText? = null
        private var param_input: ScriptValueView? = null
        private var switch_input: Switch? = null

        var itemView =
            LayoutInflater.from(context).inflate(R.layout.dialog_cmd_dialog_input_item, null)
                .apply {
                    tv_label = findViewById(R.id.tv_label)
                    tv_required = findViewById(R.id.tv_required)
                    et_input = findViewById(R.id.et_input)
                    param_input = findViewById(R.id.param_input)
                    switch_input = findViewById(R.id.switch_input)
                    param_input?.setInputFullWidth(true)
                    param_input?.onMaskClickListener = OnClickListener {
                        DialogParamsManager(context)
                            .setSystemOnly(false)
                            .setWritable(true)
                            .setParamListener(object :
                                DialogParamsManager.OnParamListener {
                                override fun onParamSelected(param: ScriptParam?) {
                                    param_input?.setValue(param?.getFormatId() ?: "")
                                }
                            }).show()
                    }
                }

        fun bindData(item: InputItem) {
            this.item = item
            tv_label?.text = item.label
            tv_required?.visibility =
                if (item.required) VISIBLE else GONE
            param_input?.visibleOrGone(false)
            et_input?.visibleOrGone(false)
            switch_input?.visibleOrGone(false)
            when (item.inputType) {
                TYPE_SWITCH -> {
                    switch_input?.visibleOrGone(true)
                    switch_input?.isChecked = item.defaultValue.toBooleanStrictOrNull() ?: false
                }

                TYPE_PARAM -> {
                    param_input?.visibleOrGone(true)
                    param_input?.getTextView()?.hint = item.hint
                    if (item.defaultValue.isNotEmpty()) {
                        param_input?.setValue(item.defaultValue)
                    }
                }

                else -> {
                    et_input?.visibleOrGone(true)
                    et_input?.hint = item.hint
                    et_input?.inputType = item.inputType
                    if (item.defaultValue.isNotEmpty()) {
                        et_input?.setText(item.defaultValue)
                    }
                }
            }
        }

        fun getInputValue(): String {
            return when (item?.inputType) {
                InputItemTypes.TYPE_SWITCH -> {
                    switch_input?.isChecked?.toString() ?: "false"
                }

                InputItemTypes.TYPE_PARAM -> {
                    param_input?.getTextView()?.text?.toString()?.trim() ?: ""
                }

                else -> {
                    et_input?.text?.toString()?.trim() ?: ""
                }
            }
        }

        fun focusInput() {
            et_input?.requestFocus()
        }
    }
} 