// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hive.script.R
import com.hive.script.ScriptProvider.Companion.getViewContext
import com.hive.script.base.ScriptCommand
import com.hive.script.base.params.ScriptParam
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.cmd.CmdScriptStart
import com.hive.script.views.dialog.DialogCmdDialogInput
import com.hive.script.views.dialog.DialogCmdDialogInput.InputItem
import com.hive.script.views.dialog.DialogCmdDialogInput.InputItemTypes
import com.hive.script.views.dialog.DialogCommonTextInput
import com.hive.script.views.dialog.DialogParamsManager
import com.hive.script.views.widgets.ScriptSpanParamTextView
import com.hive.utils.extends.string
import java.util.Collections
import com.hive.i8n.R as i8nR

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
class CmdScriptStartEditView(context: Context) : BaseCommandEditCard(context) {

    var cmd: CmdScriptStart? = null

    private var recyclerView: RecyclerView? = null
    private var tvAdd: View? = null
    private val items = mutableListOf<ParamData>()
    private lateinit var adapter: ParamAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    data class ParamData(
        var id: String = "",
        var name: String = "",
        var hint: String = "",
        var defaultValue: String = "",
        var required: Boolean = false,
        var paramIdType: Int = DialogCmdDialogInput.TYPE_TEXT // 默认为文本类型
    )

    override fun initView() {
        recyclerView = findViewById(R.id.recyclerView)
        tvAdd = findViewById(R.id.tvAdd)
        adapter = ParamAdapter()
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = adapter
        itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                Collections.swap(items, from, to)
                adapter.notifyItemMoved(from, to)
                updateCommand()
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
            override fun isLongPressDragEnabled() = false
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        tvAdd?.setOnClickListener {
            val input1 = InputItem(
                id = "paramId",
                i8nR.string.script_start_input_variable.string(),
                i8nR.string.script_start_select_variable.string(),
                true,
                DialogCmdDialogInput.TYPE_PARAM,
                ""
            )
            val input2 = InputItem(
                id = "paramName",
                i8nR.string.script_start_variable_name.string(),
                i8nR.string.script_start_variable_name_hint.string(),
                true,
                InputItemTypes.TYPE_TEXT,
                ""
            )

            val input3 = InputItem(
                id = "paramHint",
                i8nR.string.script_start_hint_text.string(),
                i8nR.string.script_start_hint_text_hint.string(),
                false,
                InputItemTypes.TYPE_TEXT,
                ""
            )

            val input4 = InputItem(
                id = "paramDefault",
                i8nR.string.script_start_default_value.string(),
                i8nR.string.script_start_default_value_hint.string(),
                false,
                InputItemTypes.TYPE_TEXT,
                ""
            )

            val input5 = InputItem(
                id = "paramRequire",
                i8nR.string.script_start_required.string(),
                i8nR.string.script_start_required_hint.string(),
                false,
                InputItemTypes.TYPE_SWITCH,
                "false"
            )

            val inputItems = mutableListOf(input1, input2, input3, input4, input5)
            val dialog = DialogCmdDialogInput(getViewContext())
            dialog.setTitle(i8nR.string.script_start_add_variable.string())
            dialog.setInputItems(inputItems)
            dialog.setInputListener(object : DialogCmdDialogInput.OnInputListener {
                override fun onConfirmed(
                    dialog: DialogCmdDialogInput, inputs: List<InputItem>
                ) {

                    fun findValue(id: String): String =
                        inputs.firstOrNull { it.id == id }?.value ?: ""

                    val paramId = findValue("paramId")
                    val paramName = findValue("paramName")
                    val paramHint = findValue("paramHint")
                    val paramDefault = findValue("paramDefault")
                    val paramRequired = findValue("paramRequire") == "true"

                    if (paramId.isNotEmpty() && paramName.isNotEmpty()) {
                        addItemView(
                            ParamData(
                                id = paramId,
                                name = paramName,
                                hint = paramHint,
                                defaultValue = paramDefault,
                                required = paramRequired,
                                paramIdType = input1.inputType
                            )
                        )
                    }
                }

                override fun onCancel() {
                    // Do nothing on cancel
                }
            })
            dialog.show()
        }
    }

    private fun addItemView(paramData: ParamData) {
        items.add(paramData)
        adapter.notifyItemInserted(items.lastIndex)
        updateCommand()
    }

