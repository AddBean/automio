// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.condition

import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandCondition
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.utils.ScriptHelper
import com.hive.script.utils.ScriptPermissionManager
import com.hive.utils.GlobalApp
import java.util.regex.Pattern

@AutoConditionRegister(type = ConditionIDS.ConditionIdPermission)
class ConditionPermission(val cmd: ScriptCommand) : ScriptCommandCondition(cmd) {

    private val matchPattern = """checkPermission\((.*)\)"""

    var permissionList: MutableList<String> = mutableListOf(ScriptHelper.PERMISSION_CAPTURE)

    override fun isMeet(cmd: ScriptCommand?): Boolean {
        if (permissionList.isEmpty()) return false
        val missed = ScriptPermissionManager.checkMissedPermissions(permissionList)
        return missed.isEmpty()
    }

    override fun doPostAction(action: String) {
        // 权限检测无后续动作
    }

    override fun getCondition(): String {
        val encoded = permissionList
            .map { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" }
            .joinToString(",")
        return "checkPermission($encoded)"
    }

    override fun getConditionName() =
        GlobalApp.getString(com.hive.i8n.R.string.cmd_name_condition_permission_name)

    override fun getConditionDesc() =
        GlobalApp.getString(
            com.hive.i8n.R.string.cmd_des_condition_permission_des,
            getPermissionDisplayText()
        )

    override fun parseCondition(condition: String) {
        val r = Pattern.compile(matchPattern)
        val m = r.matcher(condition)
        if (m.find()) {
            val params = ScriptCommandHelper.splitParams(m.group(1))
            permissionList = params
                .mapNotNull { ScriptCommandHelper.parseParamString(it).trim() }
                .filter { it.isNotEmpty() }
                .toMutableList()
            if (permissionList.isEmpty()) {
                permissionList = mutableListOf(ScriptHelper.PERMISSION_CAPTURE)
            }
        }
    }

    override fun matchCondition(condition: String) = Regex(matchPattern).matches(condition)

    override fun getPermissionRequest() = null

    fun getPermissionDisplayText(): String =
        permissionList.joinToString(",") { ScriptHelper.mPermissionMap[it] ?: it }


    companion object {
        fun getAllPermissionPairs(): List<Pair<String, String>> =
            ScriptHelper.mPermissionMap.map { (fullKey, displayName) -> displayName to fullKey }
    }
}
