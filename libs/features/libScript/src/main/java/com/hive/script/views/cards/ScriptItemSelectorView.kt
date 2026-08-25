// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.cards

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.hive.anim.AnimUtils
import com.hive.script.R
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.utils.GlobalApp
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.widgets.UIResourceIconView

/**
 * Script 选择器 item（对齐 script-design WorkflowSelector.tsx 下拉菜单 item）
 * - 图标容器：w-8 h-8 rounded-lg bg-emerald-500/10
 * - 图标：size=14dp
 * - 文字：text-[13px] font-medium text-slate-200
 * - 选中状态：bg-sky-500/10 text-sky-400 + Check 图标 size=16dp
 *
 * @author jiadou
 * @date 6/17/21
 */
class ScriptItemSelectorView(context: Context) : ListRecyclerItemView(context), View.OnClickListener {

    lateinit var mModel: ScriptInfoModel

    private var ivIcon: UIResourceIconView? = null
    private var tvName: TextView? = null
    private var ivCheck: ImageView? = null

    /** 当前是否选中 */
    private var _isSelected = false

    var view =
        LayoutInflater.from(context).inflate(R.layout.fragment_script_selector_item_layout, this)
            .apply {
                ivIcon = findViewById(R.id.iv_icon)
                tvName = findViewById(R.id.tv_name)
                ivCheck = findViewById(R.id.iv_check)
            }

    init {
        setOnClickListener(this)
    }

    override fun bindData(data: Any?) {
        mModel = data as ScriptInfoModel
        tvName?.text = mModel.scriptName
        val path = "${mModel.scriptPath}/${mModel.scriptMate?.icon}"

        ivIcon?.setResourceType("workflow")
        // 默认未选中状态
        updateSelectedState(false)
    }

    /**
     * 更新选中状态
     * @param selected 是否选中
     */
    fun updateSelectedState(selected: Boolean) {
        _isSelected = selected
        setSelected(selected) // 触发 View.isSelected，影响 selector 背景

        // 选中时显示 Check 图标，文字变天蓝色
        ivCheck?.visibility = if (selected) View.VISIBLE else View.GONE

        val textColor = if (selected) {
            GlobalApp.getColor(com.hive.i8n.R.color.design_accent_sky)
        } else {
            GlobalApp.getColor(com.hive.i8n.R.color.design_text_slate_200)
        }
        tvName?.setTextColor(textColor)
    }

    override fun onClick(v: View?) {
        AnimUtils.scaleAnim(v)
        postEvent(mModel)
    }
}