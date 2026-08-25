// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptSpanParamTextView
import com.hive.utils.extends.string
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/11/21
 */
class DialogCommonPairValueInput(context: Context?) : BaseScriptDialog(context) {

    private var mapValue = mutableMapOf<String, String>()
    private var commonListener: OnCommonListener? = null
    private var btn_submit: View? = null
    private var iv_close: View? = null
    private var layoutList: ViewGroup? = null
    private var tvAdd: View? = null
    private var tv_title: TextView? = null
    override fun initWindow() {
        iv_close = findViewById(R.id.iv_close)
        btn_submit = findViewById(R.id.btn_submit)
        layoutList = findViewById(R.id.layoutList)
        tvAdd = findViewById(R.id.tvAdd)
        tv_title = findViewById(R.id.tv_title)

        iv_close?.setOnClickListener {
            dismiss()
            commonListener?.onCanceled()
        }
        btn_submit?.setOnClickListener {
            try {
                checkInput()
                commonListener?.onSubmitted(mapValue)
                dismiss()
            } catch (e: java.lang.Exception) {
                CommonToast.show(e.message)
            }
        }
        tvAdd?.setOnClickListener {
            addItemView(null)
        }
    }

    data class PairValue(var key: String, var value: String)

    private fun checkInput() {
        mapValue.clear()
        for (i in 0 until layoutList?.childCount!!) {
            val itemView = layoutList?.getChildAt(i)
            val tagPair = itemView?.tag as PairValue
            if (tagPair.key.isEmpty() || tagPair.value.isEmpty()) {
                continue
            }
            mapValue[tagPair.key] = tagPair.value
        }
    }

    fun setTitle(title: String): DialogCommonPairValueInput {
        tv_title?.text = title
        return this
    }

    fun setOnCommonListener(callback: OnCommonListener): DialogCommonPairValueInput {
        commonListener = callback
        return this
    }

    fun setMapData(map: Map<String, String>): DialogCommonPairValueInput {
        mapValue = map.toMutableMap()
        for (entry in map) {
            addItemView(PairValue(entry.key, entry.value))
        }
        return this
    }

    private fun addItemView(pair: PairValue?) {
        val itemView =
            LayoutInflater.from(context).inflate(R.layout.item_common_pair_value_input, null)
        if (pair == null) {
            itemView.tag = PairValue("", "")
        } else {
            itemView.tag = pair
        }
        val tagPair = itemView.tag as PairValue
        val leftValue = itemView.findViewById<ScriptSpanParamTextView>(R.id.leftValue)
        val rightValue = itemView.findViewById<ScriptSpanParamTextView>(R.id.rightValue)
        itemView.findViewById<View>(R.id.ivDelete).setOnClickListener {
            layoutList?.removeView(itemView)
        }
        leftValue.setOnClickListener {
            DialogCommonTextInput(context).setSingleLine(true).setTitle(
                com.hive.i8n.R.string.sc_curl_param_key_edit_text_input_title.string()
            ).setHint(com.hive.i8n.R.string.sc_curl_param_key_edit_text_input_hint.string())
                .setText(tagPair.key)
                .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        itemView?.post {
                            tagPair.key = content
                            leftValue.setSpanText(content)
                        }
                    }

                    override fun onCanceled() {
                    }
                }).show()
        }

        rightValue.setOnClickListener {
            DialogCommonTextInput(context).setSingleLine(true).setTitle(
                com.hive.i8n.R.string.sc_curl_param_value_edit_text_input_title.string()
            ).setHint(com.hive.i8n.R.string.sc_curl_param_value_edit_text_input_hint.string())
                .setText(tagPair.value)
                .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        itemView?.post {
                            tagPair.value = content
                            rightValue.setSpanText(content)
                        }
                    }

                    override fun onCanceled() {
                    }
                }).show()
        }
        layoutList?.addView(itemView)
        itemView?.post {
            leftValue?.setSpanText(tagPair.key.ifEmpty { com.hive.i8n.R.string.sc_curl_edit_text_empty.string() })
            rightValue?.setSpanText(tagPair.value.ifEmpty { com.hive.i8n.R.string.sc_curl_edit_text_empty.string() })
            itemView.requestLayout()
        }
    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.dialog_common_pair_value_input

    interface OnCommonListener {
        fun onSubmitted(map: Map<String, String>)

        fun onCanceled()
    }
}