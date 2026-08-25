// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.beans

import android.text.TextUtils
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptMate
import com.hive.script.base.ScriptRecordHelper
import com.hive.script.base.core.ScriptParser
import com.hive.script.base.core.ScriptReader
import com.hive.script.extensions.encrypt
import com.hive.script.views.manager.ScriptMenuManager
import com.hive.utils.file.FileUtils
import java.io.File

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/17/21
 */
class ScriptInfoModel {

    //注意：是脚本文件夹路径
    var scriptPath: String? = null
        set(value) {
            field = value
            //检查是否/结尾
            if (!TextUtils.isEmpty(field) && !field!!.endsWith("/")) {
                field += "/"
            }
        }

    var scriptName: String? = null

    var scriptMate: ScriptMate? = null

    fun parseMainFile(mainFileOrPath: File?): ScriptInfoModel {
        val mainFile = mainFileOrPath?.takeIf { it.isFile } ?: mainFileOrPath?.run {
            mainFileOrPath.listFiles()?.find { it.path.endsWith(ScriptConst.SCRIPT_SUFFIX)||it.path.endsWith(ScriptConst.SCRIPT_SUFFIX_ENCRYPT) }
        }
        mainFile?.run {
            scriptPath = mainFile.parentFile?.path ?: ""
            val parentName = mainFile.parentFile.name
            scriptName = parentName
            val parser = ScriptParser()
            val reader = ScriptReader(scriptPath, null)
            scriptMate = parser.parserMate(reader)
        }
        return this
    }

    fun parseInfoFile(infoFileOrPath: File?): ScriptInfoModel {
        val infoFile = infoFileOrPath?.takeIf { it.isFile } ?: infoFileOrPath?.run {
            infoFileOrPath.listFiles()?.find { it.path.endsWith(ScriptConst.SCRIPT_SUFFIX_INFO) }
        }
        infoFile?.run {
            scriptPath = infoFile.parentFile?.path ?: ""
            val parentName = infoFile.parentFile.name
            scriptName = parentName
            val parser = ScriptParser()
            val reader = ScriptReader(scriptPath, null)
            scriptMate = parser.parserMate(reader)
        }
        return this
    }

    fun getMainFilePath(): String {
        val mainFile = scriptPath + "/" + ScriptConst.SCRIPT_MAIN_FILE_NAME
        val mainEncryptFile = scriptPath + "/" + ScriptConst.SCRIPT_MAIN_ENCRYPT_FILE_NAME
        return if (FileUtils.isFileExist(mainEncryptFile)) {
            mainEncryptFile
        } else {
            mainFile
        }
    }

    fun copy(): ScriptInfoModel {
        val model = ScriptInfoModel()
        model.scriptPath = scriptPath
        model.scriptName = scriptName
        model.scriptMate = scriptMate?.copy()
        return model
    }

    fun saveMate() {
        scriptMate?.let {
            FileUtils.writeFile(
                "${scriptPath}/${ScriptConst.SCRIPT_MAIN_INFO_FILE_NAME}",
                it.getCommandLines().encrypt()
            )
        }
    }

    suspend fun delete() {
        scriptPath ?: return
        try {
            FileUtils.clearDirectory(File(scriptPath!!), true)
            if (ScriptRecordHelper.instance.rootCommand.scriptPath.equals(scriptPath)) {
                ScriptRecordHelper.instance.reset()
                ScriptMenuManager.getMenuView()?.updateCurrentStatus()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}