// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.params

import com.hive.script.utils.ScriptCommandHelper
import com.hive.utils.GlobalApp
import com.hive.utils.global.MMKVTools
import com.hive.utils.utils.StringUtils

class ScriptParamEnv private constructor() {

    init {
        initDefaultParams()
    }

    private fun initDefaultParams() {
        loadMainParams()
    }

    private fun loadMainParams() {
        if (!hasGroup(sysGroupId)) {
            //系统变量
            paramGroup.add(
                ScriptParamGroup(
                    sysGroupId,
                    GlobalApp.getString(com.hive.i8n.R.string.sc_params_group_sys),
                    mutableListOf()
                )
            )
        }

        addParam(
            ScriptSystemParam.OUTPUT1.paramId,
            ScriptSystemParam.OUTPUT1.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.OUTPUT1.writable
            readable = ScriptSystemParam.OUTPUT1.readable
            desc = ScriptSystemParam.OUTPUT1.desc
        }

        addParam(
            ScriptSystemParam.OUTPUT2.paramId,
            ScriptSystemParam.OUTPUT2.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.OUTPUT2.writable
            readable = ScriptSystemParam.OUTPUT2.readable
            desc = ScriptSystemParam.OUTPUT2.desc
        }

        addParam(
            ScriptSystemParam.OUTPUT3.paramId,
            ScriptSystemParam.OUTPUT3.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.OUTPUT3.writable
            readable = ScriptSystemParam.OUTPUT3.readable
            desc = ScriptSystemParam.OUTPUT3.desc
        }

        addParam(
            ScriptSystemParam.CLIPBOARD.paramId,
            ScriptSystemParam.CLIPBOARD.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.CLIPBOARD.writable
            readable = ScriptSystemParam.CLIPBOARD.readable
            desc = ScriptSystemParam.CLIPBOARD.desc
        }

        addParam(
            ScriptSystemParam.TOAST.paramId,
            ScriptSystemParam.TOAST.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.TOAST.writable
            readable = ScriptSystemParam.TOAST.readable
            desc = ScriptSystemParam.TOAST.desc
        }

        addParam(
            ScriptSystemParam.LOCATION.paramId,
            ScriptSystemParam.LOCATION.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.LOCATION.writable
            readable = ScriptSystemParam.LOCATION.readable
            desc = ScriptSystemParam.LOCATION.desc
        }

        addParam(
            ScriptSystemParam.RANDOM.paramId,
            ScriptSystemParam.RANDOM.paramName,
            "0"
        )?.apply {
            writable = ScriptSystemParam.RANDOM.writable
            readable = ScriptSystemParam.RANDOM.readable
            desc = ScriptSystemParam.RANDOM.desc
        }

        addParam(
            ScriptSystemParam.GRANTED_PERMISSIONS.paramId,
            ScriptSystemParam.GRANTED_PERMISSIONS.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.GRANTED_PERMISSIONS.writable
            readable = ScriptSystemParam.GRANTED_PERMISSIONS.readable
            desc = ScriptSystemParam.GRANTED_PERMISSIONS.desc
        }

        addParam(
            ScriptSystemParam.TIMESTAMP.paramId,
            ScriptSystemParam.TIMESTAMP.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.TIMESTAMP.writable
            readable = ScriptSystemParam.TIMESTAMP.readable
            desc = ScriptSystemParam.TIMESTAMP.desc
        }

        addParam(
            ScriptSystemParam.DATETIME.paramId,
            ScriptSystemParam.DATETIME.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.DATETIME.writable
            readable = ScriptSystemParam.DATETIME.readable
            desc = ScriptSystemParam.DATETIME.desc
        }


        addParam(
            ScriptSystemParam.DEVICE.paramId,
            ScriptSystemParam.DEVICE.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.DEVICE.writable
            readable = ScriptSystemParam.DEVICE.readable
            desc = ScriptSystemParam.DEVICE.desc
        }

        addParam(
            ScriptSystemParam.RESOLUTION.paramId,
            ScriptSystemParam.RESOLUTION.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.RESOLUTION.writable
            readable = ScriptSystemParam.RESOLUTION.readable
            desc = ScriptSystemParam.RESOLUTION.desc
        }

        addParam(
            ScriptSystemParam.BRAND.paramId,
            ScriptSystemParam.BRAND.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.BRAND.writable
            readable = ScriptSystemParam.BRAND.readable
            desc = ScriptSystemParam.BRAND.desc
        }

        addParam(
            ScriptSystemParam.MODEL.paramId,
            ScriptSystemParam.MODEL.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.MODEL.writable
            readable = ScriptSystemParam.MODEL.readable
            desc = ScriptSystemParam.MODEL.desc
        }

        addParam(
            ScriptSystemParam.OS_VERSION.paramId,
            ScriptSystemParam.OS_VERSION.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.OS_VERSION.writable
            readable = ScriptSystemParam.OS_VERSION.readable
            desc = ScriptSystemParam.OS_VERSION.desc
        }

        addParam(
            ScriptSystemParam.OS_CODE.paramId,
            ScriptSystemParam.OS_CODE.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.OS_CODE.writable
            readable = ScriptSystemParam.OS_CODE.readable
            desc = ScriptSystemParam.OS_CODE.desc
        }

        addParam(
            ScriptSystemParam.COUNTY.paramId,
            ScriptSystemParam.COUNTY.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.COUNTY.writable
            readable = ScriptSystemParam.COUNTY.readable
            desc = ScriptSystemParam.COUNTY.desc
        }

        addParam(
            ScriptSystemParam.LANG.paramId,
            ScriptSystemParam.LANG.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.LANG.writable
            readable = ScriptSystemParam.LANG.readable
            desc = ScriptSystemParam.LANG.desc
        }

        addParam(
            ScriptSystemParam.FOREGROUND_PKG.paramId,
            ScriptSystemParam.FOREGROUND_PKG.paramName,
            ""
        )?.apply {
            writable = ScriptSystemParam.FOREGROUND_PKG.writable
            readable = ScriptSystemParam.FOREGROUND_PKG.readable
            desc = ScriptSystemParam.FOREGROUND_PKG.desc
        }


        if (!hasGroup(mainGroupId)) {
            //主变量
            paramGroup.add(
                ScriptParamGroup(
                    mainGroupId,
                    GlobalApp.getString(com.hive.i8n.R.string.sc_params_group_main),
                    mutableListOf()
                )
            )
        }
        val params = GlobalApp.getStringArray(com.hive.i8n.R.array.sc_params_group_list_main)
        params.forEachIndexed { index, name ->
            addParamInner(mainGroupId, "param${index + 1}", name, "")
        }
    }


