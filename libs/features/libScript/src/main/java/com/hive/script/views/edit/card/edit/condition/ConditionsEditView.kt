// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit.condition

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandCondition
import com.hive.script.condition.ConditionColor
import com.hive.script.condition.ConditionIDS
import com.hive.script.condition.ConditionImage
import com.hive.script.condition.ConditionNotification
import com.hive.script.condition.ConditionParam
import com.hive.script.condition.ConditionPermission
import com.hive.script.condition.ConditionView
import com.hive.script.utils.ScriptBitmapHelper
import com.hive.script.views.widgets.ScriptSpanHelper
import com.hive.script.views.widgets.ScriptSpanMenuView
import com.hive.utils.GlobalApp
import com.hive.utils.utils.ColorUtils
import com.hive.views.widgets.TextDrawableView


/**
 * conditionType:0:notification,1:view
 */
class ConditionsEditView(context: Context) : BaseConditionEditCard(context) {

    constructor(context: Context, attributeSet: android.util.AttributeSet) : this(context)

    var onReverseSelectorListener: ((state: String) -> Unit)? = null

    var onMeetStateSelectorListener: ((meetState: String) -> Unit)? = null

    private var command: ScriptCommand? = null

    private val stateMap = mutableMapOf(
        "no" to GlobalApp.getString(com.hive.i8n.R.string.sc_condition_view_edit_no),
        "yes" to GlobalApp.getString(com.hive.i8n.R.string.sc_condition_view_edit_yes)
    )

    private val meetMap = mutableMapOf(
        "or" to GlobalApp.getString(com.hive.i8n.R.string.sc_condition_view_edit_or),
        "and" to GlobalApp.getString(com.hive.i8n.R.string.sc_condition_view_edit_and)
    )

    //添加文字右侧的图片
    private val bmpDown = BitmapFactory.decodeResource(
        GlobalApp.getResources(), R.drawable.icon_arr_down
    )
    private val bmpEdit = BitmapFactory.decodeResource(
        GlobalApp.getResources(), R.drawable.sc_icon_arr_edit
    )


    var conditionType: Int = 0

    private var btnAddCondition: TextDrawableView? = null
    private var conditionTitle: ScriptSpanMenuView? = null
    private var layoutList: ViewGroup? = null

    init {
        btnAddCondition = findViewById(R.id.btnAddCondition)
        conditionTitle = findViewById(R.id.conditionTitle)
        layoutList = findViewById(R.id.layoutList)
        btnAddCondition?.setOnClickListener {
            val cmd = command ?: return@setOnClickListener
            when (conditionType) {
                ConditionIDS.ConditionIdNotification -> {
                    val cdn = ConditionNotification(cmd)
                    cdn.appList = cmd.conditionList?.filterIsInstance<ConditionNotification>()
                        ?.firstOrNull()?.appList ?: mutableListOf()
                    command?.conditionList?.add(cdn)
                    onBindCommand(command!!)
                }

                ConditionIDS.ConditionIdView -> {
                    val cdn = ConditionView(cmd)
                    command?.conditionList?.add(cdn)
                    onBindCommand(command!!)
                }

                ConditionIDS.ConditionIdImage -> {
                    val cdn = ConditionImage(cmd)
                    command?.conditionList?.add(cdn)
                    onBindCommand(command!!)
                }

                ConditionIDS.ConditionIdColor -> {
                    val cdn = ConditionColor(cmd)
                    command?.conditionList?.add(cdn)
                    onBindCommand(command!!)
                }

                ConditionIDS.ConditionIdParam -> {
                    val cdn = ConditionParam(cmd)
                    command?.conditionList?.add(cdn)
                    onBindCommand(command!!)
                }

                ConditionIDS.ConditionIdPermission -> {
                    val cdn = ConditionPermission(cmd)
                    cdn.permissionList = cmd.conditionList?.filterIsInstance<ConditionPermission>()
                        ?.firstOrNull()?.permissionList?.toMutableList()
                        ?: mutableListOf(com.hive.script.utils.ScriptHelper.PERMISSION_CAMERA)
                    command?.conditionList?.add(cdn)
                    onBindCommand(command!!)
                }
            }
        }
    }

    override fun onBindCommand(command: ScriptCommand) {
        this.command = command
        layoutList?.removeAllViews()
        command.conditionList?.forEach {
            val conditionEditView = getConditionEditView(it)
            conditionEditView.onBindCondition(it)
            layoutList?.addView(conditionEditView)
        }
        conditionTitle?.movementMethod = LinkMovementMethod.getInstance()
        conditionTitle?.highlightColor = GlobalApp.getColor(android.R.color.transparent);
        setReverseState(
            if (command.conditionReverse) "no" else "yes",
            if (command.conditionMeetAll) "and" else "or"
        )
    }

