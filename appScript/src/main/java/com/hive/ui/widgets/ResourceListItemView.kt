// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.widgets

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.hive.app.script.R
import com.hive.extension.visibleOrGone
import com.hive.i8n.R as i8nR
import com.hive.script.R as scriptR
import com.hive.utils.GlobalApp
import com.hive.views.widgets.UIResourceIconView

/**
 * 统一的资源列表 Item 控件
 * 支持 Workflow/Skill/Tool/Custom Tool 四种资源类型
 * 自动处理播放按钮 tint 和运行状态显示
 *
 * @author jiadou
 * @date 2026/04/28
 */
class ResourceListItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // UI Components (从 XML inflate)
    private val iconResource: UIResourceIconView
    private val tvName: AppCompatTextView
    private val tvStatus: AppCompatTextView
    private val tvDescription: AppCompatTextView
    private val ivPlay: ImageView
    private val ivArrow: ImageView
    private val ivDelete: ImageView
    private val layoutMeta: LinearLayout
    private val statusDot: View

    // Config
    var resourceType: String = "workflow"
    var showDescription: Boolean = true
    var showDeleteButton: Boolean = false
    var showArrow: Boolean = false
    var showStatusDot: Boolean = false

    init {
        // Inflate XML layout
        View.inflate(context, R.layout.view_resource_list_item, this)

        // Find views
        iconResource = findViewById(R.id.icon_resource)
        tvName = findViewById(R.id.tv_name)
        tvStatus = findViewById(R.id.tv_status)
        tvDescription = findViewById(R.id.tv_description)
        ivPlay = findViewById(R.id.iv_play)
        ivArrow = findViewById(R.id.iv_arrow)
        ivDelete = findViewById(R.id.iv_delete)
        layoutMeta = findViewById(R.id.layout_meta)
        statusDot = findViewById(R.id.status_dot)
    }

    /**
     * 配置资源类型和显示选项
     */
    fun configure(
        resourceType: String,
        showDescription: Boolean = true,
        showDeleteButton: Boolean = false,
        showArrow: Boolean = false,
        showStatusDot: Boolean = false
    ) {
        this.resourceType = resourceType
        this.showDescription = showDescription
        this.showDeleteButton = showDeleteButton
        this.showArrow = showArrow
        this.showStatusDot = showStatusDot

        iconResource.setResourceType(resourceType)
        tvDescription.visibleOrGone(false)
        ivDelete.visibleOrGone(showDeleteButton)
        ivArrow.visibleOrGone(showArrow)
        statusDot.visibleOrGone(showStatusDot)

        // 如果显示状态点，需要调整状态标签的 marginStart
        if (showStatusDot) {
            tvStatus.visibleOrGone(true) // 确保 status 可见
        }
    }

    /**
     * 绑定数据
     */
    fun bindData(
        name: String,
        description: String = "",
        isRunning: Boolean = false,
        isCustom: Boolean = false
    ) {
        tvName.text = name
        tvDescription.text = description

        // 更新运行状态
        updateRunningState(isRunning)

        // 根据资源类型和是否自定义，调整图标色调
        if (resourceType == "tool" && showStatusDot) {
            iconResource.setIconTintOverride(
                if (isCustom) GlobalApp.getColor(i8nR.color.design_accent_amber)
                else GlobalApp.getColor(i8nR.color.design_accent_sky)
            )
        }
    }

    /**
     * 更新运行状态（自动处理播放按钮 tint）
     */
    fun updateRunningState(isRunning: Boolean) {
        // 播放按钮状态
        ivPlay.isSelected = isRunning

        // 播放按钮着色（对齐 ScriptWorkflowItemView）
        val emerald400 = GlobalApp.getColor(i8nR.color.script_workflow_emerald)
        val runningIcon = GlobalApp.getColor(i8nR.color.script_workflow_running_icon)
        ivPlay.colorFilter = PorterDuffColorFilter(
            if (isRunning) runningIcon else emerald400,
            PorterDuff.Mode.SRC_IN
        )

        // 背景 state
        ivPlay.background?.state = if (isRunning) {
            intArrayOf(android.R.attr.state_selected)
        } else {
            intArrayOf()
        }

        // 状态标签
        tvStatus.isSelected = isRunning
        tvStatus.text = context.getString(
            if (isRunning) i8nR.string.script_state_running
            else i8nR.string.script_state_pending
        )
        tvStatus.setTextColor(
            if (isRunning) {
                GlobalApp.getColor(i8nR.color.script_workflow_status_text_running)
            } else {
                GlobalApp.getColor(i8nR.color.script_workflow_status_text_idle)
            }
        )
    }

    /**
     * 设置播放按钮点击监听
     */
    fun setPlayClickListener(listener: OnClickListener?) {
        ivPlay.setOnClickListener(listener)
    }

    /**
     * 设置删除按钮点击监听
     */
    fun setDeleteClickListener(listener: OnClickListener?) {
        ivDelete.setOnClickListener(listener)
    }

    /**
     * 设置整个 item 点击监听
     */
    fun setItemClickListener(listener: OnClickListener?) {
        setOnClickListener(listener)
    }
}