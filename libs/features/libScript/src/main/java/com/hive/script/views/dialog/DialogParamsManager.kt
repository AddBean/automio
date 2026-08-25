// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.base.params.ScriptParam
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptTabListView
import com.hive.utils.extends.string
import com.hive.utils.utils.DeviceCompatHelper
import com.hive.views.fragment.PagerTag
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory

class DialogParamsManager(context: Context?) : BaseScriptDialog(context), IListRecyclerViewFactory {
    private var readable = false

    private var writable = false

    private var systemOnly = false

    private var listener: OnParamListener? = null
    private var iv_close: View? = null
    private var tvAddParam: View? = null
    private var tabListView: ScriptTabListView? = null

    override fun initWindow() {
        iv_close = findViewById(R.id.iv_close)
        tvAddParam = findViewById(R.id.tvAddParam)
        tabListView = findViewById(R.id.tabListView)
        iv_close?.setOnClickListener {
            dismiss()
        }
        tvAddParam?.setOnClickListener {
            DialogParamsEdit(context).setCallback {
                updateTab()
            }.show()

        }
        updateTab()
    }

    private fun updateTab() {
        post {
            tabListView?.clearTab()
            tabListView?.setLayoutManagerFactory(object : ScriptTabListView.ILayoutManagerFactory {
                override fun createLayoutManager(pageTag: PagerTag): RecyclerView.LayoutManager {
                    return if (pageTag.name == com.hive.i8n.R.string.sc_params_group_sys.string()) {
                        LinearLayoutManager(context)
                    } else {
                        GridLayoutManager(context, 2)
                    }
                }
            })
            ScriptParamEnv.getParamEnv().getGroups().filter {
                if (systemOnly) {
                    it.id == "sys"
                } else {
                    true
                }
            }.forEach {
                val list = it.params.filter {
                    if (readable) {
                        it.readable
                    } else if (writable) {
                        it.writable
                    } else {
                        true
                    }
                }
                    .toMutableList()
                tabListView?.addTabWithType(
                    it.name,
                    list.map { param -> viewTypeByGroup(param.groupId) to param }.toList(),
                    this
                )
            }
            post {
                tabListView?.notifyDataSetChanged()
                if (ScriptParamEnv.getParamEnv().getGroups().size > 1) {
                    tabListView?.setCurrentTab(1)
                }
            }
        }
    }

    private fun viewTypeByGroup(group: String): Int {
        return if (group == "sys") 0 else 1
    }

    fun setParamListener(listener: OnParamListener): DialogParamsManager {
        this.listener = listener
        return this
    }

    fun setReadable(readable: Boolean): DialogParamsManager {
        this.readable = readable
        return this
    }

    fun setWritable(writable: Boolean): DialogParamsManager {
        this.writable = writable
        return this
    }

    fun setSystemOnly(systemOnly: Boolean): DialogParamsManager {
        this.systemOnly = systemOnly
        return this
    }

    override fun createItemView(viewType: Int): ListRecyclerItemView {
        if (viewType == 0) {
            return ParamSysCard()
        } else {
            return ParamCard()
        }
    }

    override fun getMarginParams() =
        arrayOf(0, if (DeviceCompatHelper.isLandscape()) 0 else 160 * DP, 0, 0)

    override fun getWindowLayoutId() = R.layout.dialog_params_manager

    interface OnParamListener {
        fun onParamSelected(param: ScriptParam?)
    }

    inner class ParamCard : ListRecyclerItemView(context) {
        private var param: ScriptParam? = null
        private var editIcon: View? = null
        private var viewBg: View? = null
        private var editSysIcon: View? = null
        private var paramSysType: TextView? = null
        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_params_item, this).apply {
            editIcon = findViewById(R.id.editIcon)
            viewBg = findViewById(R.id.viewBg)
            editSysIcon = findViewById(R.id.editSysIcon)
            paramSysType = findViewById(R.id.paramSysType)
            setOnClickListener {
                if (listener != null) {
                    dismiss()
                    listener?.onParamSelected(param)
                } else {
                    DialogParamsEdit(context)
                        .setTitle(com.hive.i8n.R.string.sc_param_manager_edit_title.string())
                        .setEditParams(param!!)
                        .setCallback {
                            updateTab()
                        }.show()
                }

            }
            editIcon?.setOnClickListener {
                DialogParamsEdit(context)
                    .setTitle(com.hive.i8n.R.string.sc_param_manager_edit_title.string())
                    .setEditParams(param!!)
                    .setCallback {
                        updateTab()
                    }.show()
            }
        }

        override fun bindData(data: Any?) {
            val tvParamName = layout.findViewById<TextView>(R.id.tvParamName)
            param = (data as ScriptParam)
            viewBg?.setBackgroundColor(param?.getColor() ?: 0)
            tvParamName.text = param?.name
            editSysIcon?.visibleOrGone(param?.writable == true && param?.readable == true)
            paramSysType?.visibleOrGone(param?.writable == false || param?.readable == false)
            paramSysType?.text =
                if (param?.writable == true) com.hive.i8n.R.string.param_write_only.string() else com.hive.i8n.R.string.param_read_only.string()
        }
    }

    inner class ParamSysCard : ListRecyclerItemView(context) {
        private var param: ScriptParam? = null
        private var editIcon: View? = null
        private var viewBg: View? = null
        private var editSysIcon: View? = null
        private var paramSysType: TextView? = null
        private var paramSysDes: TextView? = null
        private var tvParamSysName: TextView? = null
        private var viewSysBg: View? = null
        val layout =
            LayoutInflater.from(context).inflate(R.layout.dialog_params_sys_item, this).apply {
                editIcon = findViewById(R.id.editIcon)
                viewBg = findViewById(R.id.viewBg)
                editSysIcon = findViewById(R.id.editSysIcon)
                paramSysType = findViewById(R.id.paramSysType)
                paramSysDes = findViewById(R.id.paramSysDes)
                tvParamSysName = findViewById(R.id.tvParamSysName)
                viewSysBg = findViewById(R.id.viewSysBg)
                setOnClickListener {
                    if (listener != null) {
                        dismiss()
                        listener?.onParamSelected(param)
                    } else {
                        DialogParamsEdit(context)
                            .setTitle(com.hive.i8n.R.string.sc_param_manager_edit_title.string())
                            .setEditParams(param!!)
                            .setCallback {
                                updateTab()
                            }.show()
                    }

                }
                editSysIcon?.setOnClickListener {
                    DialogParamsEdit(context)
                        .setTitle(com.hive.i8n.R.string.sc_param_manager_edit_title.string())
                        .setEditParams(param!!)
                        .setCallback {
                            updateTab()
                        }.show()
                }
            }

        override fun bindData(data: Any?) {
            param = (data as ScriptParam)
            viewSysBg?.setBackgroundColor(param?.getColor() ?: 0)
            tvParamSysName?.text = param?.name

            editSysIcon?.visibleOrGone(param?.writable == true && param?.readable == true)
            paramSysType?.visibleOrGone(param?.writable == false || param?.readable == false)
            paramSysType?.text =
                if (param?.writable == true) com.hive.i8n.R.string.param_write_only.string() else com.hive.i8n.R.string.param_read_only.string()
            paramSysDes?.text = param?.desc
        }
    }
}

