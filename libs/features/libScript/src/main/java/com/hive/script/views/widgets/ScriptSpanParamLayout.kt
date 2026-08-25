// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.content.Context
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.hive.base.BaseLayout
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.base.params.ScriptParam
import com.hive.script.views.dialog.DialogParamsManager
import com.hive.utils.extends.dp
import com.hive.utils.system.ClipboardUtil
import com.hive.utils.system.CommonUtils
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView
import com.hive.views.widgets.CommonToast

class ScriptSpanParamLayout(context: Context?, attrs: AttributeSet?) :
    BaseLayout(context, attrs) {

    private var editText: ScriptSpanParamEditView? = null

    private var llQuickFill: View? = null

    private var rvQuickFill: ListRecyclerView? = null

    private var tvAddParam: View? = null

    private var tvClean: View? = null

    private var tvCopy: View? = null

    private var tvFormat: View? = null

    private var tvPaste: View? = null

    private var tvQuickFill: TextView? = null

    private var scriptText: String? = null

    private var scriptHint: String? = null

    private var actionTitle: String? = null

    private var actionList: List<QuickAction>? = null

    private var actionMenuList = mutableListOf(
        ActionMenuType.Copy,
        ActionMenuType.Paste,
        ActionMenuType.Clean
    )

    init {
        val typedArray =
            context?.obtainStyledAttributes(attrs, R.styleable.ScriptTextParamsEditView)
        scriptHint = typedArray?.getString(R.styleable.ScriptTextParamsEditView_scriptHint)
        scriptText = typedArray?.getString(R.styleable.ScriptTextParamsEditView_scriptText)
        typedArray?.recycle()
    }

    override fun initView(view: View?) {
        editText = findViewById(R.id.editText)
        llQuickFill = findViewById(R.id.llQuickFill)
        rvQuickFill = findViewById(R.id.rvQuickFill)
        tvAddParam = findViewById(R.id.tvAddParam)
        tvClean = findViewById(R.id.tvClean)
        tvCopy = findViewById(R.id.tvCopy)
        tvFormat = findViewById(R.id.tvFormat)
        tvPaste = findViewById(R.id.tvPaste)
        tvQuickFill = findViewById(R.id.tvQuickFill)
        editText?.requestFocus()
        editText?.hint = scriptHint
        editText?.setSpanText(scriptText)
        tvAddParam?.setOnClickListener {
            DialogParamsManager(context)
                .setReadable(true)
                .setParamListener(object :
                    DialogParamsManager.OnParamListener {
                    override fun onParamSelected(param: ScriptParam?) {
                        editText?.insertParams(param)
                    }
                }).show()
        }
        tvClean?.setOnClickListener {
            editText?.setText("")
        }
        tvCopy?.setOnClickListener {
            ClipboardUtil.getInstance(context)
                .copyText("copy", editText?.text.toString())
            CommonToast.show(com.hive.i8n.R.string.sc_copy_success)
        }

        tvPaste?.setOnClickListener {
            editText?.requestFocus()
            CommonUtils.openKeyboard(editText)
            editText?.post {
                val data = ClipboardUtil.getInstance(editText?.context)
                    .getClipText(editText?.context) ?: ""
                if (!TextUtils.isEmpty(data)) {
                    editText?.setText(data)
                }
            }
        }

        tvFormat?.setOnClickListener {
            editText?.requestFocus()
            editText?.format()
        }
        post {
            updateActionMenuList()
            refreshQuickFill()
        }
    }

    private fun refreshQuickFill() {
        val hasQuickAction = !actionList.isNullOrEmpty()
        llQuickFill?.visibleOrGone(hasQuickAction)
        editText?.setPadding(
            editText?.paddingLeft ?: 0,
            editText?.paddingTop ?: 0,
            editText?.paddingRight ?: 0,
            if (hasQuickAction) 72.dp else 42.dp
        )
        if (hasQuickAction) {
            tvQuickFill?.text = actionTitle
            rvQuickFill?.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            rvQuickFill?.setItemViewFactory(object : IListRecyclerViewFactory {
                override fun createItemView(viewType: Int): ListRecyclerItemView {
                    return object : ListRecyclerItemView(context) {
                        val layout =
                            View.inflate(context, R.layout.script_quick_fill_item, this)

                        val tvAction = findViewById<TextView>(R.id.tvAction)

                        override fun bindData(data: Any?) {
                            val action = data as QuickAction
                            tvAction?.tag = action
                            tvAction?.text = action.name
                            this.setOnClickListener {
                                setText(action.value)
                            }
                        }
                    }
                }
            })
            rvQuickFill?.submitDataSets(actionList ?: emptyList())
        }
    }

    fun setFunctionInsertParam(enable: Boolean) {
        tvAddParam?.visibleOrGone(enable)
    }

    fun setHint(hint: String?) {
        editText?.hint = hint
    }

    fun setText(text: String?) {
        editText?.setSpanText(text)
    }

    fun setSingleLine(enable: Boolean) {
        editText?.isSingleLine = enable
    }

    fun setQuickAction(title: String, actions: List<QuickAction>) {
        actionTitle = title
        actionList = actions
        post {
            refreshQuickFill()
        }
    }

    fun setActionMenuList(actionMenuList: List<ActionMenuType>) {
        this.actionMenuList.clear()
        this.actionMenuList.addAll(actionMenuList)
        post {
            updateActionMenuList()
        }
    }

    private fun updateActionMenuList() {
        tvCopy?.visibleOrGone(actionMenuList.contains(ActionMenuType.Copy))
        tvPaste?.visibleOrGone(actionMenuList.contains(ActionMenuType.Paste))
        tvFormat?.visibleOrGone(actionMenuList.contains(ActionMenuType.Format))
        tvClean?.visibleOrGone(actionMenuList.contains(ActionMenuType.Clean))
    }


    fun getText(): String? {
        return editText?.text?.toString()
    }

    fun addTextChangedListener(watcher: ScriptTextWatcher?) {
        editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: android.text.Editable?) {
                watcher?.afterTextChanged(editText?.text?.toString())
            }
        })
    }

    override fun getLayoutId() = R.layout.script_text_params_edit_view

    interface ScriptTextWatcher {

        fun afterTextChanged(s: String?)
    }

    class QuickAction(val name: String, val value: String)

    enum class ActionMenuType {
        Copy,
        Paste,
        Format,
        Clean
    }
}