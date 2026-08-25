// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import com.hive.extension.visibleOrGone
import com.hive.files.utils.XAppInfoParser
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.params.ScriptParam
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.base.params.ScriptSystemParam
import com.hive.script.cmd.CmdIf
import com.hive.script.condition.ConditionColor
import com.hive.script.condition.ConditionIDS
import com.hive.script.condition.ConditionImage
import com.hive.script.condition.ConditionNotification
import com.hive.script.condition.ConditionParam
import com.hive.script.condition.ConditionPermission
import com.hive.script.condition.ConditionView
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.dialog.DialogAppSelector
import com.hive.script.views.dialog.DialogPermissionSelector
import com.hive.script.views.dialog.DialogParamsManager
import com.hive.script.views.edit.card.edit.condition.ConditionsEditView
import com.hive.script.views.widgets.ScriptFloatView
import com.hive.script.views.widgets.ScriptMultipleSelectorView
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.script.views.widgets.ScriptValueView
import com.hive.utils.GlobalApp
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.FloatOptView
import com.hive.views.widgets.SelectorTabView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdIfEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {
    private var notifyTargetParamId: String = ScriptSystemParam.OUTPUT1.paramId
    private var typeSelector: ScriptTabSelectorView? = null
    private var typeNotificationSelector: ScriptTabSelectorView? = null
    private var flContent: ViewGroup? = null
    private var conditionsEditView: ConditionsEditView? = null
    private var optContent: ViewGroup? = null
    private var optContentNotification: ViewGroup? = null
    private var optContentPermission: ViewGroup? = null
    private var appNotificationPkg: ScriptMultipleSelectorView? = null
    private var actionDelay: ScriptFloatView? = null
    private var targetParamIdView: ScriptValueView? = null

    var cmd: CmdIf? = null

    override fun initView() {
        flContent = findViewById(R.id.fl_content)
        optContent = findViewById(R.id.opt_content)
        optContentNotification = findViewById(R.id.opt_content_notification)
        optContentPermission = findViewById(R.id.opt_content_permission)
        typeSelector = findViewById(R.id.type_selector)
        typeNotificationSelector = findViewById(R.id.notification_type_selector)
        targetParamIdView = findViewById(R.id.targetParamIdView)

        appNotificationPkg = findViewById(R.id.notification_app_pkg)
        actionDelay = findViewById(R.id.number_action_delay)
        conditionsEditView = ConditionsEditView(context)
        flContent?.addView(conditionsEditView)

        typeSelector?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    cmd?.postAction = p!!.second!!
                    onBindCommand(cmd!!)
                }
            }
        appNotificationPkg?.onSelectorItemClickListener =
            object : ScriptMultipleSelectorView.OnSelectorItemClickListener {
                override fun onSelectorRequestAdd() {
                    showSelector()
                }

                override fun onSelectorChanged() {
                    refreshAppInfo()
                }
            }
        typeNotificationSelector?.onTabSelectedChangedListener =
            object : SelectorTabView.OnTabSelectedChangedListener {
                override fun onSelectedChanged(p: Pair<String?, String?>?) {
                    cmd?.postAction = p!!.second!!
                    onBindCommand(cmd!!)
                }
            }
        conditionsEditView?.onReverseSelectorListener = {
            cmd?.conditionReverse = "no" == it
            onBindCommand(cmd!!)
        }

        conditionsEditView?.onMeetStateSelectorListener = {
            cmd?.conditionMeetAll = "and" == it
            onBindCommand(cmd!!)
        }
        actionDelay?.changedListener =
            FloatOptView.OnValueChangedListener { value ->
                cmd?.delayTime = value.toLong()
                actionDelay?.updateUi()
            }
        targetParamIdView?.onMaskClickListener = OnClickListener {
            DialogParamsManager(context)
                .setWritable(true)
                .setParamListener(object :
                    DialogParamsManager.OnParamListener {
                    override fun onParamSelected(param: ScriptParam?) {
                        cmd?.conditionList?.filterIsInstance<ConditionNotification>()?.forEach {
                            it.targetParamId =
                                ScriptParamEnv.getParam(param?.getFullId() ?: "")?.getFullId()
                        }
                        cmd?.let { onBindCommand(it) }
                    }
                }).show()
        }
    }


    private fun showSelector() {
        DialogAppSelector(context)
            .setOnAppSelectedListener(object : DialogAppSelector.OnAppSelectedListener {
                override fun onSelected(
                    dialog: DialogAppSelector,
                    appInfo: XAppInfoParser.AppInfo?
                ) {
                    dialog.dismiss()
                    appInfo ?: return
                    appInfo.appName ?: return
                    appInfo.packageName ?: return
                    appNotificationPkg?.addData(Pair(appInfo.appName!!, appInfo.packageName!!))
                    refreshAppInfo()
                }
            }).show()
    }

    private fun refreshAppInfo() {
        cmd?.conditionList?.filterIsInstance<ConditionNotification>()?.forEach {
            it.appList.clear()
            it.appList.addAll(appNotificationPkg?.getDataSet()?.distinctBy { it.second }
                ?: mutableListOf())
        }
        bindCommand(cmd!!)
    }

    fun setConditionType(type: Int) {
        conditionsEditView?.conditionType = type
        updateConditionType()
    }

    override fun checkCommandOrThrowError() {
        val cnd = cmd?.conditionList?.firstOrNull()
        if (cmd?.conditionList?.isNotEmpty() == false) {
            throw RuntimeException(GlobalApp.getString(com.hive.i8n.R.string.sc_add_cmd_if_error))
        }
        if (cnd is ConditionNotification) {
            CommonToast.showLong(com.hive.i8n.R.string.sc_notifiction_preimssion_tip)
        }
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdIf
        cmd?.run {
            typeSelector?.setValue(command.postAction)
            typeNotificationSelector?.setValue(command.postAction)
            actionDelay?.setNumber(command.delayTime.toFloat())
            conditionsEditView?.setReverseState(
                if (conditionReverse) "no" else "yes",
                if (conditionMeetAll) "and" else "or"
            )
            targetParamIdView?.setValue(
                ScriptCommandHelper.paramFormat.format(notifyTargetParamId)
            )
            setConditionType(cmd!!)
            if (this.conditionList == null) {
                this.conditionList = mutableListOf()
            }
            cmd?.conditionList?.filterIsInstance<ConditionNotification>()?.firstOrNull()?.run {
                appNotificationPkg?.loadDataSet(appList)
                notifyTargetParamId = targetParamId ?: ScriptSystemParam.OUTPUT1.paramId
                targetParamIdView?.setValue(
                    ScriptCommandHelper.paramFormat.format(targetParamId)
                )
            }
            cmd?.conditionList?.filterIsInstance<ConditionPermission>()?.firstOrNull()?.run {
                permissionList.map { fullKey ->
                    (ScriptHelper.mPermissionMap[fullKey] ?: fullKey) to fullKey
                }
            }
            conditionsEditView?.onBindCommand(this)

        }
    }

    private fun setConditionType(command: CmdIf) {
        when (command.conditionList?.firstOrNull()) {
            is ConditionNotification -> {
                conditionsEditView?.conditionType = ConditionIDS.ConditionIdNotification
            }

            is ConditionView -> {
                conditionsEditView?.conditionType = ConditionIDS.ConditionIdView
            }

            is ConditionImage -> {
                conditionsEditView?.conditionType = ConditionIDS.ConditionIdImage
            }

            is ConditionColor -> {
                conditionsEditView?.conditionType = ConditionIDS.ConditionIdColor
            }

            is ConditionParam -> {
                conditionsEditView?.conditionType = ConditionIDS.ConditionIdParam
            }

            is ConditionPermission -> {
                conditionsEditView?.conditionType = ConditionIDS.ConditionIdPermission
            }

            else -> {
            }
        }
        updateConditionType()
    }

    private fun updateConditionType() {
        when (conditionsEditView?.conditionType) {
            ConditionIDS.ConditionIdNotification -> {
                optContent?.visibleOrGone(false)
                optContentNotification?.visibleOrGone(true)
                optContentPermission?.visibleOrGone(false)
            }

            ConditionIDS.ConditionIdView -> {
                optContent?.visibleOrGone(true)
                optContentNotification?.visibleOrGone(false)
                optContentPermission?.visibleOrGone(false)
            }

            ConditionIDS.ConditionIdImage -> {
                optContent?.visibleOrGone(true)
                optContentNotification?.visibleOrGone(false)
                optContentPermission?.visibleOrGone(false)
            }

            ConditionIDS.ConditionIdColor -> {
                optContent?.visibleOrGone(true)
                optContentNotification?.visibleOrGone(false)
                optContentPermission?.visibleOrGone(false)
            }

            ConditionIDS.ConditionIdParam -> {
                optContent?.visibleOrGone(false)
                optContentNotification?.visibleOrGone(false)
                optContentPermission?.visibleOrGone(false)
            }

            ConditionIDS.ConditionIdPermission -> {
                optContent?.visibleOrGone(false)
                optContentNotification?.visibleOrGone(false)
                optContentPermission?.visibleOrGone(true)
            }

            else -> {
                optContent?.visibleOrGone(false)
                optContentNotification?.visibleOrGone(false)
                optContentPermission?.visibleOrGone(false)
            }
        }
    }

    override fun getEditContentId() = R.layout.cmd_if_card

}