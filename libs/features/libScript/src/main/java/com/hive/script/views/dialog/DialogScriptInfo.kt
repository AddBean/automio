// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.hive.script.R
import com.hive.script.extensions.getInfoMap
import com.hive.script.scope.ScriptScopeRepository
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView

/**
 *
 * @author jiadou
 * @date 2021/10/24
 */
class DialogScriptInfo(context: Context?) : BaseScriptDialog(context) {
    private var recycler_view: ListRecyclerView? = null
    private var tv_btn_cancel: TextView? = null
    private var tv_btn_scope: TextView? = null
    private var tv_title: TextView? = null
    private var loadedScript: ScriptInfoModel? = null

    override fun initWindow() {
        recycler_view = findViewById(R.id.recycler_view)
        tv_title = findViewById(R.id.tv_title)
        tv_btn_cancel = findViewById(R.id.tv_btn_cancel)
        tv_btn_scope = findViewById(R.id.tv_btn_scope)
        tv_btn_cancel?.setOnClickListener { dismiss() }
        tv_btn_scope?.setOnClickListener {
            val scriptPath = loadedScript?.scriptPath ?: return@setOnClickListener
            DialogScriptScopeManager(context).loadScript(scriptPath).show()
        }
    }

    fun loadScript(data: ScriptInfoModel): DialogScriptInfo {
        loadedScript = data
        tv_title?.text = data.scriptName
        recycler_view?.setItemViewFactory(object : IListRecyclerViewFactory {
            override fun createItemView(viewType: Int) =
                object : ListRecyclerItemView(context) {
                    init {
                        LayoutInflater.from(context)
                            .inflate(R.layout.dialog_script_info_item, this)
                    }

                    override fun bindData(data: Any?) {
                        val p = data as Pair<String, String>
                        findViewById<TextView>(R.id.tv_name).text = p.first
                        findViewById<TextView>(R.id.tv_info).text = p.second
                    }
                }
        })

        val dataSet = mutableListOf<Pair<String, String>>()
        data.scriptMate?.getInfoMap(data.scriptPath)?.forEach {
            dataSet.add(it.key to "" + it.value)
        }
        recycler_view?.submitDataSets(dataSet);
        val hasScope = data.scriptPath?.let {
            runCatching { ScriptScopeRepository.load(java.io.File(it), validate = false) }.getOrNull()
        } != null
        tv_btn_scope?.visibility = if (hasScope) View.VISIBLE else View.GONE
        return this
    }


    override fun getWindowLayoutId() = R.layout.dialog_script_info

}