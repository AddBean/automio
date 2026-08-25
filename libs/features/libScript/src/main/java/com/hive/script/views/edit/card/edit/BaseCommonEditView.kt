// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.hive.anim.AnimUtils
import com.hive.base.BaseLayout
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.condition.ConditionIDS
import com.hive.script.condition.ConditionNotification
import com.hive.script.condition.ConditionParam
import com.hive.script.condition.ConditionPermission
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.views.beans.PointVectorFloat
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptDragView
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string
import com.hive.views.widgets.TextDrawableView

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
class BaseCommonEditView(context: Context, attrs: AttributeSet?) : BaseLayout(context, attrs) {

    private var isVisibleGone: Boolean = false

    private var conditionType: Int? = null

    private var cmd: ScriptCommand? = null

    private var isAnimRunning = false

    private var commonEditLayout: View? = null
    private var moreTv: TextDrawableView? = null
    private var offsetView: ScriptValueView? = null
    private var setRectValue: ScriptValueView? = null

    override fun initView(view: View?) {
        commonEditLayout = findViewById(R.id.commonEditLayout)
        moreTv = findViewById(R.id.moreTv)
        offsetView = findViewById(R.id.offsetView)
        setRectValue = findViewById(R.id.setRectValue)
        moreTv?.setOnClickListener {
            moreTv?.isSelected = moreTv?.isSelected == false
            updateExpandSelection(true)
        }
        moreTv?.isSelected = true

        offsetView?.onMaskClickListener = OnClickListener {
            ScriptRecordManager.setRecordDragViewType(ScriptRecordManager.RecordDragViewType.OFFSET)
            BaseScriptDialog.saveStateAndHidden()
            ScriptDragView.setNormalizedVector(cmd?.offsetVector)
            ScriptRecordManager.showRecordView()
            ScriptRecordManager.updateRecordView(
                ScriptRecordViewManager.ViewState.default()
                    .ofTrue(ScriptRecordViewManager.RecordViewType.TOUCHABLE)
                    .ofTrue(ScriptRecordViewManager.RecordViewType.MATCH_DRAG)
                    .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
            )
            ScriptRecordManager.setRecordResultListener { action, data ->
                if (action == ScriptRecordEventHandler.RecordResultAction.ACTION_DRAG) {
                    cmd?.offsetVector = data as PointVectorFloat
                    onBindCommand(cmd!!)
//                    ScriptRecordManager.hiddenRecordView()
                    BaseScriptDialog.restoreState()
                } else {
//                    ScriptRecordManager.hiddenRecordView()
                    BaseScriptDialog.restoreState()
                }
            }
        }
        setRectValue?.onMaskClickListener = OnClickListener {
            ScriptInsertManager.startSetRectLayout(cmd!!) {
                setRectValue?.setValue(
                    if (ScriptCommonUtils.isFloatRectOrigin(cmd!!.limitRect)) GlobalApp.getString(
                        com.hive.i8n.R.string.sc_edit_layout_rect_not_seted
                    ) else GlobalApp.getString(
                        com.hive.i8n.R.string.sc_edit_layout_rect_seted
                    )
                )
            }
        }
        updateExpandSelection(false)
    }

    fun expandCommon() {
        moreTv?.isSelected = false
        updateExpandSelection(true)
    }

    private fun updateExpandSelection(animEnable: Boolean) {
        if (shouldShowCommonEditParams()) {
            this.visibleOrGone(true)
            if (moreTv?.isSelected == true) {
                moreTv?.text = GlobalApp.getString(com.hive.i8n.R.string.sc_more_down_edit)
                moreTv?.setDrawableLeft(GlobalApp.getDrawable(R.drawable.sc_more_down))
                updateFunctionLayout()
                if (animEnable) {
                    if (isAnimRunning) return
                    isAnimRunning = true
                    commonEditLayout?.visibleOrGone(true)
                    commonEditLayout?.measure(MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED)
                    AnimUtils.heightAnim(
                        commonEditLayout,
                        commonEditLayout?.measuredHeight ?: 0,
                        0,
                        object :
                            AnimUtils.AnimListener() {
                            override fun onOver(v: View?) {
                                isAnimRunning = false
                                commonEditLayout?.visibleOrGone(false)
                            }
                        })
                } else {
                    isAnimRunning = false
                    commonEditLayout?.visibleOrGone(false)
                }

            } else {
                moreTv?.text = GlobalApp.getString(com.hive.i8n.R.string.sc_more_up_edit)
                moreTv?.setDrawableLeft(GlobalApp.getDrawable(R.drawable.sc_more_up))
                commonEditLayout?.visibleOrGone(true)
                updateFunctionLayout()
                commonEditLayout?.measure(MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED)
                if (animEnable) {
                    if (isAnimRunning) return
                    isAnimRunning = true
                    AnimUtils.heightAnim(
                        commonEditLayout,
                        0,
                        commonEditLayout?.measuredHeight ?: 0,
                        object :
                            AnimUtils.AnimListener() {
                            override fun onOver(v: View?) {
                                isAnimRunning = false
                            }
                        }
                    )
                }
            }
        } else {
            this.visibleOrGone(false)
            isAnimRunning = false
        }

    }

    private fun updateFunctionLayout() {
        offsetView?.visibleOrGone(cmd?.isSupportOffset() == true)
        setRectValue?.visibleOrGone(cmd?.isSupportRect() == true)
    }

    fun setConditionType(type: Int) {
        conditionType = type
        updateExpandSelection(false)
    }

    private fun shouldShowCommonEditParams(): Boolean {
        if (isVisibleGone) return false
        //通知栏不展示
        if (conditionType == ConditionIDS.ConditionIdNotification ||
            cmd?.conditionList?.firstOrNull() is ConditionNotification
        ) {
            return false
        }

        //变量不展示
        if (conditionType == ConditionIDS.ConditionIdParam ||
            cmd?.conditionList?.firstOrNull() is ConditionParam
        ) {
            return false
        }

        //权限检测不展示
        if (conditionType == ConditionIDS.ConditionIdPermission ||
            cmd?.conditionList?.firstOrNull() is ConditionPermission
        ) {
            return false
        }
        return cmd?.isSupportOffset() == true || cmd?.isSupportRect() == true
    }

    fun onBindCommand(command: ScriptCommand) {
        cmd = command
        updateExpandSelection(false)
        if (cmd == null
            || (cmd?.offsetVector?.fromX == 0f && cmd?.offsetVector?.fromY == 0f &&
                    cmd?.offsetVector?.toX == 0f && cmd?.offsetVector?.toY == 0f)
        ) {
            offsetView?.setValue(com.hive.i8n.R.string.sc_setting_none.string())
        } else {
            offsetView?.setValue(com.hive.i8n.R.string.sc_setting_seted.string())
        }

        setRectValue?.setValue(
            if (ScriptCommonUtils.isFloatRectOrigin(cmd!!.limitRect)) GlobalApp.getString(com.hive.i8n.R.string.sc_edit_layout_rect_not_seted) else GlobalApp.getString(
                com.hive.i8n.R.string.sc_edit_layout_rect_seted
            )
        )
    }

    fun setVisibleGone(isVisibleGone: Boolean) {
        this.isVisibleGone = isVisibleGone
    }


    override fun getLayoutId() = R.layout.cmd_common_card


}