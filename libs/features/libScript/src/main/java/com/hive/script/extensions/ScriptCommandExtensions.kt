// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.extensions

import android.text.TextUtils
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommand.CmdExecuteResult
import com.hive.script.base.ScriptMate
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.cmd.AutoCmdRegister
import com.hive.script.cmd.CmdBreak
import com.hive.script.cmd.CmdFor
import com.hive.script.cmd.CmdJump
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.edit.xeditor.utils.XEditorSnapManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string
import com.hive.utils.utils.StringUtils
import java.io.File
import java.util.Date

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/7/21
 */

fun ScriptCommand.getType(): Int {
    if (type > -1) return type
    val register: AutoCmdRegister? = javaClass.getAnnotation(AutoCmdRegister::class.java)
    type = register?.type ?: -1
    return type
}

fun <T : ScriptCommand> T.getIndexInParent(): Int {
    val index = parentCommand?.commandQueue?.indexOf(this) ?: 0
    return if (index < 0) 1 else (index + 1)
}

@Synchronized
fun <T : ScriptCommand> T.forEachAllCommand(cb: (c: ScriptCommand) -> Unit) {
    cb.invoke(this)
    if (commandQueue.isNotEmpty()) {
        commandQueue.forEach {
            it.forEachAllCommand(cb)
        }
        return
    }
}

fun ScriptCommand.updateChildParent() {
    commandQueue.forEach {
        it.parentCommand = this
        it.updateChildParent()
    }
}

/**
 * 提取第一个命令行，if、for等语句的注释等在第一行，因此相关信息应该单独解析第一行
 */
fun ScriptCommand.getCommandFirstLine(): String {
    return this.getCommandLines().split("\n")?.firstOrNull() ?: ""
}

/**
 * 替换自身
 */
fun ScriptCommand.replaceTo(targetCmd: ScriptCommand) {
    val parentQueue = this.parentCommand?.commandQueue
    parentQueue ?: return
    val index = parentQueue.indexOf(this)
    if (index < 0) return
    parentQueue[index] = targetCmd
//    targetCmd.parentCommand = this.parentCommand
    updateAllParent()
    XEditorSnapManager.get().save(this)
}

/**
 * 遍历子树
 */
fun ScriptCommand.traverseCommand(
    cmd: ScriptCommand? = null,
    innerLoop: Boolean = false,
    callback: (cmd: ScriptCommand) -> Unit
) {
    val targetCmd = cmd ?: if (innerLoop) null else this
    targetCmd?.run {
        callback.invoke(targetCmd)
    }
    if (targetCmd?.commandQueue?.isNotEmpty() == true) {
        repeat(targetCmd.commandQueue.size) {
            traverseCommand(targetCmd.commandQueue[it], true, callback)
        }
    }
}

fun ScriptCommand.findRootCommand(): ScriptCommand {
    return if (parentCommand == null) {
        this
    } else {
        parentCommand!!.findRootCommand()
    }
}


fun ScriptCommand.isLastCommand(): Boolean {
    val parentQueue = parentCommand?.commandQueue
    parentQueue ?: return true
    val index = parentQueue.indexOf(this)
    return index == parentQueue.size - 1
}

fun ScriptCommand.updateAllParent() {
    return findRootCommand().updateParent()
}

/**
 * 更新父级
 */
fun ScriptCommand.updateParent() {
    //确保parent不为空
    traverseCommand { cmd ->
        if (cmd.parentCommand == null) {
            traverseCommand {
                if (it.commandQueue.contains(cmd)) {
                    cmd.parentCommand = it
                    return@traverseCommand
                }
            }
        }
    }
}

fun ScriptCommand.moveDown(): Boolean {
    val parentQueue = this.parentCommand?.commandQueue
    parentQueue ?: return false
    val index = parentQueue.indexOf(this)
    if (index < 0) return false
    if (index == parentQueue.size - 1) return false
    parentQueue.add(index + 1, this)
    XEditorSnapManager.get().save(this)
    updateAllParent()
    parentQueue.removeAt(index)
    return true
}

fun ScriptCommand.moveUp(): Boolean {
    val parentQueue = this.parentCommand?.commandQueue
    parentQueue ?: return false
    val index = parentQueue.indexOf(this)
    if (index < 0) return false
    if (index == 0) return false
    parentQueue.removeAt(index)
    parentQueue.add(index - 1, this)
    updateAllParent()
    XEditorSnapManager.get().save(this)
    return true
}

fun ScriptCommand.isParent(parent: ScriptCommand): Boolean {
    var temp = this
    while (temp.parentCommand != null) {
        if (temp.parentCommand == parent) {
            return true
        }
        temp = temp.parentCommand!!
    }
    return false
}

fun ScriptCommand.isContainedUnReachable(): Boolean {
    var contains = false
    this.forEachAllCommand {
        if (it.unReachable) {
            contains = true
            return@forEachAllCommand
        }
    }
    return contains
}

