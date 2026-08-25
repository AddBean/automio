// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.cards

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.utils.GlobalApp
import com.hive.views.R as ViewsR

/**
 * design-spec WorkflowPage 列表卡片：#121212 圆角卡、左侧图标区、副标题（mate.tag）、右侧圆形播放。
 */
class ScriptWorkflowItemView(context: Context) : ScriptItemView(context) {

    private val tvDesc: TextView? by lazy { view.findViewById(R.id.tvDesc) }
    private val tvStatus: TextView? by lazy { view.findViewById(R.id.tvStatus) }

    override fun getItemContentId(): Int = R.layout.fragment_script_item_workflow_spec

    override fun bindData(data: Any?) {
        super.bindData(data)
        val itemData = data as? ItemData ?: return
        val model = itemData.data as? ScriptInfoModel ?: return
        val tag = model.scriptMate?.tag?.trim().orEmpty()
        tvDesc?.text = tag
        tvDesc?.visibleOrGone(tag.isNotEmpty())
        view.findViewById<View>(R.id.layout_script_item_extra)?.visibleOrGone(itemData.isEditModel)
        view.findViewById<View>(R.id.ivMore)?.visibility = View.GONE

        val ivPlay = view.findViewById<ImageView>(R.id.ivPlay)
        // Web: PlayCircle / 播放中；emerald-400，播放中为白（Lucide 矢量 + tint）
        val emerald400 = GlobalApp.getColor(com.hive.i8n.R.color.script_workflow_emerald)
        val runningIcon = GlobalApp.getColor(com.hive.i8n.R.color.script_workflow_running_icon)
        val idleText = ContextCompat.getColor(context, com.hive.i8n.R.color.script_workflow_status_text_idle)
        val runningText = ContextCompat.getColor(context, com.hive.i8n.R.color.script_workflow_status_text_running)
        val isPlaying = ivPlay?.isSelected == true
        ivPlay?.colorFilter = PorterDuffColorFilter(
            if (isPlaying) runningIcon else emerald400,
            PorterDuff.Mode.SRC_IN
        )
        ivPlay?.background?.state = if (isPlaying) intArrayOf(android.R.attr.state_selected) else intArrayOf()

        tvStatus?.apply {
            isSelected = isPlaying
            text = context.getString(
                if (isPlaying) com.hive.i8n.R.string.script_state_running
                else com.hive.i8n.R.string.script_state_pending
            )
            setTextColor(if (isPlaying) runningText else idleText)
        }

        val ivClick = view.findViewById<ImageView>(R.id.ivClick)
        if (TextUtils.isEmpty(model.scriptMate?.icon)) {
            // 父类在无 icon 时会设成 ic_click，这里改回 design 的 Lucide Activity
            ivClick?.setImageResource(R.drawable.ic_lucide_activity)
            ivClick?.colorFilter = PorterDuffColorFilter(emerald400, PorterDuff.Mode.SRC_IN)
        } else {
            ivClick?.clearColorFilter()
        }
    }
}
