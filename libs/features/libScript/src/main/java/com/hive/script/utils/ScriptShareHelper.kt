// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.content.Context
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptSaver
import com.hive.script.scope.ExportScanResult
import com.hive.script.scope.ScriptScopeBundleV2Exporter
import com.hive.script.views.dialog.DialogExportDependencyConfirm
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.utils.GlobalApp
import com.hive.utils.ShareUtils
import com.hive.utils.encrypt.Md5Utils
import com.hive.utils.extends.toast
import com.hive.utils.file.FileUtils
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.DialogLoading
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

object ScriptShareHelper {

    /**
     * 分享工作流，走依赖选择弹框后导出 bundle 再分享。
     * @param context 用于展示依赖选择弹框，非空时走弹框流程
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun startShare(context: Context?, info: ScriptInfoModel, pwd: String?, ctrValue: String?, expireTime: Long) {
        GlobalScope.launch(Dispatchers.IO) {
            changeToEncrypt(info, pwd, ctrValue, expireTime) { data ->
                val targetFold = data.scriptPath
                FileUtils.makeDirs(ScriptConst.Save_Share_Path)
                val srcFile = File(targetFold!!)
                val desFile = File(
                    "${ScriptConst.Save_Share_Path}${File(targetFold).name}${ScriptConst.Script_File_Suffix}"
                )

                if (context == null) {
                    ScriptHelper.runInMain {
                        CommonToast.getInstance().showToastLong(
                            GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_export_failed)
                        )
                    }
                    return@changeToEncrypt
                }

                ScriptHelper.runInMain {
                    val loading = DialogLoading(context).apply { show() }
                    exportWorkflowBundleZipWithDialog(
                        workflow = data,
                        workflowDir = srcFile,
                        outputZip = desFile,
                        context = context,
                        onScanComplete = { loading.dismiss() },
                        onSuccess = {
                            if (desFile.exists()) {
                                share(data, desFile)
                            } else {
                                CommonToast.getInstance().showToast(com.hive.i8n.R.string.sc_export_error_msg)
                            }
                        },
                        onCancel = { },
                        onError = { }
                    )
                }
            }
        }
    }

    /**
     * 导出 bundle.zip（带依赖选择弹框）：
     * 先扫描依赖，弹框展示 skill/tool 列表，用户确认后按选择导出。
     * @param onScanComplete 扫描完成、弹框即将展示时回调（主线程），可用于关闭 loading
     */
    fun exportWorkflowBundleZipWithDialog(
        workflow: ScriptInfoModel,
        workflowDir: File,
        outputZip: File,
        context: Context,
        onSuccess: () -> Unit,
        onCancel: () -> Unit,
        onError: (Throwable) -> Unit,
        onScanComplete: (() -> Unit)? = null
    ) {
        ScriptHelper.runInIO {
            val result: ExportScanResult = try {
                ScriptScopeBundleV2Exporter.scanDependencies(workflowDir)
            } catch (e: Throwable) {
                com.hive.i8n.R.string.sc_export_error_msg.toast(e.message)
                ScriptHelper.runInMain {
                    onError(e)
                }
                return@runInIO
            }

            if (result.errors.isNotEmpty()) {
                ScriptHelper.runInMain {
                    CommonToast.getInstance().showToastLong(result.errors.joinToString("; "))
                    onError(IllegalStateException(result.errors.joinToString("; ")))
                }
                return@runInIO
            }

            ScriptHelper.runInMain {
                onScanComplete?.invoke()
                DialogExportDependencyConfirm(context)
                    .setScanResult(result)
                    .setOnConfirmListener(object : DialogExportDependencyConfirm.OnConfirmListener {
                        override fun onConfirm(
                            selectedSkillIds: Set<String>,
                            selectedToolPaths: Set<String>,
                            selectedPermissions: Set<String>
                        ) {
                            ScriptHelper.runInIO {
                                try {
                                    ScriptScopeBundleV2Exporter.exportWithSelection(
                                        workflow = workflow,
                                        workflowDir = workflowDir,
                                        outputZip = outputZip,
                                        scanResult = result,
                                        selectedSkillIds = selectedSkillIds,
                                        selectedToolPaths = selectedToolPaths,
                                        selectedPermissions = selectedPermissions
                                    )
                                    ScriptHelper.runInMain { onSuccess() }
                                } catch (e: Throwable) {
                                    ScriptHelper.runInMain {
                                        CommonToast.getInstance().showToastLong(
                                            e.message ?: GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_export_failed)
                                        )
                                        onError(e)
                                    }
                                }
                            }
                        }

                        override fun onCancel() {
                            onCancel()
                        }
                    })
                    .show()
            }
        }
    }

    /**
     * 加密脚本
     */
    private suspend fun changeToEncrypt(
        info: ScriptInfoModel,
        pwd: String?,
        ctrValue: String?,
        expireTime: Long,
        callback: ((info: ScriptInfoModel) -> Unit)? = null
    ) {
        val data = info.copy()
        if (!pwd.isNullOrEmpty()) {
            val scriptPath = data.scriptPath!!
            val scripSharePath = ScriptHelper.copyScriptToShare(scriptPath)
            val cmdRoot = ScriptCommandRoot()
            ScriptCommandRoot.loadScript(
                scripSharePath,
                cmdRoot
            )
            cmdRoot.scriptMate?.control = ctrValue
            cmdRoot.scriptMate?.encrypt = 1
            cmdRoot.scriptMate?.expireTime = expireTime
            cmdRoot.scriptMate?.passwordMd5 = Md5Utils.string2md5(pwd)
            cmdRoot.scriptPath = scripSharePath
            data.scriptMate = cmdRoot.scriptMate
            data.scriptPath = scripSharePath
            ScriptSaver.saveToLocalNoLoading(data.scriptName!!, cmdRoot, pwd, null) {
                callback?.invoke(data)
            }
            FileUtils.deleteFile(File(scripSharePath + ScriptConst.SCRIPT_MAIN_FILE_NAME))
        } else {
            callback?.invoke(data)
        }

    }

    private fun share(info: ScriptInfoModel?, file: File) {
        ShareUtils.getInstance(GlobalApp.getContext()).shareFileToSystem(file)
    }
}
