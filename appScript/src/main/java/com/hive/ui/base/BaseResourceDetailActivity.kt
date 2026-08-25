// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.base

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import com.hive.app.script.R
import com.hive.base.BaseFragmentActivity

/**
 * 资源详情页抽象基类
 * 统一技能、工具、工作流等资源的详情页结构
 */
abstract class BaseResourceDetailActivity<T> : BaseFragmentActivity() {

    // 共同视图引用
    protected var tvName: TextView? = null
    protected var tvId: TextView? = null
    protected var tvTypeBadge: TextView? = null
    protected var tvDescription: TextView? = null
    protected var layoutActions: View? = null
    protected var ivIcon: ImageView? = null
    protected var btnEdit: TextView? = null
    protected var btnRun: TextView? = null

    // 溢出菜单 popup
    protected var moreMenuPopup: PopupWindow? = null

    // 当前数据
    protected var currentData: T? = null

    override fun doOnCreate(savedState: Bundle?) {
        currentData = loadDataFromIntent()
        if (currentData == null) {
            finish()
            return
        }
        bindViews()
        render(currentData!!)
        bindActions()
    }

    override fun onDestroy() {
        moreMenuPopup?.dismiss()
        moreMenuPopup = null
        super.onDestroy()
    }

    /**
     * 从 Intent 加载数据
     */
    protected abstract fun loadDataFromIntent(): T?

    /**
     * 绑定视图
     */
    protected abstract fun bindViews()

    /**
     * 渲染数据到视图
     */
    protected abstract fun render(data: T)

    /**
     * 绑定操作事件
     */
    protected abstract fun bindActions()

    /**
     * 判断是否自定义资源（默认 false，子类可覆盖）
     */
    protected open fun isCustom(): Boolean = false

    /**
     * 执行删除操作
     */
    protected abstract fun performDelete()

    /**
     * 绑定通用视图（子类可调用此方法减少重复 findViewById）
     */
    protected fun bindCommonViews() {
        tvName = findViewById(R.id.tv_name)
        tvId = findViewById(R.id.tv_id)
        tvTypeBadge = findViewById(R.id.tv_type_badge)
        tvDescription = findViewById(R.id.tv_description)
        layoutActions = findViewById(R.id.layout_actions)
        btnEdit = findViewById(R.id.btn_edit)
        btnRun = findViewById(R.id.btn_run)
    }

    /**
     * 显示删除确认对话框
     */
    protected fun showDeleteConfirm(resourceName: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(com.hive.i8n.R.string.agent_tool_delete_title))
            .setMessage(getString(com.hive.i8n.R.string.agent_tool_delete_message, resourceName))
            .setNegativeButton(getString(com.hive.i8n.R.string.cancel), null)
            .setPositiveButton(getString(com.hive.i8n.R.string.delete)) { _, _ ->
                performDelete()
            }
            .show()
    }

    /**
     * 更新操作按钮可见性（子类在 render() 中调用）
     * - 自定义资源：底部编辑+运行栏显示
     * - 内置资源：隐藏
     */
    protected fun updateActionsVisibility() {
        layoutActions?.visibility = if (isCustom()) View.VISIBLE else View.GONE
    }

    /**
     * 生成复制名称（避免与已有名称冲突）
     */
    protected fun generateCopyName(baseName: String, exists: (String) -> Boolean): String {
        var candidate = "$baseName (Copy)"
        var suffix = 2
        while (exists(candidate)) {
            candidate = "$baseName (Copy $suffix)"
            suffix++
        }
        return candidate
    }

    /**
     * dp 转 px
     */
    protected fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
