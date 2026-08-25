// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.net.download.sample.SimpleDownloader
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.logger.ScriptLoggerView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.utils.extends.string
import com.hive.utils.file.FileUtils

/**
 *
 * @author jiadou
 * 格式为：download url=... path=... output=p0 gallery=false
 */
@AutoCmdRegister(type = IDS.CmdDownload, name = "download")
class CmdDownload : ScriptCommand(), ScriptRegularInterface {
    var url: String = ""

    var savePath: String = ""

    var saveParamId = ScriptParamEnv.getDefaultParam()?.getFullId()

    var saveToGallery = false

    override fun onExecute() : CmdExecuteResult {
        val realUrl = parseParamText(url) ?: url
        if (realUrl.isEmpty()) {
            ScriptInterpreterObserver.notifyLogger(
                this, ScriptLoggerView.LogType.ERROR, com.hive.i8n.R.string.sc_download_failure.string()
            )
            return CmdExecuteResult.failure(
                com.hive.i8n.R.string.sc_download_failure.string()
            )
        }
        ScriptInterpreterObserver.notifyLogger(
            this, ScriptLoggerView.LogType.DEBUG, com.hive.i8n.R.string.sc_download_format.string(realUrl)
        )
        var result = -1L
        val saveFileName = FileUtils.getFileName(realUrl)
        val saveFullPath: String = if (saveFileName.isNotEmpty()) {
            savePath + saveFileName
        } else {
            savePath + System.currentTimeMillis()
        }
        try {
            result = SimpleDownloader.getInstance(GlobalApp.getContext()).downloadToFile(
                realUrl,
                saveFullPath,
                object : SimpleDownloader.OnDownloadListener {
                    override fun onDownloadUpdate(saveName: String?, var1: Long, var3: Long) {
                        if (var3 > 0) {
                            ScriptInterpreterObserver.notifyLogger(
                                this@CmdDownload,
                                ScriptLoggerView.LogType.DEBUG,
                                com.hive.i8n.R.string.sc_download_progress_format.string(
                                    ((var1 / var3) * 100).toInt()
                                )
                            )
                        }
                    }

                    override fun onFileExist(saveName: String?, isExist: Boolean) = false
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (result > 0) {
            ScriptInterpreterObserver.notifyLogger(
                this, ScriptLoggerView.LogType.DEBUG, com.hive.i8n.R.string.sc_download_success.string()
            )
            writeParam(saveParamId, saveFullPath)
            if (saveToGallery) {
                ScriptHelper.saveToGallery(saveFullPath)
            }
            return CmdExecuteResult.success(saveFullPath)
        } else {
            ScriptInterpreterObserver.notifyLogger(
                this, ScriptLoggerView.LogType.ERROR, com.hive.i8n.R.string.sc_download_failure.string()
            )
            return CmdExecuteResult.failure(
                com.hive.i8n.R.string.sc_download_failure.string()
            )
        }
    }


    override fun getCommandName() = getString(com.hive.i8n.R.string.cmd_download_name)

    override fun getCommandDescribe() = getString(com.hive.i8n.R.string.cmd_download_des)

    override fun getCommandIcon() = R.drawable.sc_icon_download_cmd

    override fun getCommand(): String {
        return "${cmdPrefix()} url=\"${url.encode()}\" path=\"${savePath.encode()}\" output=$saveParamId gallery=$saveToGallery"
    }

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        p["url"]?.decode()?.let { url = it }
        savePath = p["path"]?.decode() ?: savePath
        if (savePath.isEmpty()) savePath = ScriptConst.getDownloadPath()
        saveParamId = p["output"] ?: saveParamId
        saveToGallery = p["gallery"]?.toBooleanStrictOrNull() ?: false
    }

    override fun getPermissionRequest(): List<String> {
        // 无权限时 getDownloadPath 会返回应用私有目录，无需权限
        return emptyList()
    }

    companion object {

        fun createCommand(): CmdDownload {
            val cmd = CmdDownload()
            cmd.savePath = ScriptConst.getDownloadPath()
            cmd.saveParamId = ScriptParamEnv.getDefaultParam()?.getFullId()
            return cmd
        }

    }
}