// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.permissions

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.hive.base.R
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView

/**
 * 权限聚合弹窗：列出当前缺失的权限，每项右侧「去打开」按钮；
 * 点击「去打开」后跳转到应用设置页。
 *
 * 注意：libScript 中也有一个 DialogPermissionAggregate（com.hive.script.views.dialog.DialogPermissionAggregate），
 * 用于脚本场景，支持特殊权限（无障碍、通知监听、屏幕录制等），根据权限类型跳转到不同设置页。
 * 此版本用于 PermissionsChecker 的通用权限提示，仅跳转到应用设置页。
 */
class DialogPermissionAggregate(
    context: Context,
    missedPermissions: List<Pair<String, String>>,
    private val onCancelCallback: (() -> Unit)? = null,
) : Dialog(context, com.hive.views.R.style.base_dialog), IListRecyclerViewFactory {

    private val missedList: List<Pair<String, String>> = missedPermissions

    private var listRecyclerView: ListRecyclerView? = null
    private var tvBtnCancel: TextView? = null

    init {
        setContentView(R.layout.dialog_permission_aggregate)
        listRecyclerView = findViewById(R.id.listRecyclerView)
        tvBtnCancel = findViewById(R.id.tvBtnCancel)
        listRecyclerView?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listRecyclerView?.setItemViewFactory(this)
        tvBtnCancel?.setOnClickListener {
            onCancelCallback?.invoke()
            dismiss()
        }
        listRecyclerView?.post {
            listRecyclerView?.submitDataSets(missedList.map { it as Any })
        }
    }

    override fun createItemView(viewType: Int): ListRecyclerItemView = PermissionItemView(context, this)

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
            tvDesc?.text = description
            btnGoOpen?.setOnClickListener {
                // 跳转到应用设置页
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
                dialog.dismiss()
            }
        }
    }
}