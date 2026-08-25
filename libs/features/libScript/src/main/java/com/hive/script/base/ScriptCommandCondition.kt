// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base

import com.hive.script.R
import com.hive.script.base.core.ScriptParser
import com.hive.script.utils.ScriptHelper
import com.hive.utils.GlobalApp
import java.io.Serializable

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
abstract class ScriptCommandCondition(cmd: ScriptCommand) : Serializable {

    var command: ScriptCommand = cmd

    abstract fun getCondition(): String

    abstract fun getConditionName(): String

    abstract fun getConditionDesc(): String

    abstract fun parseCondition(cmd: String)

    abstract fun matchCondition(condition: String): Boolean

    abstract fun isMeet(cmd: ScriptCommand?): Boolean

    abstract fun doPostAction(action: String)

    open fun getPermissionRequest(): List<String>? =
        mutableListOf(ScriptHelper.PERMISSION_BIND_ACCESSIBILITY_SERVICE)

    companion object {

        val Post_Action_None = "none"

        val Post_Action_Click = "click"

        val Post_Action_Long_Click = "longClick"

        val actionMap = mutableMapOf<String, String>().apply {
            put("contains", GlobalApp.getString(com.hive.i8n.R.string.cnd_action_name_notification_contains))
            put("equals", GlobalApp.getString(com.hive.i8n.R.string.cnd_action_name_notification_equals))
            put("startWith", GlobalApp.getString(com.hive.i8n.R.string.cnd_action_name_notification_start_with))
            put("endWith", GlobalApp.getString(com.hive.i8n.R.string.cnd_action_name_notification_end_with))
        }

        val ocrMap = mutableMapOf<String, String>().apply {
            put("1", GlobalApp.getString(com.hive.i8n.R.string.sc_edit_condition_view_ocr_type_1))
            put("0", GlobalApp.getString(com.hive.i8n.R.string.sc_edit_condition_view_ocr_type_0))
        }



        val actionMap2 = mutableMapOf<String, String>().apply {
            put("accurate", GlobalApp.getString(com.hive.i8n.R.string.cnd_action_name_menu_1))
            put("normal", GlobalApp.getString(com.hive.i8n.R.string.cnd_action_name_menu_2))
            put("obscure", GlobalApp.getString(com.hive.i8n.R.string.cnd_action_name_menu_3))
        }

        val actionMapColor = mutableMapOf<String, String>().apply {
            //循环到99%
            for (i in 1..50) {
                put(i.toString(), "${i}%")
            }
        }

        val actionMapImage = mutableMapOf<String, String>().apply {
            //循环到99%
            for (i in 99 downTo 20) {
                put(i.toString(), "${i}%")
            }
        }

        //contains,equals,greater,less,beContains
        val actionMapParam = mutableMapOf<String, String>().apply {
            put("contains", GlobalApp.getString(com.hive.i8n.R.string.cnd_action_name_param_contains))
            put("equals", GlobalApp.getString(com.hive.i8n.R.string.cnd_action_name_param_equals))
            put("greater", GlobalApp.getString(com.hive.i8n.R.string.cnd_action_name_param_greater))
            put("less", GlobalApp.getString(com.hive.i8n.R.string.cnd_action_name_param_less))
            put("isEmpty", GlobalApp.getString(com.hive.i8n.R.string.cnd_action_name_param_isEmpty))
            put("beContains", GlobalApp.getString(com.hive.i8n.R.string.cnd_action_name_param_be_contains))
        }

        fun getConditionEntity(cond: String?, cmd: ScriptCommand): ScriptCommandCondition? {
            cond ?: return null
            val conditionClass = ScriptParser.conditionMap.forEach { condition ->
                if (condition.matchCondition(cond)) {
                    val condNew = condition::class.java.getConstructor(ScriptCommand::class.java)
                        .newInstance(cmd) as ScriptCommandCondition
                    condNew.parseCondition(cond)
                    return condNew
                }
            }
            return conditionClass::class.java.getConstructor(ScriptCommand::class.java)
                .newInstance(cmd) as ScriptCommandCondition
        }
    }

}