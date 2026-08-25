// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit

import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptMate
import com.hive.script.views.edit.xeditor.XCellLayout
import com.hive.script.views.edit.xeditor.utils.XEditorHelper
import com.hive.utils.file.FileUtils
import com.hive.utils.utils.GsonHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/15/21
 */
class ScriptEditHelper {

    var scriptMate: ScriptMate? = null
    var scriptMain: ScriptCommandRoot? = null

    @OptIn(DelicateCoroutinesApi::class)
    fun loadPath(scriptPath: String, cb: (root: ScriptCommandRoot?) -> Unit?) {
        GlobalScope.launch(Dispatchers.Main) {
            scriptMain = ScriptCommandRoot()
            ScriptCommandRoot.loadScript(scriptPath, scriptMain!!)
            XEditorHelper.setCellLayout(loadLayout(scriptPath))
            scriptMate = scriptMain?.scriptMate
            cb.invoke(scriptMain)
        }
    }

    fun loadRoot(root: ScriptCommandRoot, cb: (root: ScriptCommandRoot?) -> Unit?) {
        scriptMain = root
        scriptMate = root.scriptMate
        cb.invoke(scriptMain)
    }

    suspend fun loadLayout(scriptPath: String?): XCellLayout? {
        return withContext(Dispatchers.IO) {
            scriptPath ?: return@withContext null
            val scriptFold = File(scriptPath).path
            val layoutPath = scriptFold + File.separator + ScriptConst.SCRIPT_LAYOUT_FILE_NAME
            if (!FileUtils.isFileExist(layoutPath)) return@withContext null
            val json = FileUtils.readFile(layoutPath, StandardCharsets.UTF_8.name()).toString()
            return@withContext GsonHelper.getInstance().fromJson(json, XCellLayout::class.java)
        }

    }

    companion object {
        val instance: ScriptEditHelper by lazy {
            ScriptEditHelper()
        }

        fun get() = instance
    }


}