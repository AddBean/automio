// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.params.ScriptParam
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.cmd.CmdScriptEnd
import com.hive.script.views.dialog.DialogCommonTextInput
import com.hive.script.views.dialog.DialogParamsManager
import com.hive.script.views.widgets.ScriptSpanParamTextView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string
import java.util.Collections
import com.hive.i8n.R as i8nR

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
class CmdScriptEndEditView(context: Context) : BaseCommandEditCard(context) {

    var cmd: CmdScriptEnd? = null
    private var recyclerView: RecyclerView? = null
    private var tvAdd: View? = null
    private val items = mutableListOf<ParamData>()
    private lateinit var adapter: ParamAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    
    // 参数数据类
    data class ParamData(
        var paramId: String = "",
        var content: String = "",
        var isFixed: Boolean = false
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
            showAddParamDialog()
        }
    }
    
    private fun showAddParamDialog() {
        DialogParamsManager(context)
            .setReadable(true)
            .setWritable(true)
            .setParamListener(object : DialogParamsManager.OnParamListener {
                override fun onParamSelected(param: ScriptParam?) {
                    param?.let {
                        val paramId = it.getFullId()
                        showContentInputDialog(paramId)
                    }
                }
            }).show()
    }
    
    private fun showContentInputDialog(paramId: String) {
        DialogCommonTextInput(context)
            .setSingleLine(false)
            .setTitle(i8nR.string.script_end_set_variable_value.string())
            .setHint(i8nR.string.script_end_enter_variable_value.string())
            .setText("")
            .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                override fun onSubmitted(content: String) {
                    if (content.isNotEmpty()) {
                        addItemView(ParamData(paramId, content))
                    }
                }

                override fun onCanceled() {
                    // 取消操作，不做处理
                }
            }).show()
    }
    
    private fun addItemView(paramData: ParamData) {
        items.add(paramData)
        adapter.notifyItemInserted(items.lastIndex)
        updateCommand()
    }
    
    private fun updateCommand() {
        val paramSettings = mutableListOf<CmdScriptEnd.ParamSetting>()

        items.forEach { paramData ->
            if (paramData.paramId.isEmpty()) {
                return@forEach
            }
            paramSettings.add(
                CmdScriptEnd.ParamSetting(
                    paramId = paramData.paramId,
                    content = paramData.content
                )
            )
        }
        
        cmd?.paramSettings?.clear()
        cmd?.paramSettings?.addAll(paramSettings)
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdScriptEnd
        items.clear()

        val fixedParamId = ScriptParamEnv.getOutputParam1()?.getFullId().orEmpty()
        val existingSettings = cmd?.paramSettings.orEmpty()
        if (existingSettings.isEmpty() && fixedParamId.isNotEmpty()) {
            items.add(ParamData(fixedParamId, "", true))
        } else {
            existingSettings.forEach { setting ->
                items.add(
                    ParamData(
                        paramId = setting.paramId,
                        content = setting.content,
                        isFixed = setting.paramId == fixedParamId
                    )
                )
            }
            if (fixedParamId.isNotEmpty() && items.none { it.paramId == fixedParamId }) {
                items.add(0, ParamData(fixedParamId, "", true))
            }
        }
        adapter.notifyDataSetChanged()
        updateCommand()
    }

    override fun getEditContentId() = R.layout.cmd_script_end

    private inner class ParamAdapter : RecyclerView.Adapter<ParamAdapter.ParamViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParamViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_script_end_param, parent, false)
            return ParamViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ParamViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ParamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvParamId = itemView.findViewById<ScriptSpanParamTextView>(R.id.tvParamId)
            private val tvParamValue = itemView.findViewById<ScriptSpanParamTextView>(R.id.tvParamValue)
            private val ivDelete = itemView.findViewById<ImageView>(R.id.ivDelete)
            private val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
            private val ivDragHandle = itemView.findViewById<ImageView>(R.id.ivDragHandle)
            private val layoutHeaderDrag = itemView.findViewById<View>(R.id.layoutHeaderDrag)

            fun bind(paramData: ParamData) {
                tvParamId.setSpanText(ScriptParamEnv.getParam(paramData.paramId)?.getFormatId())
                tvParamValue.setSpanText(paramData.content)

                if (paramData.isFixed) {
                    tvParamId.isEnabled = false
                    tvParamId.alpha = 0.6f
                    ivDelete.visibility = View.INVISIBLE
                    tvTitle.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary))
                    tvParamId.setOnClickListener(null)
                } else {
                    tvParamId.isEnabled = true
                    tvParamId.alpha = 1f
                    ivDelete.visibility = View.VISIBLE
                    tvTitle.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.tech_cyan))
                    tvParamId.setOnClickListener {
                        DialogParamsManager(context)
                            .setReadable(true)
                            .setWritable(true)
                            .setParamListener(object : DialogParamsManager.OnParamListener {
                                override fun onParamSelected(param: ScriptParam?) {
                                    param ?: return
                                    paramData.paramId = param.getFullId()
                                    notifyItemChanged(bindingAdapterPosition)
                                    updateCommand()
                                }
                            }).show()
                    }
                }

                tvParamValue.setOnClickListener {
                    DialogCommonTextInput(context)
                        .setSingleLine(false)
                        .setTitle(i8nR.string.script_end_set_variable_value.string())
                        .setHint(i8nR.string.script_end_enter_variable_value.string())
                        .setText(paramData.content)
                        .setOnCommonListener(object : DialogCommonTextInput.OnCommonListener {
                            override fun onSubmitted(content: String) {
                                if (content.isNotEmpty()) {
                                    paramData.content = content
                                    notifyItemChanged(bindingAdapterPosition)
                                    updateCommand()
                                }
                            }

                            override fun onCanceled() = Unit
                        }).show()
                }

                ivDelete.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                    items.removeAt(position)
                    notifyItemRemoved(position)
                    updateCommand()
                }
                ivDragHandle.visibility = if (paramData.isFixed) View.INVISIBLE else View.VISIBLE
                val dragTouchListener = View.OnTouchListener { _, event ->
                    if (!paramData.isFixed && event.actionMasked == MotionEvent.ACTION_DOWN) {
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
