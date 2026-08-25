// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.hive.base.BaseLayout
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.base.ScriptClickActionHelper
import com.hive.script.views.beans.PointVectorFloat
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptDragView
import com.hive.script.views.widgets.ScriptFloatView
import com.hive.script.views.widgets.ScriptNumberView
import com.hive.script.views.widgets.ScriptRandomSizeView
import com.hive.script.views.widgets.ScriptSizeSeekbarView
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.extends.string
import com.hive.views.widgets.FloatOptView
import com.hive.views.widgets.NumberOptView
import com.hive.views.widgets.SelectorTabView

class OperateCommonEditView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs) {

    private var listener: OnOperateListener? = null

    private var operateData = OperateData()

    private var number_count: ScriptNumberView? = null
    private var number_gap: ScriptFloatView? = null
    private var number_press: ScriptFloatView? = null
    private var opt_drag: ScriptValueView? = null
    private var opt_drag_duration: ScriptFloatView? = null
    private var opt_drag_press_duration: ScriptFloatView? = null
    private var opt_drag_type: ScriptTabSelectorView? = null
    private var random_size: ScriptRandomSizeView? = null
    private var type_selector: ScriptTabSelectorView? = null

    override fun initView(view: View?) {
        number_count = findViewById(R.id.number_count)
        number_gap = findViewById(R.id.number_gap)
        number_press = findViewById(R.id.number_press)
        opt_drag = findViewById(R.id.opt_drag)
        opt_drag_duration = findViewById(R.id.opt_drag_duration)
        opt_drag_press_duration = findViewById(R.id.opt_drag_press_duration)
        opt_drag_type = findViewById(R.id.opt_drag_type)
        random_size = findViewById(R.id.random_size)
        type_selector = findViewById(R.id.type_selector)

        random_size?.mOnProgressChanged = object : ScriptSizeSeekbarView.OnSizeChangedListener {
            override fun onSizeChanged(action: Int, size: Int) {
                operateData.random = size
                listener?.onOperateChangedData(operateData)
            }
        }
        type_selector?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    operateData.action = p!!.second!!
                    updateUi(operateData.action)
                    listener?.onOperateChangedData(operateData)
                }
            }

        opt_drag_type?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    operateData.dragType = p!!.second!!.toInt()
                    updateUi(operateData.action)
                    listener?.onOperateChangedData(operateData)
                }
            }

        number_press?.changedListener = FloatOptView.OnValueChangedListener { value ->
            operateData.pressDuration = value.toLong()
            listener?.onOperateChangedData(operateData)
        }
        number_gap?.changedListener = FloatOptView.OnValueChangedListener { value ->
            operateData.fastGap = value.toLong()
            listener?.onOperateChangedData(operateData)
        }
        number_count?.changedListener = NumberOptView.OnValueChangedListener { value ->
            operateData.fastCount = value
            listener?.onOperateChangedData(operateData)
        }
        opt_drag_duration?.changedListener = FloatOptView.OnValueChangedListener { value ->
            operateData.dragDuration = value.toLong()
            listener?.onOperateChangedData(operateData)
        }

        opt_drag_press_duration?.changedListener = FloatOptView.OnValueChangedListener { value ->
            operateData.dragPressDuration = value.toLong()
            listener?.onOperateChangedData(operateData)
        }
        opt_drag?.onMaskClickListener = OnClickListener {
            BaseScriptDialog.saveStateAndHidden()
            ScriptRecordManager.setRecordDragViewType(ScriptRecordManager.RecordDragViewType.DRAG)
            ScriptDragView.setNormalizedVector(operateData.dragData)
            ScriptRecordManager.showRecordView()
            ScriptRecordManager.updateRecordView(
                ScriptRecordViewManager.ViewState.default()
                    .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.MATCH_DRAG)
                    .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
            )
            ScriptRecordManager.setRecordResultListener { action, data ->
                if (action == ScriptRecordEventHandler.RecordResultAction.ACTION_DRAG) {
                    operateData.dragData = data as PointVectorFloat
                    listener?.onOperateChangedData(operateData)
                    bindData(operateData, listener)
//                    ScriptRecordManager.hiddenRecordView()
                    BaseScriptDialog.restoreState()
                } else {
//                    ScriptRecordManager.hiddenRecordView()
                    BaseScriptDialog.restoreState()
                }
            }
        }
    }

    fun bindData(data: OperateData, listener: OnOperateListener?) {
        this.listener = listener
        operateData = data
        type_selector?.setValue(operateData.action)
        random_size?.setValue(operateData.random)
        number_gap?.setNumber(operateData.fastGap.toFloat())
        number_count?.setNumber(operateData.fastCount)
        number_press?.setNumber(operateData.pressDuration.toFloat())
        opt_drag_type?.setValue(operateData.dragType.toString())
        opt_drag_duration?.setNumber(operateData.dragDuration.toFloat())
        opt_drag_press_duration?.setNumber(operateData.dragPressDuration.toFloat())
        if (operateData.dragData == null
            || (operateData.dragData?.fromX == 0f && operateData.dragData?.fromY == 0f &&
                    operateData.dragData?.toX == 0f && operateData.dragData?.toY == 0f)
        ) {
            opt_drag?.setValue(com.hive.i8n.R.string.sc_setting_none.string())
        } else {
            opt_drag?.setValue(com.hive.i8n.R.string.sc_setting_seted.string())
        }
        updateUi(operateData.action)
    }

    private fun updateUi(action: String?) {
        when (action) {
            ScriptClickActionHelper.ACTION_CLICK -> {
                number_gap?.visibleOrGone(false)
                number_count?.visibleOrGone(false)
                number_press?.visibleOrGone(false)
                random_size?.visibleOrGone(true)
                opt_drag_duration?.visibleOrGone(false)
                opt_drag_type?.visibleOrGone(false)
                opt_drag?.visibleOrGone(false)
                opt_drag_press_duration?.visibleOrGone(false)

                number_gap?.isEnabled = false
                number_count?.isEnabled = false
                number_press?.isEnabled = false
                random_size?.isEnabled = true
                opt_drag_type?.isEnabled = false
                opt_drag?.isEnabled = false
                opt_drag_duration?.isEnabled = false
                opt_drag_press_duration?.isEnabled = false
            }

            ScriptClickActionHelper.ACTION_PRESS -> {
                number_gap?.visibleOrGone(false)
                number_count?.visibleOrGone(false)
                number_press?.visibleOrGone(true)
                random_size?.visibleOrGone(true)
                opt_drag_duration?.visibleOrGone(false)
                opt_drag_type?.visibleOrGone(false)
                opt_drag?.visibleOrGone(false)
                opt_drag_press_duration?.visibleOrGone(false)

                number_gap?.isEnabled = false
                number_count?.isEnabled = false
                number_press?.isEnabled = true
                random_size?.isEnabled = true
                opt_drag?.isEnabled = false
                opt_drag_type?.isEnabled = false
                opt_drag_duration?.isEnabled = false
                opt_drag_press_duration?.isEnabled = false
            }

            ScriptClickActionHelper.ACTION_FAST_CLICK -> {
                number_gap?.visibleOrGone(true)
                number_count?.visibleOrGone(true)
                number_press?.visibleOrGone(false)
                random_size?.visibleOrGone(true)
                opt_drag_duration?.visibleOrGone(false)
                opt_drag_type?.visibleOrGone(false)
                opt_drag?.visibleOrGone(false)
                opt_drag_press_duration?.visibleOrGone(false)

                number_gap?.isEnabled = true
                number_count?.isEnabled = true
                number_press?.isEnabled = false
                random_size?.isEnabled = true
                opt_drag?.isEnabled = false
                opt_drag_type?.isEnabled = false
                opt_drag_duration?.isEnabled = false
                opt_drag_press_duration?.isEnabled = false
            }

            ScriptClickActionHelper.ACTION_BREAK -> {
                number_gap?.visibleOrGone(false)
                number_count?.visibleOrGone(false)
                number_press?.visibleOrGone(false)
                random_size?.visibleOrGone(false)
                opt_drag_duration?.visibleOrGone(false)
                opt_drag_type?.visibleOrGone(false)
                opt_drag?.visibleOrGone(false)
                opt_drag_press_duration?.visibleOrGone(false)

                number_gap?.isEnabled = false
                number_count?.isEnabled = false
                number_press?.isEnabled = false
                random_size?.isEnabled = false
                opt_drag?.isEnabled = false
                opt_drag_type?.isEnabled = false
                opt_drag_duration?.isEnabled = false
                opt_drag_press_duration?.isEnabled = false
            }

            ScriptClickActionHelper.ACTION_DRAG -> {
                number_gap?.visibleOrGone(false)
                number_count?.visibleOrGone(false)
                number_press?.visibleOrGone(false)
                random_size?.visibleOrGone(true)
                opt_drag?.visibleOrGone(true)
                opt_drag_type?.visibleOrGone(true)
                opt_drag_duration?.visibleOrGone(true)
                opt_drag_press_duration?.visibleOrGone(operateData.dragType == 0)

                number_gap?.isEnabled = false
                number_count?.isEnabled = false
                number_press?.isEnabled = false
                random_size?.isEnabled = true
                opt_drag?.isEnabled = true
                opt_drag_type?.isEnabled = true
                opt_drag_duration?.isEnabled = true
                opt_drag_press_duration?.isEnabled = operateData.dragType == 0
            }
        }
    }

    data class OperateData(
        var random: Int = 0,
        var fastGap: Long = 0,
        var fastCount: Int = 0,
        var pressDuration: Long = 0,
        var action: String = ScriptClickActionHelper.ACTION_CLICK,
        var dragType: Int = 0,
        var dragPressDuration: Long = 1500L,
        var dragDuration: Long = 500L,
        var dragData: PointVectorFloat? = null
    )

    interface OnOperateListener {
        fun onOperateChangedData(data: OperateData)
    }

    override fun getLayoutId(): Int = R.layout.operate_common_edit_view
}