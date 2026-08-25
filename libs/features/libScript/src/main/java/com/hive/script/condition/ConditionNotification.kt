// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.condition

import android.app.Notification
import android.text.TextUtils
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandCondition
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.utils.ScriptHelper
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.utils.utils.StringUtils
import java.util.regex.Matcher
import java.util.regex.Pattern

@AutoConditionRegister(type = ConditionIDS.ConditionIdNotification)
class ConditionNotification(val cmd: ScriptCommand) : ScriptCommandCondition(cmd) {

    private var targetNotification: Notification? = null

    private var matchPattern = """checkNotification\((.*)\)"""

    var action: String? = "contains"

    var targetParamId: String? = ScriptParamEnv.getDefaultParam()?.getFullId()

    var text: String? =
        GlobalApp.getString(com.hive.i8n.R.string.sc_edit_condition_notification_edit_text_default)

    var timeGap = 100L

    var timeCurrent = 0L

    var appList = mutableListOf<Pair<String, String>>()

    override fun isMeet(cmd: ScriptCommand?): Boolean {
        if (isMeetFromService()) {
            return true
        }
        if (isMeetFromAccessibility()) {
            return true
        }
        return false;
    }

    private fun isMeetFromAccessibility(): Boolean {
        val paramText = cmd.parseParamText(text) ?: text
        targetNotification =
            ConditionNotificationProcessor.accessInstance()
                .checkNotification(paramText, appList.map { it.second })
        return targetNotification != null
    }

    private fun isMeetFromService(): Boolean {
        val paramText = cmd.parseParamText(text) ?: text
        targetNotification =
            ConditionNotificationProcessor.serviceInstance()
                .checkNotification(paramText, appList.map { it.second })
        return targetNotification != null
    }


    override fun doPostAction(action: String) {
        if (timeCurrent + timeGap > System.currentTimeMillis()) {
            return
        }
        timeCurrent = System.currentTimeMillis()

        val tickerText = targetNotification?.tickerText?.toString()
        val androidTitle = targetNotification?.extras?.getString("android.title") ?: ""
        val androidText = targetNotification?.extras?.getString("android.text") ?: ""
        val androidBigText = targetNotification?.extras?.getString("android.bigText") ?: ""


        //如果不为空则用\n拼接
        val text = "$androidTitle\n$androidText\n$androidBigText\n$tickerText\n".trim()

        cmd.writeParam(targetParamId, text)
        when (action) {
            Post_Action_Click -> {
                targetNotification?.run {
                    //如果锁屏，则在解锁工作流后再点击下
                    ScriptEventHelper.get().openNotification(this)
                } ?: run {
                    DLog.e("ConditionNotification", "notificationEvent is null")
                }
                targetNotification = null
            }
        }
    }

    override fun getCondition() =
        "checkNotification($action,\"${
            StringUtils.encoding(text)
        }\",\"${
            appList.map { "${it.first}_${it.second}" }.takeIf { it.isNotEmpty() }
                ?.joinToString("|") ?: "all"
        }\",$targetParamId)"

    override fun getConditionName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_notification_name)

    override fun getConditionDesc() = GlobalApp.getString(com.hive.i8n.R.string.cmd_des_notification_des, text)


    fun getActionName(): String {
        return actionMap[action] ?: ""
    }

    fun getTextName(): String {
        return text ?: ""
    }

    override fun parseCondition(cmd: String) {
        val r: Pattern = Pattern.compile(matchPattern)
        val m: Matcher = r.matcher(cmd)
        if (m.find()) {
            val params = ScriptCommandHelper.splitParams(m.group(1)?.toString())
            action = params.getOrNull(0) ?: action
            text = StringUtils.decoding(
                ScriptCommandHelper.parseParamString(
                    params.getOrNull(1) ?: text
                )
            )
            var apps = ScriptCommandHelper.parseParamString(params.getOrNull(2))
            if (TextUtils.isEmpty(apps)) {
                apps = "all"
            }
            appList = if (apps == "all") {
                mutableListOf()
            } else {
                apps.split("|").map {
                    val s = it.split("_")
                    s.getOrNull(0) to s.getOrNull(1)
                }.filter { it.first != null && it.second != null }
                    .toMutableList() as MutableList<Pair<String, String>>
            }
            targetParamId = params.getOrNull(3)
            if (targetParamId.isNullOrEmpty()) {
                targetParamId = ScriptParamEnv.getDefaultParam()?.getFullId()
            }
        }
    }

    override fun matchCondition(condition: String) = Regex(matchPattern).matches(condition)

    override fun getPermissionRequest() =
        mutableListOf(
            ScriptHelper.PERMISSION_NOTIFICATION_LISTENER,
        )


}