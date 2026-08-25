// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.editor

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.hive.base.BaseLayout
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.views.edit.AbsListItemView
import com.hive.script.views.edit.DialogScriptEdit
import com.hive.script.views.edit.IListViewFactory
import com.hive.script.views.edit.ScriptEditFactory
import com.hive.script.views.edit.ScriptMenuEditHelper
import com.hive.script.views.edit.ScriptRecyclerView
import com.hive.script.views.manager.ScriptMenuManager

/**
 *
 * @author jiadou
 * @date 6/22/21
 */
class ListScriptEditView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs),
    IListViewFactory, AbsListItemView.OnItemEventListener {

    private lateinit var mCommand: ScriptCommand
    var dialogView: DialogScriptEdit? = null

    private var recycler_view: ScriptRecyclerView? = null
    private var btn_empty_add: View? = null

    override fun initView(view: View?) {
        recycler_view = findViewById(R.id.recycler_view)
        btn_empty_add = findViewById(R.id.btn_empty_add)
        ScriptMenuManager.hiddenMenuView()
        recycler_view?.setEnableDrag(true)
        recycler_view?.setItemViewFactory(this)
        btn_empty_add?.setOnClickListener {
            ScriptMenuEditHelper.showAddDialog(context, mCommand, -1, dialogView)
        }
    }

    fun submitData(command: ScriptCommand) {
        mCommand = command
        recycler_view?.submitData(mCommand.commandQueue)
        recycler_view?.notifyDataSetChanged()
        btn_empty_add?.visibleOrGone(command.commandQueue.isEmpty())
    }


    override fun createItemView(viewType: Int) =
        ScriptEditFactory.createItemView(this, context, viewType)

    override fun onItemEvent(itemData: Any?, eventData: Any?) {
        ScriptMenuEditHelper.handleMenuEdit(
            context,
            itemData as ScriptCommand,
            null,
            eventData as ScriptMenuEditHelper.ClickType,
            ScriptMenuEditHelper.InsertType.INSERT_AFTER,
            dialogView
        )
        recycler_view?.notifyDataSetChanged()
    }

    fun notifyData() {
        recycler_view?.notifyDataSetChanged()
        btn_empty_add?.visibleOrGone(mCommand.commandQueue.isEmpty())
    }


    override fun getLayoutId() = R.layout.script_edit_view_n


}