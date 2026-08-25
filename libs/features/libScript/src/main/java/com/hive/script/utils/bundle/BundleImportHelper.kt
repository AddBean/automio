// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils.bundle

import android.content.Context
import com.hive.script.ScriptProvider
import com.hive.script.views.dialog.DialogBundleImportConflictConfirm
import com.hive.script.event.RefreshScriptListEvent
import com.hive.utils.GlobalApp
import com.hive.views.widgets.CommonToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

/**
 * 统一处理 bundle 导入结果：冲突弹框、成功/失败 Toast、刷新列表。
 * 供本地文件导入流程复用。
 */
object BundleImportHelper {

    /**
     * 处理 tryInstall 返回结果。
     * @param bundleResult tryInstall 返回值，null 表示非 bundle
     * @param context 用于弹框，null 时使用 ScriptProvider.getViewContext()
     * @param onSuccess 导入成功时回调（主线程）
     * @return true 表示已处理（bundleResult 非 null），false 表示非 bundle
     */
    fun handleBundleResult(
        bundleResult: WorkflowBundleInstaller.InstallResult?,
        context: Context? = null,
        onSuccess: ((WorkflowBundleInstaller.InstallResult) -> Unit)? = null
    ): Boolean {
        if (bundleResult == null) {
            CommonToast.show(com.hive.i8n.R.string.sc_bundle_import_not_bundle)
            return false
        }

        val ctx = context ?: ScriptProvider.getViewContext()

        val pending = bundleResult.pendingInstall
        if (pending != null && bundleResult.pendingConflicts.isNotEmpty()) {
            DialogBundleImportConflictConfirm(ctx)
                .setPending(pending, bundleResult.pendingConflicts)
                .setOnConfirmListener(object : DialogBundleImportConflictConfirm.OnConfirmListener {
                    override fun onConfirm(
                        p: WorkflowBundleInstaller.PendingInstall,
                        actions: Map<String, WorkflowBundleInstaller.ConflictAction>
                    ) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val resolved = WorkflowBundleInstaller.continueInstall(p, actions)
                            withContext(Dispatchers.Main) {
                                handleResolvedResult(resolved, onSuccess)
                            }
                        }
                    }

                    override fun onCancel(p: WorkflowBundleInstaller.PendingInstall) {
                        WorkflowBundleInstaller.cancelPending(p)
                    }
                })
                .show()
            return true
        }

        handleResolvedResult(bundleResult, onSuccess)
        return true
    }

    private fun handleResolvedResult(
        result: WorkflowBundleInstaller.InstallResult,
        onSuccess: ((WorkflowBundleInstaller.InstallResult) -> Unit)?
    ) {
        if (result.success) {
            CommonToast.show(com.hive.i8n.R.string.sc_bundle_import_success)
            if (result.missingTools.isNotEmpty()) {
                val missing = result.missingTools.joinToString(", ").take(200)
                CommonToast.show(
                    GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_missing_tools, missing)
                )
            }
            EventBus.getDefault().post(RefreshScriptListEvent())
            onSuccess?.invoke(result)
        } else {
            CommonToast.show(
                result.errorMessage ?: GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_failed)
            )
        }
    }
}
