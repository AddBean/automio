// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.condition

import android.text.TextUtils
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandCondition
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.utils.ScriptCommandHelper
import com.hive.utils.GlobalApp
import com.hive.utils.utils.StringUtils
import java.util.regex.Matcher
import java.util.regex.Pattern

@AutoConditionRegister(type = ConditionIDS.ConditionIdParam)
class ConditionParam(val cmd: ScriptCommand) : ScriptCommandCondition(cmd) {

    private var matchPattern = """checkParam\((.*)\)"""

    //是否等于、是否大于、是否小于、是否包含、是否被包含
    var action: String? = "contains"//contains,equals,greater,less,beContains

    var paramId: String? = ""

    var value: String? = ""

    override fun isMeet(cmd: ScriptCommand?): Boolean {
        val realValue = getRealValue()
        val paramValue = getParamValue()
        when (action) {
            "contains" -> {
                return paramValue.contains(realValue)
            }

            "equals" -> {
                return paramValue == realValue
            }

            "greater" -> {
                return (paramValue.toIntOrNull() ?: 0) > (realValue.toIntOrNull() ?: 0)
            }

            "less" -> {
                return (paramValue.toIntOrNull() ?: 0) < (realValue.toIntOrNull() ?: 0)
            }

            "isEmpty" -> {
                return TextUtils.isEmpty(paramValue)
            }

            "beContains" -> {
                return realValue.contains(paramValue)
            }
        }
        return false
    }


    override fun doPostAction(action: String) {

    }

    private fun getRealValue(): String {
        return cmd.parseParamText(value) ?: ""
    }

    private fun getParamValue(): String {
        return cmd.getParamEnv().readParam(paramId) ?: ""
    }

    override fun getCondition() =
        "checkParam($action,$paramId,\"${
            StringUtils.encoding(value)
        }\")"

    override fun getConditionName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_condition_param_name)

    override fun getConditionDesc() =
        GlobalApp.getString(
            com.hive.i8n.R.string.cmd_name_condition_param_des,
            getParamName(),
            getActionName(),
            ScriptParamEnv.getParam(value)?.name ?: value
        )


    override fun parseCondition(condition: String) {
        val r: Pattern = Pattern.compile(matchPattern)
        val m: Matcher = r.matcher(condition)
        if (m.find()) {
            val params = ScriptCommandHelper.splitParams(m.group(1)?.toString())
            action = params.getOrNull(0)
            paramId = params.getOrNull(1)
            value = StringUtils.decoding(params.getOrNull(2))
        }
    }

    fun getActionName(): String {
        return actionMapParam[action] ?: action ?: ""
    }

    fun getActionValue(): String? {
        if (TextUtils.isEmpty(value)) {
            return GlobalApp.getString(com.hive.i8n.R.string.sc_condition_param_value_edit_name)
        }
        return value
    }

    fun getParamName(): String {
        return cmd.getParamEnv().getParmaName(paramId)?.name
            ?: ScriptParamEnv.getDefaultParam()?.name
            ?: GlobalApp.getString(com.hive.i8n.R.string.sc_condition_param_selecter_name)
    }

    override fun matchCondition(condition: String) = Regex(matchPattern).matches(condition)

    override fun getPermissionRequest() = mutableListOf<String>()


}