    fun setReverseState(state: String, meetState: String) {
        conditionTitle?.setSpanText(
            GlobalApp.getString(com.hive.i8n.R.string.sc_condition_view_edit_title),
            listOf(
                ScriptSpanHelper.ClickSpan(
                    spanText = stateMap[state],
                    spanType = ScriptSpanHelper.ClickSpanType.Selector,
                    spanTextColor = Color.WHITE,
                    spanExtra = stateMap,
                    spanIcon = bmpDown
                ) { spn ->
                    if (spn.spanText == stateMap["no"]) {
                        onReverseSelectorListener?.invoke("no")
                    } else {
                        onReverseSelectorListener?.invoke("yes")
                    }
                }, ScriptSpanHelper.ClickSpan(
                    spanText = meetMap[meetState],
                    spanType = ScriptSpanHelper.ClickSpanType.Selector,
                    spanTextColor = Color.WHITE,
                    spanExtra = meetMap,
                    spanIcon = bmpDown
                ) { spn ->
                    if (spn.spanText == meetMap["or"]) {
                        onMeetStateSelectorListener?.invoke("or")
                    } else {
                        onMeetStateSelectorListener?.invoke("and")
                    }
                })
        )
    }


    private fun removeCondition(condition: ScriptCommandCondition?) {
        condition ?: return
        command?.conditionList ?: return
        command?.conditionList?.remove(condition)
        onBindCommand(command!!)
    }

    override fun getEditContentId() = R.layout.script_conditions_edit_view

    private fun getConditionEditView(condition: ScriptCommandCondition): ConditionItemEditView {
        return ConditionItemEditView(context).apply {
            onBindCondition(condition)
        }
    }

