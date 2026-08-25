// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.text.Editable
import android.text.TextUtils
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptCommand
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.views.widgets.ScriptSpanHelper
import com.hive.utils.extends.decode
import com.hive.utils.extends.string
import com.hive.utils.file.FileUtils
import com.hive.utils.utils.StringUtils
import okio.ByteString.Companion.decodeHex
import org.apache.commons.jexl3.JexlBuilder
import org.apache.commons.jexl3.JexlContext
import org.apache.commons.jexl3.MapContext


object ScriptCommandHelper {

    /** 变量引用格式：${group.param} */
    val paramFormat = "\${%s}"

    /** 匹配 ${} 变量引用 */
    val paramRegexString = """\$\{(.+?)\}"""

    val paramRegex = paramRegexString.toRegex()

    fun parseToRawText(s: Editable?): String {
        val spans = s?.getSpans(0, s.length, ScriptSpanHelper.ClickSpan::class.java)
        var text = s.toString()

        //把[]替换为#{rawValue}
        spans?.forEach {
            text = text.replace(it.spanText ?: "", it.rawValue ?: "")
        }
        return text
    }

    fun getFilesByRelativePaths(cmd: ScriptCommand, relativePaths: List<String>?): List<String> {
        val result = mutableListOf<String>()
        relativePaths?.forEach {
            result.add(getFileByRelativePath(cmd, it))
        }
        return result
    }

    fun getFileByRelativePath(cmd: ScriptCommand, relativePath: String): String {
        val basePath = cmd.getRootScript()?.getScriptBasePath()
        if (!TextUtils.isEmpty(basePath)) {
            if (FileUtils.isFileExist(basePath + relativePath)) {
                return basePath + relativePath
            }
        }
        return ScriptConst.Save_Script_Temp_Path + relativePath
    }

    fun splitParams(input: String?): List<String> {
        input ?: return emptyList()
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var isInsideQuotes = false

        for (char in input) {
            if (char == '"') {
                isInsideQuotes = !isInsideQuotes
            } else if (char == ',' && !isInsideQuotes) {
                result.add(current.toString())
                current = StringBuilder()
            } else {
                current.append(char)
            }
        }

        result.add(current.toString())
        return result
    }


    fun parseParamString(params: String?): String {
        return params?.replace("\"", "") ?: ""
    }

    /**
     * 替换参数名，
     * 例子："这是测试${main.params1}这是测试${main.params2}"，替换完后为："这是测试参数1这是测试参数2"，
     * 其中"main.params1"和"main.params2"为参数id，通过getParmaName(id)?.name获取参数名
     */
    fun parseParamsName(srcText: String): String {
        return ScriptParamEnv.getParamEnv()?.run {
            var result = srcText
            val matchers = paramRegex.findAll(srcText)
            matchers.forEach {
                paramRegex.find(it.value)?.groupValues?.get(1)?.let { id ->
                    val name = getParmaName(id)?.name
                    if (name != null) {
                        result = result.replace(it.value, name)
                    }
                }
            }
            result
        } ?: srcText
    }

    fun parseParamsId(srcText: String): String {
        return ScriptParamEnv.getParamEnv().run {
            val result = srcText
            val matchers = paramRegex.findAll(srcText)
            matchers.forEach {
                paramRegex.find(it.value)?.groupValues?.get(1)?.let { id ->
                    return id
                }
            }
            result
        }
    }

    /**
     * 读取变量,格式：${变量组名.变量名}
     */
    fun parseParamValue(env: ScriptParamEnv?, txt: String): String? {
        val text = StringUtils.decoding(txt)
        if (ScriptParamEnv.isParam(txt)) {
            //提取变量组名和变量名,格式：${变量组名.变量名}
            val regex = paramRegex
            val matchResults = regex.findAll(text)
            val valueMap = mutableMapOf<String, String>()
            matchResults.forEach {
                val group = it.groupValues[1].split(".")
                if (group.size == 2) {
                    val groupId = group[0]
                    val paramId = group[1]
                    val value = getParamValue(env, groupId, paramId)
                    valueMap[it.value] = value ?: "None"
                }
            }
            var result = text
            valueMap.forEach { (key, value) ->
                result = result.replace(key, value)
            }
            return result
        }
        return null
    }

    fun getParamValue(env: ScriptParamEnv?, groupId: String, paramId: String): String? {
        return env?.getGroups()?.find { it.id == groupId }?.params?.find { it.id == paramId }
            ?.read()?.decode()
    }

    //不能有空格、@、#、$、%、^、&、*、(、)、-、+、=、|、\、/、{、}、[、]、:、;、"、'、<、>、,、.、?、!、~、`、
    fun checkParamName(name: String): Boolean {
        val regex = """^[^@#$%^&*()-+=|\{\}\[\]:;"'<>,.?!\~`\\\/]*$""".toRegex()
        return regex.matches(name)
    }

    fun calculation(exp: String?): String {
        val fixedExp = exp?.replace("÷", "/")
        val jexl = JexlBuilder().create()
        val expression = jexl.createExpression(fixedExp)
        val context: JexlContext = MapContext()
        return expression.evaluate(context).toString()
    }

    fun getValueDisplayName(value: Any?): String {
        val isEmpty: Boolean = if (value is String?) {
            value.isNullOrEmpty()
        } else if (value is Map<*, *>) {
            value.isNullOrEmpty()
        } else {
            false
        }
        return if (isEmpty) {
            com.hive.i8n.R.string.sc_curl_edit_text_empty.string()
        } else {
            com.hive.i8n.R.string.sc_curl_edit_text_no_empty.string()
        }
    }
}