    fun getCommandLines(): String {
        val sb = StringBuilder()
        getGroups().forEach {
            sb.append(it.getCommandLines())
            if (it != getGroups().last()) {
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    fun matchCmd(line: String): Boolean {
        return line.startsWith("def")
    }

    /**
     * 添加变量
     */
    fun addParam(p: ScriptParam) {
        val group = paramGroup.find { it.id == p.groupId }
        if (group == null) {
            val group = ScriptParamGroup(p.groupId, p.groupId, mutableListOf())
            paramGroup.add(group)
        }
        addParamInner(p.groupId, p.id, p.name, p.initValue)
    }

    /**
     * 添加变量
     */
    fun addParam(fullParamId: String, name: String, content: String): ScriptParam? {
        val group = fullParamId.substringBefore(".")
        val param = fullParamId.substringAfter(".")
        return addParamInner(group, param, name, content)
    }

    /**
     * 添加变量
     */
    private fun addParamInner(
        groupId: String,
        paramId: String,
        name: String,
        content: String
    ): ScriptParam? {
        val group = paramGroup.find { it.id == groupId } ?: return null
        val fullId = "$groupId.$paramId"
        if (getParam(fullId) != null) return null
        val localValue = loadFromMMKVIfExists(fullId)
        val initVal = if (localValue != null) localValue else content
        val param = ScriptParam(groupId, paramId, name, initVal, initVal)
        group.params.add(param)
        paramMap[fullId] = param
        return param
    }

    /**
     * 写入变量
     */
    fun writeParamInit(paramFullId: String, initValue: String) {
        val group = paramFullId.substringBefore(".")
        val param = paramFullId.substringAfter(".")
        writeParamInit(group, param, initValue)
    }

    /**
     * 写入变量
     * @param persist 为 true 时同时写入 MMKV 本地存储，下次初始化可从本地读取；默认 false，线上兼容
     */
    fun writeParam(paramFullId: String, content: String?, isInit: Boolean = false, persist: Boolean = false) {
        val group = paramFullId.substringBefore(".")
        val param = paramFullId.substringAfter(".")
        writeParam(group, param, content ?: "", isInit, persist)
    }

    /**
     * 写入变量
     * @param persist 为 true 时同时写入 MMKV 本地存储；默认 false
     */
    fun writeParam(groupId: String, paramId: String, content: String?, isInit: Boolean = false, persist: Boolean = false) {
        val fullId = "$groupId.$paramId"
        val group = paramGroup.find { it.id == groupId }
        if (group != null) {
            val param = group.params.find { it.id == paramId }
            param?.write(content ?: "", isInit)
            if (persist) {
                MMKVTools.putScriptParamString(MMKV_PARAM_PREFIX + fullId, content ?: "")
            }
        }
    }

    /**
     * 写入变量初始值
     */
    fun writeParamInit(groupId: String, paramId: String, initValue: String?) {
        val group = paramGroup.find { it.id == groupId }
        if (group != null) {
            val param = group.params.find { it.id == paramId }
            param?.initValue = initValue ?: ""
        }
    }

    /**
     * 读取变量组
     */
    fun readGroup(groupId: String): ScriptParamGroup? {
        return paramGroup.find { it.id == groupId }
    }

    /**
     * 读取变量组
     */
    fun getGroups(): List<ScriptParamGroup> {
        return paramGroup
    }

    /**
     * 是否有变量组
     */
    fun hasGroup(groupId: String): Boolean {
        return paramGroup.any { it.id == groupId }
    }

    /**
     * 解析变量
     */
    fun parseParam(line: String) {
        val regexGroup = """def\s+group\s+(\w+)\s+#(.*)""".toRegex()
        val regexParam = """def\s+(.*)="(.*)"\s+#(.*)""".toRegex()
        val groupMatch = regexGroup.find(line)
        val paramMatch = regexParam.find(line)
        if (groupMatch != null) {
            val groupId = groupMatch.groupValues[1]
            val groupName = groupMatch.groupValues[2]
            val group = ScriptParamGroup(groupId, groupName, mutableListOf())
            if (!hasGroup(group.id)) {
                paramGroup.add(group)
            }
        } else if (paramMatch != null) {
            val paramId = paramMatch.groupValues[1]
            val paramValue = StringUtils.decoding(paramMatch.groupValues[2])
            val paramName = paramMatch.groupValues[3]
            val regexParamId = """(.*)\.(.*)""".toRegex()
            val paramIdMatch = regexParamId.find(paramId)
            if (paramIdMatch != null) {
                val groupId = paramIdMatch.groupValues[1]
                val id = paramIdMatch.groupValues[2]
                val group = paramGroup.find { it.id == groupId }
                if (group != null) {
                    if (!group.hasParam(id)) {
                        addParamInner(groupId, id, paramName, paramValue)
                    } else {
                        writeParamInit(groupId, id, paramValue)
//                        writeParam(groupId, id, paramValue, true)
                    }
                }
            }
        }
    }

    /**
     * 获取变量
     */
    fun getParmaName(paramId: String?): ScriptParam? {
        return paramMap[paramId]
    }

    /**
     * 读取变量,格式：\${变量组名.变量名},递归解析,最多解析8层
     */
    fun parseParamText(text: String?, depth: Int): String? {
        if (text == null || depth >= 8) return text
        val content = StringUtils.decoding(text)
        return if (isParamText(content)) {
            val result = ScriptCommandHelper.parseParamValue(this, content)
            if (isParamText(result)) {
                parseParamText(result, depth + 1)
            } else {
                result
            }
        } else {
            content
        }
    }

    /**
     * 合并变量，将上一次的变量组合并到当前变量中
     */
    fun combineEnv(lastEnv: ScriptParamEnv?) {
        lastEnv?.getGroups()?.forEach { group ->
            val newGroup = readGroup(group.id)
            if (newGroup == null) {
                paramGroup.add(group)
            }
        }
        updateParamMap()
    }


    /**
     * 更新变量映射
     */
    private fun updateParamMap() {
        paramMap.clear()
        paramGroup.forEach { group ->
            group.params.forEach { param ->
                paramMap["${group.id}.${param.id}"] = param
            }
        }
    }

    /**
     * 读取变量
     */
    fun readParam(paramId: String?): String? {
        return paramMap[paramId]?.read()
    }

    companion object {
        const val sysGroupId = "sys"

        const val mainGroupId = "main"

        private const val MMKV_PARAM_PREFIX = "script_param_"

        private fun loadFromMMKVIfExists(fullId: String): String? =
            MMKVTools.getScriptParamString(MMKV_PARAM_PREFIX + fullId, null)

        private val paramGroup = mutableListOf<ScriptParamGroup>()

        private val paramMap = mutableMapOf<String, ScriptParam>()

        private val instance = ScriptParamEnv()

        fun getDefault(): ScriptParamEnv {
            return instance
        }

        fun getGroupList(): List<ScriptParamGroup> {
            return paramGroup
        }

        /**
         * 获取变量环境
         */
        fun getParamEnv(): ScriptParamEnv {
            return getDefault()
        }

        /**
         * 获取变量
         */
        fun getParam(id: String?): ScriptParam? {
            id ?: return null
            return paramMap[id]
        }

        /**
         * 获取默认变量
         */
        fun getDefaultParam(): ScriptParam? {
            return paramMap.values.firstOrNull { it.groupId == mainGroupId }
        }

        /**
         * 获取默认系统变量
         */
        fun getDefaultSysParam(): ScriptParam? {
            return paramMap.values.firstOrNull { it.groupId == sysGroupId }
        }

        /**
         * 获取默认输出变量
         */
        fun getOutputParam1(): ScriptParam? {
            return ScriptSystemParam.OUTPUT1.getParam()
        }

        /**
         * 获取默认输出变量
         */
        fun getOutputParam2(): ScriptParam? {
            return ScriptSystemParam.OUTPUT2.getParam()
        }

        /**
         * 获取默认输出变量
         */
        fun getOutputParam3(): ScriptParam? {
            return ScriptSystemParam.OUTPUT3.getParam()
        }


        /**
         * 是否是变量，如果格式含有：${变量组名.变量名}
         */
        fun isParam(text: String?): Boolean {
            text ?: return false
            return ScriptCommandHelper.paramRegex.containsMatchIn(text)
        }

        /**
         * 是否是变量，如果格式含有：${变量组名.变量名}
         */
        fun isParamText(txt: String?): Boolean {
            val text = StringUtils.decoding(txt)
            return isParam(text)
        }

        fun parseParamsId(atText: String): String {
            return ScriptCommandHelper.parseParamsId(atText)
        }
    }
}