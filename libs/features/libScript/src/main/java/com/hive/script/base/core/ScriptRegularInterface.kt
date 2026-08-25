// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.core

import com.hive.script.cmd.AutoCmdRegister

/**
 * 脚本命令正则匹配接口。
 *
 * 命令匹配顺序：commandMap 按 cmdPrefix() 长度升序排列，
 * 使 findLast 能命中最具体命令（如 clickImage 优于 click）。
 *
 * 命令名来源（优先级）：@AutoCmdRegister(name) > 类名推导（CmdXxx → xxx）
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
interface ScriptRegularInterface {

    fun parseCmd(cmd: String)

    /**
     * 命令匹配。默认实现：由 Tokenizer 提取命令名，与 cmdPrefix() 比较。
     * 需自定义逻辑的命令（如 ScriptMate、CmdIf）可 override。
     */
    fun matchCmd(cmd: String): Boolean = ScriptLineTokenizer.getCommandName(cmd.trim()) == cmdPrefix()

    /**
     * 命令前缀，用于 commandMap 排序（短→长），确保 findLast 命中最具体命令。
     * 优先读取 @AutoCmdRegister(name)，非空则用之；否则从类名推导：CmdXxx → xxx。
     * 无注解或需特殊逻辑的实现（如 ScriptMate）可 override。
     */
    fun cmdPrefix(): String {
        val ann = this::class.java.getAnnotation(AutoCmdRegister::class.java)
        if (ann != null && ann.name.isNotEmpty()) return ann.name
        val name = this::class.java.simpleName
        return when {
            name.startsWith("Cmd") -> name.removePrefix("Cmd").replaceFirstChar { it.lowercase() }
            else -> name.replaceFirstChar { it.lowercase() }
        }
    }
}