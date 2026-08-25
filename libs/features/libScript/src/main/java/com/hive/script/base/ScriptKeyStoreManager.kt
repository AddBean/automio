// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base

import com.hive.utils.global.SPTools
import com.hive.utils.utils.GsonHelper

object ScriptKeyStoreManager {

    private val scriptKeyList = mutableListOf<ScriptKeyBean>()

    private val spTools = SPTools.getInstance()

    fun init() {
        val keyList = spTools.getString("script_key_list", null)
        if (keyList != null) {
            val list = GsonHelper.getInstance().fromJson(keyList, Array<String>::class.java)
            list.forEach {
                val split = it.split("=")
                scriptKeyList.add(ScriptKeyBean(split[0], split[1]))
            }
        }
    }

    fun findKey(scriptPath: String?): String? {
        scriptKeyList.forEach {
            if (it.scriptPath == scriptPath) {
                return it.key
            }
        }
        return null
    }

    /**
     * 保存脚本的key，并保存到本地，如果已经存在则更新
     */
    fun saveKey(scriptPath: String?, text: String) {
        val key = findKey(scriptPath)
        if (key != null) {
            val iterator = scriptKeyList.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next.scriptPath == scriptPath) {
                    iterator.remove()
                }
            }
        }
        scriptKeyList.add(ScriptKeyBean(scriptPath!!, text))
        saveToLocal()
    }

    private fun saveToLocal() {
        val keyList = mutableListOf<String>()
        scriptKeyList.forEach {
            keyList.add("${it.scriptPath}=${it.key}")
        }
        spTools.putString("script_key_list", GsonHelper.getInstance().toJson(keyList))
    }

    data class ScriptKeyBean(
        var scriptPath: String,
        var key: String
    )
}