fun ScriptCommand.isUnReachable(): Boolean {
    if (this.parentCommand?.unReachable == true) return true
    val queue = this.parentCommand?.commandQueue ?: return false
    val index = queue.indexOf(this)
    if (index == 0) return false
    val lastLoopCmds = mutableListOf<CmdFor>()
    //是否有跳转指令,如果有则视为都可达
    var hasJump = false
    this.getRootScript()?.forEachAllCommand {
        if (it is CmdJump) {
            hasJump = true
            return@forEachAllCommand
        }
    }
    if (hasJump) {
        return false
    }
    for (i in 0 until index) {
        val c = queue.getOrNull(i) ?: return false
        //如果同级命令前有跳出指令，则说明也不可达
        if (c is CmdBreak) {
            return !hasJump
        }
        if (c is CmdFor) {
            if (c.loopCount == -1 || c.loopCount == 0) {
                lastLoopCmds.add(c)
            }
        }
    }
    lastLoopCmds.forEach {
        if (!it.isCanBeBreak()) {
            return !hasJump
        }
    }
    return false
}

/**
 * 是否可以被break
 */
fun ScriptCommand.isCanBeBreak(): Boolean {
    this.commandQueue.find { it is CmdBreak }?.let {
        return true
    }
    val breakFor = this.commandQueue.filter { it !is CmdFor }.find { it.isCanBeBreak() }
    return breakFor != null
}

fun ScriptMate.getInfoMap(scriptFullPath: String?): MutableMap<String, String> {
    val scriptMate = this
    var permission = ScriptHelper.mPermissionMap.toList()
        .filter { scriptMate.permission?.contains(it.first) == true }.map { it.second }
        .joinToString(separator = ",")
    if (TextUtils.isEmpty(permission)) {
        permission = GlobalApp.getString(com.hive.i8n.R.string.sc_permission_none)
    }

    fun switchValue(boolean: Boolean): String {
        return if (boolean) {
            com.hive.i8n.R.string.sc_switch_on.string()
        } else {
            com.hive.i8n.R.string.sc_switch_off.string()
        }
    }

    val map = mutableMapOf<String, String>()
    map[GlobalApp.getString(com.hive.i8n.R.string.sc_script_info_0)] = "" + scriptMate.version
    if (scriptFullPath != null) {
        map[GlobalApp.getString(com.hive.i8n.R.string.sc_script_info_1)] = File(scriptFullPath).path
    }
    map[GlobalApp.getString(com.hive.i8n.R.string.sc_script_info_3)] = "${scriptMate.device}"
    map[GlobalApp.getString(com.hive.i8n.R.string.sc_script_info_4)] =
        "${scriptMate.width}x${scriptMate.height}"
    map[GlobalApp.getString(com.hive.i8n.R.string.sc_script_info_5)] = permission
    map[GlobalApp.getString(com.hive.i8n.R.string.sc_script_info_2)] = StringUtils.dateFormat(
        Date(scriptMate.updateTime)
    )

    if (scriptMate.expireTime > 0) {
        map[GlobalApp.getString(com.hive.i8n.R.string.sc_share_ctr_expire_type_title)] = StringUtils.dateFormat(
            Date(scriptMate.expireTime)
        )
    } else {
        map[GlobalApp.getString(com.hive.i8n.R.string.sc_share_ctr_expire_type_title)] =
            com.hive.i8n.R.string.sc_time_day_none.string()
    }


    map[GlobalApp.getString(com.hive.i8n.R.string.sc_script_info_pwd)] = switchValue(scriptMate.isEncrypt())

    map[GlobalApp.getString(com.hive.i8n.R.string.sc_share_ctr_run_type_title)] =
        switchValue(scriptMate.hasControlRun())

    map[GlobalApp.getString(com.hive.i8n.R.string.sc_share_ctr_view_type_title)] =
        switchValue(scriptMate.hasControlView())

    map[GlobalApp.getString(com.hive.i8n.R.string.sc_share_ctr_edit_type_title)] =
        switchValue(scriptMate.hasControlEdit())

    map[GlobalApp.getString(com.hive.i8n.R.string.sc_share_ctr_share_type_title)] =
        switchValue(scriptMate.hasControlShare())

    map[GlobalApp.getString(com.hive.i8n.R.string.sc_share_ctr_cloud_type_title)] =
        switchValue(scriptMate.hasControlCloud())



    return map
}


/**
 * 根据在tree中位置获取id
 */
fun ScriptCommand.getTreeId(): String {
    val idList = mutableListOf<Int>()
    var temp = this
    while (temp.parentCommand != null) {
        val index = temp.getIndexInParent()
        idList.add(index)
        temp = temp.parentCommand!!
    }
    idList.reverse()
    return idList.joinToString(separator = ".")
}

/**
 * 获取总命令个数
 */
fun ScriptCommand.count(): Int {
    var count = 1
    this.forEachAllCommand {
        count++
    }
    return count
}

/**
 * 同步执行
 */
fun ScriptCommand.executeSync(): CmdExecuteResult {
    ScriptRecordManager.getRecordInnerView()?.addCommandView(this)
    ScriptInterpreterObserver.notifyCommandExecuteBefore(this)
    val result = this.doExecute()
    ScriptInterpreterObserver.notifyCommandExecuteAfter(this)
    return result
}