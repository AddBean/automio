// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.hive.script.R
import com.hive.script.utils.ScriptPermissionManager
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.GlobalApp
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView

/**
 * 权限聚合弹窗：列出当前缺失的权限，每项右侧「去打开」按钮；
 * 点击「去打开」后关闭弹窗，用户需自行返回后重新执行。
 *
 * 注意：libBase 中也有一个 DialogPermissionAggregate（com.hive.permissions.DialogPermissionAggregate），
 * 用于 PermissionsChecker 的通用权限提示，跳转到应用设置页。
 * 此版本用于脚本场景，支持特殊权限（无障碍、通知监听、屏幕录制等），根据权限类型跳转到不同设置页。
 */
class DialogPermissionAggregate(
    context: Context,
    missed: List<Pair<String, String>>,
) : BaseScriptDialog(context), IListRecyclerViewFactory {

    private val missedList: List<Pair<String, String>> = missed

    private var listRecyclerView: ListRecyclerView? = null
    private var tvBtnCancel: TextView? = null

    override fun initWindow() {
        listRecyclerView = findViewById(R.id.listRecyclerView)
        tvBtnCancel = findViewById(R.id.tvBtnCancel)
        listRecyclerView?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listRecyclerView?.setItemViewFactory(this)
        tvBtnCancel?.setOnClickListener { dismiss() }
        listRecyclerView?.post {
            listRecyclerView?.submitDataSets(missedList.map { it as Any })
        }
    }

    override fun createItemView(viewType: Int): ListRecyclerItemView = PermissionItemView(context, this)

    override fun getWindowLayoutId(): Int = R.layout.dialog_permission_aggregate

    override fun enableFadeAnimation(): Boolean = true

    private class PermissionItemView(
        context: Context,
        private val dialog: DialogPermissionAggregate
    ) : ListRecyclerItemView(context) {

        private val tvDesc: TextView?
        private val btnGoOpen: TextView?

        init {
            LayoutInflater.from(context).inflate(R.layout.item_permission_aggregate, this, true)
            tvDesc = findViewById(R.id.tv_permission_desc)
            btnGoOpen = findViewById(R.id.btn_go_open)
        }

        override fun bindData(data: Any?) {
            val pair = data as? Pair<*, *> ?: return
            val description = pair.second as? String ?: return
            val permission = pair.first as? String ?: return
            tvDesc?.text = description
            btnGoOpen?.setOnClickListener {
                ScriptPermissionManager.openOrRequestPermission(permission, GlobalApp.getTopActivity())
                dialog.dismiss()
            }
        }
    }

}
