// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.core

import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptKeyStoreManager
import com.hive.script.extensions.decrypt
import com.hive.script.utils.ScriptHelper
import com.hive.utils.GlobalApp
import com.hive.utils.file.FileUtils
import com.hive.views.widgets.CommonToast
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
class ScriptReader(var scriptPath: String?, cmdList: List<String>?) : IScriptReader {

    init {
        // 仅从文件加载时校验 path；从命令列表加载时 scriptPath 可为 null
        if (cmdList == null) {
            ScriptHelper.checkScriptPath(scriptPath)
        }
    }

    private var commandList = cmdList ?: parserPath()

    private fun parserPath(): List<String> {

        var baseDir = scriptPath ?: return mutableListOf()
        if (File(baseDir).isFile) {
            baseDir = File(scriptPath).parentFile?.path ?: return mutableListOf()
        }

        //找出主程序文件
        val mainScriptFile = File(baseDir).listFiles()
            ?.find { it.name.endsWith(ScriptConst.SCRIPT_SUFFIX) || it.name.endsWith(ScriptConst.SCRIPT_SUFFIX_ENCRYPT) }?.path

        //找出主程序info文件
        val infoScriptFile = File(baseDir).listFiles()
            ?.find { it.name.endsWith(ScriptConst.SCRIPT_SUFFIX_INFO) }?.path

        var key: String? = null
        var parserPath = mainScriptFile
        //查找主程序解密 key
        if (mainScriptFile?.endsWith(ScriptConst.SCRIPT_SUFFIX_ENCRYPT) == true) {
            key = ScriptKeyStoreManager.findKey(baseDir)
            //说明未找到 key，则降级为解析 info
            if (key == null) {
                parserPath = infoScriptFile
            }
        }

        val inputStream = when {
            //如果是assets文件，直接读取
            parserPath?.startsWith("assets") == true -> {
                GlobalApp.getApp().assets.open(parserPath!!)
            }
            else -> {
                if (!FileUtils.isFileExist(parserPath)) {
                    CommonToast.show(com.hive.i8n.R.string.sc_error_file_not_exist)
                    return emptyList()
                }
                FileInputStream(File(parserPath!!))
            }
        }
        return InputStreamReader(inputStream).use { reader ->
            val text = reader.readText()
            val decodeText = if (parserPath?.endsWith(ScriptConst.SCRIPT_SUFFIX_INFO) == true) {
                if (text.contains("mate")) {
                    text
                } else {
                    text.decrypt()
                }
            } else if (key != null) {
                text.decrypt(key)
            } else {
                text
            }
            decodeText.split("\n").filterNot { it == "" }
        }
    }

    private var mCurrentLine = 0

    override fun readLine(): String? {
        if (mCurrentLine >= commandList.size) return null
        mCurrentLine++
        return commandList[mCurrentLine - 1].trim()
    }

    override fun getCurrentLine(): Int = mCurrentLine

    override fun reset() {
        mCurrentLine = 0
    }

    override fun backLine() {
        mCurrentLine--
        if (mCurrentLine < 0) mCurrentLine = 0
    }
}