    private fun showEditDialog(
        view: ScriptSpanParamTextView,
        paramData: ParamData,
        title: String,
        hint: String,
        value: String
    ) {

        DialogCommonTextInput(context)
            .setSingleLine(true)
            .setTitle(title)
            .setHint(hint)
            .setText(value)
            .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                override fun onSubmitted(content: String) {
                    if (content.isNotEmpty()) {
                        // 更新对应的值
                        when (view.id) {
                            R.id.tvParamName -> paramData.name = content
                            R.id.tvParamHint -> paramData.hint = content
                            R.id.tvParamDefault -> paramData.defaultValue = content
                        }
                        view.setSpanText(content)
                        updateCommand()
                    }
                }

                override fun onCanceled() {

                }
            }).show()
    }

    private fun updateCommand() {
        val params = mutableListOf<String>()
        val inputs = mutableListOf<String>()
        val hints = mutableListOf<String>()
        val defaults = mutableListOf<String>()
        val requires = mutableListOf<String>()

        items.forEach { paramData ->
            if (paramData.id.isEmpty() || paramData.name.isEmpty()) {
                return@forEach
            }

            params.add(paramData.id)
            inputs.add(paramData.name)
            hints.add(paramData.hint)
            defaults.add(paramData.defaultValue)
            requires.add(paramData.required.toString())
        }

        // 更新命令对象
        cmd?.dialogParams = params.joinToString("|")
        cmd?.dialogInputs = inputs.joinToString("|")
        cmd?.dialogHints = hints.joinToString("|")
        cmd?.dialogDefaults = defaults.joinToString("|")
        cmd?.dialogRequires = requires.joinToString("|")
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdScriptStart

        items.clear()

        val params = cmd?.dialogParams?.split("|") ?: emptyList()
        val inputs = cmd?.dialogInputs?.split("|") ?: emptyList()
        val hints = cmd?.dialogHints?.split("|") ?: emptyList()
        val defaults = cmd?.dialogDefaults?.split("|") ?: emptyList()
        val requires = cmd?.dialogRequires?.split("|") ?: emptyList()

        val minSize = minOf(params.size, inputs.size)
        for (i in 0 until minSize) {
            val paramId = params[i]
            val inputName = inputs[i]
            val hint = if (i < hints.size) hints[i] else ""
            val defaultValue = if (i < defaults.size) defaults[i] else ""
            val required = if (i < requires.size) requires[i].toBoolean() else false

            if (paramId.isNotEmpty() && inputName.isNotEmpty()) {
                addItemView(
                    ParamData(
                        id = paramId,
                        name = inputName,
                        hint = hint,
                        defaultValue = defaultValue,
                        required = required,
                        // 根据参数ID判断类型，如果以 ${开头，则为参数类型
                        paramIdType = if (paramId.startsWith("\${"))
                            DialogCmdDialogInput.TYPE_PARAM
                        else
                            DialogCmdDialogInput.TYPE_TEXT
                    )
                )
            }
        }
        adapter.notifyDataSetChanged()
        updateCommand()
    }

    override fun getEditContentId() = R.layout.cmd_script_start

    private inner class ParamAdapter : RecyclerView.Adapter<ParamAdapter.ParamViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParamViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_script_start_param, parent, false)
            return ParamViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ParamViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ParamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvParamId = itemView.findViewById<ScriptSpanParamTextView>(R.id.tvParamId)
            private val tvParamName = itemView.findViewById<ScriptSpanParamTextView>(R.id.tvParamName)
            private val tvParamHint = itemView.findViewById<ScriptSpanParamTextView>(R.id.tvParamHint)
            private val tvParamDefault = itemView.findViewById<ScriptSpanParamTextView>(R.id.tvParamDefault)
            private val switchRequired = itemView.findViewById<SwitchCompat>(R.id.switchRequired)
            private val ivDelete = itemView.findViewById<ImageView>(R.id.ivDelete)
            private val ivDragHandle = itemView.findViewById<ImageView>(R.id.ivDragHandle)
            private val layoutHeaderDrag = itemView.findViewById<View>(R.id.layoutHeaderDrag)

            fun bind(paramData: ParamData) {
                val parseParamsId = ScriptParamEnv.parseParamsId(paramData.id)
                tvParamId.setSpanText(ScriptParamEnv.getParam(parseParamsId)?.getFormatId())
                tvParamName.setSpanText(paramData.name)
                tvParamHint.setSpanText(paramData.hint)
                tvParamDefault.setSpanText(paramData.defaultValue)
                switchRequired.setOnCheckedChangeListener(null)
                switchRequired.isChecked = paramData.required

                tvParamId.setOnClickListener {
                    DialogParamsManager(context)
                        .setReadable(true)
                        .setParamListener(object : DialogParamsManager.OnParamListener {
                            override fun onParamSelected(param: ScriptParam?) {
                                param ?: return
                                paramData.id = param.getFullId()
                                notifyItemChanged(bindingAdapterPosition)
                                updateCommand()
                            }
                        }).show()
                }
                tvParamName.setOnClickListener {
                    showEditDialog(
                        tvParamName,
                        paramData,
                        i8nR.string.script_start_parameter_name.string(),
                        i8nR.string.script_start_parameter_name_hint.string(),
                        paramData.name
                    )
                }
                tvParamHint.setOnClickListener {
                    showEditDialog(
                        tvParamHint,
                        paramData,
                        i8nR.string.script_start_hint_text_edit.string(),
                        i8nR.string.script_start_hint_text_edit_hint.string(),
                        paramData.hint
                    )
                }
                tvParamDefault.setOnClickListener {
                    showEditDialog(
                        tvParamDefault,
                        paramData,
                        i8nR.string.script_start_default_value_edit.string(),
                        i8nR.string.script_start_default_value_edit_hint.string(),
                        paramData.defaultValue
                    )
                }
                switchRequired.setOnCheckedChangeListener { _, isChecked ->
                    paramData.required = isChecked
                    updateCommand()
                }
                ivDelete.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                    items.removeAt(position)
                    notifyItemRemoved(position)
                    updateCommand()
                }
                val dragTouchListener = View.OnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        itemTouchHelper.startDrag(this)
                    }
                    false
                }
                ivDragHandle.setOnTouchListener(dragTouchListener)
                layoutHeaderDrag.setOnTouchListener(dragTouchListener)
            }
        }
    }
}