    inner class ConditionItemEditView(context: Context) : BaseLayout(context) {

        private var condition: ScriptCommandCondition? = null

        private var conditionText: ScriptSpanMenuView? = null
        private var ivDelete: View? = null

        override fun initView(p0: View?) {
            conditionText = findViewById(R.id.conditionText)
            ivDelete = findViewById(R.id.ivDelete)
            clipChildren = false
            clipToPadding = false
            ivDelete?.setOnClickListener {
                removeCondition(condition)
            }
        }


        fun onBindCondition(condition: ScriptCommandCondition) {
            this.condition = condition
            conditionText?.movementMethod = LinkMovementMethod.getInstance()
            conditionText?.highlightColor = GlobalApp.getColor(android.R.color.transparent);
            when (condition) {
                is ConditionNotification -> {
                    conditionText?.setSpanText(
                        GlobalApp.getString(com.hive.i8n.R.string.sc_condition_notification_edit_text),
                        listOf(
                            ScriptSpanHelper.ClickSpan(
                                spanText = condition.getActionName(),
                                spanType = ScriptSpanHelper.ClickSpanType.Selector,
                                spanTextColor = Color.WHITE,
                                spanExtra = ScriptCommandCondition.actionMap.filter { it.key == "contains" },
                                spanIcon = bmpDown
                            ) { spn ->
                                condition.action =
                                    ScriptCommandCondition.actionMap.filter { it.value == spn.spanText }.keys.firstOrNull()
                                onBindCondition(condition)
                            }, ScriptSpanHelper.ClickSpan(
                                spanText = condition.getTextName(),
                                spanType = ScriptSpanHelper.ClickSpanType.Input,
                                spanExtra = if (!TextUtils.isEmpty(condition.text)) Color.WHITE else GlobalApp.getColor(
                                    com.hive.i8n.R.color.colorRed
                                ),
                                spanIcon = bmpEdit
                            ) {
                                condition.text = it.spanText
                                onBindCondition(condition)
                            })
                    )
                }

                is ConditionView -> {
                    conditionText?.setSpanText(
                        GlobalApp.getString(com.hive.i8n.R.string.sc_condition_view_edit_text),
                        listOf(
                            ScriptSpanHelper.ClickSpan(
                                spanText = condition.getActionName(),
                                spanType = ScriptSpanHelper.ClickSpanType.Selector,
                                spanTextColor = Color.WHITE,
                                spanExtra = ScriptCommandCondition.actionMap,
                                spanIcon = bmpDown
                            ) { spn ->
                                condition.action =
                                    ScriptCommandCondition.actionMap.filter { it.value == spn.spanText }.keys.firstOrNull()
                                onBindCondition(condition)
                            }, ScriptSpanHelper.ClickSpan(
                                spanText = condition.getTextName(),
                                spanType = ScriptSpanHelper.ClickSpanType.Input,
                                spanTextColor = if (!TextUtils.isEmpty(condition.text)) Color.WHITE else GlobalApp.getColor(
                                    com.hive.i8n.R.color.colorRed
                                ),
                                spanIcon = bmpEdit
                            ) {
                                condition.text = it.spanText
                                onBindCondition(condition)
                            }, ScriptSpanHelper.ClickSpan(
                                spanText = condition.getOcrName(),
                                spanType = ScriptSpanHelper.ClickSpanType.Selector,
                                spanTextColor = Color.WHITE,
                                spanExtra = ScriptCommandCondition.ocrMap,
                                spanIcon = bmpDown
                            ) { spn ->
                                condition.ocrType =
                                    ScriptCommandCondition.ocrMap.filter { it.value == spn.spanText }.keys.firstOrNull()
                                        ?.toIntOrNull() ?: 1
                                onBindCondition(condition)
                            })
                    )
                }


                is ConditionImage -> {
                    conditionText?.setSpanText(
                        GlobalApp.getString(com.hive.i8n.R.string.sc_condition_view_edit_image),
                        listOf(
                            ScriptSpanHelper.ClickSpan(
                                spanText = GlobalApp.getString(com.hive.i8n.R.string.sc_condition_image_selecter_name),
                                spanType = ScriptSpanHelper.ClickSpanType.ImageSelector,
                                spanTextColor = Color.WHITE,
                                spanIcon = ScriptBitmapHelper.createBitmapByFiles(condition.getAttachFiles())
                                    ?: bmpEdit
                            ) {
                                condition.attachmentFiles =
                                    (it.spanExtra as List<String>).toMutableList()
                                onBindCondition(condition)
                            }, ScriptSpanHelper.ClickSpan(
                                spanText = condition.getActionName(),
                                spanType = ScriptSpanHelper.ClickSpanType.Selector,
                                spanTextColor = Color.WHITE,
                                spanExtra = ScriptCommandCondition.actionMapImage,
                                spanIcon = bmpDown
                            ) { spn ->
                                condition.actionValue =
                                    ScriptCommandCondition.actionMapImage.filter { it.value == spn.spanText }.keys.firstOrNull()
                                onBindCondition(condition)
                            })
                    )
                }

                is ConditionColor -> {
                    conditionText?.setSpanText(
                        GlobalApp.getString(com.hive.i8n.R.string.sc_condition_view_edit_color),
                        listOf(
                            ScriptSpanHelper.ClickSpan(
                                spanText = GlobalApp.getString(com.hive.i8n.R.string.sc_condition_color_selecter_name),
                                spanType = ScriptSpanHelper.ClickSpanType.ColorSelector,
                                spanTextColor = Color.WHITE,
                                spanExtra = condition.color ?: Color.BLACK,
                                spanIcon = ColorUtils.createColorBitmap(
                                    ColorUtils.toHexColor(
                                        condition.color ?: Color.BLACK
                                    )
                                )
                            ) {
                                condition.color = it.spanExtra as Int
                                onBindCondition(condition)
                            },

                            ScriptSpanHelper.ClickSpan(
                                spanText = condition.getActionName(),
                                spanType = ScriptSpanHelper.ClickSpanType.Selector,
                                spanTextColor = Color.WHITE,
                                spanExtra = ScriptCommandCondition.actionMapColor,
                                spanIcon = bmpDown
                            ) { spn ->
                                condition.actionValue =
                                    ScriptCommandCondition.actionMapColor.filter { it.value == spn.spanText }.keys.firstOrNull()
                                onBindCondition(condition)
                            })
                    )
                }

                is ConditionParam -> {
                    conditionText?.setSpanText(
                        GlobalApp.getString(com.hive.i8n.R.string.sc_condition_view_edit_param),
                        listOf(
                            ScriptSpanHelper.ClickSpan(
                                spanText = condition.getParamName(),
                                spanType = ScriptSpanHelper.ClickSpanType.ParamSelector,
                                spanTextColor = Color.WHITE,
                                spanExtra = condition.paramId,
                                spanIcon = bmpEdit,
                            ) {
                                condition.paramId = it.spanExtra as String
                                onBindCondition(condition)
                            }, ScriptSpanHelper.ClickSpan(
                                spanText = condition.getActionName(),
                                spanType = ScriptSpanHelper.ClickSpanType.Selector,
                                spanTextColor = Color.WHITE,
                                spanExtra = ScriptCommandCondition.actionMapParam,
                                spanIcon = bmpDown
                            ) { spn ->
                                condition.action =
                                    ScriptCommandCondition.actionMapParam.filter { it.value == spn.spanText }.keys.firstOrNull()
                                onBindCondition(condition)
                            }, ScriptSpanHelper.ClickSpan(
                                spanText = condition.getActionValue(),
                                spanType = ScriptSpanHelper.ClickSpanType.Input,
                                spanTextColor = Color.WHITE,
                                spanExtra = null,
                                spanIcon = bmpEdit
                            ) {
                                condition.value = it.spanText
                                onBindCondition(condition)
                            })
                    )
                }

                is ConditionPermission -> {
                    val displayText = condition.getPermissionDisplayText()
                    val hint = GlobalApp.getString(com.hive.i8n.R.string.sc_condition_permission_select_hint)
                    conditionText?.setSpanText(
                        GlobalApp.getString(com.hive.i8n.R.string.sc_condition_permission_edit_text),
                        listOf(
                            ScriptSpanHelper.ClickSpan(
                                spanText = displayText.ifEmpty { hint },
                                spanType = ScriptSpanHelper.ClickSpanType.PermissionMultiSelector,
                                spanTextColor = Color.WHITE,
                                spanExtra = condition,
                                spanIcon = bmpEdit
                            ) {
                                onBindCondition(condition)
                            }
                        )
                    )
                }

                else -> ""
            }
        }

        override fun getLayoutId() = R.layout.script_condition_edit_item_view
    }
}