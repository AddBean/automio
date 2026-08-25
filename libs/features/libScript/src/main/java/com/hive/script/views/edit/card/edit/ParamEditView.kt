// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import com.hive.base.BaseLayout
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.base.params.ScriptParam
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.views.dialog.DialogCommonSelector
import com.hive.script.views.widgets.ScriptSpanParamLayout
import com.hive.utils.GlobalApp
import com.hive.utils.utils.StringUtils

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
class ParamEditView(context: Context, attrs: AttributeSet?) : BaseLayout(context, attrs) {

    private var mParam: ScriptParam? = null

    var isEdit = false
    private var edGroup: EditText? = null
    private var edParam: EditText? = null
    private var edValue: ScriptSpanParamLayout? = null
    private var ivGroupSelector: ImageView? = null
    private var layoutGroup: View? = null
    private var lineGroup: View? = null

    override fun initView(view: View?) {
        edGroup = findViewById(R.id.edGroup)
        edParam = findViewById(R.id.edParam)
        edValue = findViewById(R.id.edValue)
        ivGroupSelector = findViewById(R.id.ivGroupSelector)
        layoutGroup = findViewById(R.id.layoutGroup)
        lineGroup = findViewById(R.id.lineGroup)
        edGroup?.hint = GlobalApp.getString(com.hive.i8n.R.string.sc_param_hint)
        edParam?.hint = GlobalApp.getString(com.hive.i8n.R.string.sc_param_name_hint)
        edValue?.setHint(GlobalApp.getString(com.hive.i8n.R.string.sc_param_value_hint))
        edValue?.setFunctionInsertParam(false)
        val hasCustomParamsGroup = ScriptParamEnv.getGroupList()
            .any { it.id != ScriptParamEnv.sysGroupId && it.id != ScriptParamEnv.mainGroupId }
        ivGroupSelector?.visibleOrGone(hasCustomParamsGroup)
        ivGroupSelector?.setOnClickListener {
            showParamGroupDialog()
        }
    }

    private fun showParamGroupDialog() {
        DialogCommonSelector(context).setTitle(GlobalApp.getString(com.hive.i8n.R.string.sc_param_group_list_title))
            .setDataSet(
                ScriptParamEnv.getGroupList()
                    .filter { it.id != ScriptParamEnv.sysGroupId && it.id != ScriptParamEnv.mainGroupId }
                    .map {
                        Pair(0, it.name)
                    }.toMutableList()
            ).setSelectListener(object : DialogCommonSelector.OnSelectListener {
                override fun onSelected(
                    dialog: DialogCommonSelector, pos: Int, pair: Pair<Int, String>
                ) {
                    edGroup?.setText(pair.second)
                    dialog.dismiss()
                }

                override fun onCancel() {
                }

            }).show()
    }

    fun setEditMode(isEdit: Boolean) {
        this.isEdit = isEdit
        layoutGroup?.visibleOrGone(false)
        lineGroup?.visibleOrGone(false)
        edParam?.isEnabled = !isEdit
    }

    fun bindParam(param: ScriptParam) {
        mParam = param
        edGroup?.setText(param.groupId)
        edParam?.setText(param.name)
        edValue?.setText(StringUtils.decoding(param.initValue))
    }

    fun checkOrThrowError() {
        mParam?.groupId = edGroup?.text.toString()
        if (mParam?.id.isNullOrEmpty()) {
            mParam?.id = edParam?.text.toString()
        }
        mParam?.name = edParam?.text.toString()
        mParam?.initValue = StringUtils.encoding(edValue?.getText().toString())
        mParam?.write(StringUtils.encoding(edValue?.getText().toString()))
        mParam?.let {

            if (it.id.isEmpty()) {
                throw IllegalArgumentException(GlobalApp.getString(com.hive.i8n.R.string.sc_param_id_empty))
            }
            if (it.name.isEmpty()) {
                throw IllegalArgumentException(GlobalApp.getString(com.hive.i8n.R.string.sc_param_name_empty))
            }
//            if (it.value.isEmpty()) {
//                throw IllegalArgumentException(GlobalApp.getString(com.hive.i8n.R.string.sc_param_value_empty))
//            }
            if (!ScriptCommandHelper.checkParamName(it.id)) {
                throw IllegalArgumentException(GlobalApp.getString(com.hive.i8n.R.string.sc_param_id_error))
            }

            if (!ScriptCommandHelper.checkParamName(it.groupId)) {
                throw IllegalArgumentException(GlobalApp.getString(com.hive.i8n.R.string.sc_param_group_id_error))
            }
        } ?: throw IllegalArgumentException(GlobalApp.getString(com.hive.i8n.R.string.sc_param_empty))
    }

    override fun getLayoutId() = R.layout.param_edit_view
}