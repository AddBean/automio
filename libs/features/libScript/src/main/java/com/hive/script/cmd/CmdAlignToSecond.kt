// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.utils.GlobalApp
import com.hive.script.base.core.ScriptLineTokenizer

@AutoCmdRegister(type = IDS.CmdAlignToSecond, name = "alignToSecond")
class CmdAlignToSecond : ScriptCommand(), ScriptRegularInterface {

    private val toleranceValue = 20L//计算误差在20ms内，必须大于5ms

    var value: Int? = null

    override fun onExecute(): CmdExecuteResult {
        while (!checkIsOnTime(value ?: 0)) {
            ScriptThreadManager.delay(5)
        }
        ScriptThreadManager.delay(toleranceValue)
        return CmdExecuteResult.success()
    }

    /**
     * 检查当前秒时间是否是value的整倍数，value最大为60s，60s即为1分钟整点，计算误差在${toleranceValue}ms内
     */
    private fun checkIsOnTime(value: Int): Boolean {
        val time = System.currentTimeMillis() / 1000
        return (time % value).toInt() == 0 && System.currentTimeMillis() % 1000 < toleranceValue
    }


    override fun getCommandName() = GlobalApp.getString(com.hive.i8n.R.string.cmd_name_time_calibrator)

    override fun getCommandDescribe() =
        GlobalApp.getString(com.hive.i8n.R.string.cmd_name_time_calibrator_des, valueMap[value] ?: "${value}s")

    override fun getCommand() = "${cmdPrefix()} seconds=$value"

    override fun parseCmd(cmd: String) {
        value = ScriptLineTokenizer.parseKeyValueParams(cmd)["seconds"]?.toIntOrNull()
    }

    override fun getPermissionRequest() = null

    override fun getCommandIcon() = R.drawable.sc_time_calibrator

    companion object {

        val valueMap = mapOf(
            5 to GlobalApp.getString(com.hive.i8n.R.string.cmd_name_time_calibrator_5s),
            10 to GlobalApp.getString(com.hive.i8n.R.string.cmd_name_time_calibrator_10s),
            30 to GlobalApp.getString(com.hive.i8n.R.string.cmd_name_time_calibrator_30s),
            60 to GlobalApp.getString(com.hive.i8n.R.string.cmd_name_time_calibrator_60s),
        )

        fun createCommand(value: Int) = CmdAlignToSecond().apply {
            this.value = value
        }

    }
}