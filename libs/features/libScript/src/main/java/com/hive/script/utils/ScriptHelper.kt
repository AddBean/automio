// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import androidx.core.content.FileProvider
import com.blankj.utilcode.util.ThreadUtils
import com.hive.config.BuildConfigHelper
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptMate
import com.hive.script.base.ScriptKeyStoreManager
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.cmd.CmdCallScript
import com.hive.script.cmd.CmdJumpPoint
import com.hive.script.scope.LocalResourceListRepository
import com.hive.script.scope.ScriptScopeRepository
import com.hive.script.event.RefreshScriptListInitEvent
import com.hive.script.extensions.decrypt
import com.hive.script.extensions.encrypt
import com.hive.script.extensions.forEachAllCommand
import com.hive.script.utils.bundle.WorkflowBundleManifest
import com.hive.utils.utils.GsonHelper
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.script.views.dialog.DialogPermissionAggregate
import com.hive.script.views.dialog.DialogScriptLoading
import com.hive.utils.GlobalApp
import com.hive.utils.extends.string
import com.hive.utils.file.FileUtils
import com.hive.utils.thread.UIHandlerUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 * @author jiadou
 * @date 7/9/21
 */
object ScriptHelper {

    @OptIn(DelicateCoroutinesApi::class)
    fun blockUntilViewReady(view: View, runOnMain: (() -> Unit)? = null) {
        val executed = AtomicBoolean(false)
        val ready = AtomicBoolean(false)
        var maxCount = 100
        val safeRun = {
            if (executed.compareAndSet(false, true)) {
                runOnMain?.invoke()
            }
        }
        view.post {
            safeRun()
            ready.set(true)
        }
        while (!ready.get() && maxCount > 0) {
            try {
                if (ThreadUtils.isMainThread()) return
                ScriptThreadManager.delay(5)
            } catch (e: InterruptedException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
            maxCount--
        }
        GlobalScope.launch(Dispatchers.Main) {
            safeRun()
        }
    }

    fun runInMain(run: () -> Unit) {
        UIHandlerUtils.getInstance().executeInMainThread {
            run.invoke()
        }
    }

    fun runInMain(run: () -> Unit, delay: Long) {
        UIHandlerUtils.getInstance().executeInMainThread({
            run.invoke()
        }, delay)
    }

    fun runInMain(run: Runnable, delay: Long) {
        UIHandlerUtils.getInstance().executeInMainThread({
            run.run()
        }, delay)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun runInIO(run: (dialog: DialogScriptLoading?) -> Unit) {
        val dialogLoading = DialogScriptLoading(ScriptProvider.getViewContext())
        GlobalScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                dialogLoading.show()
            }
            try {
                run.invoke(dialogLoading)
            } finally {
                withContext(Dispatchers.Main) {
                    dialogLoading.dismiss()
                }
            }
        }
    }

    /**
     * 复制脚本
     */
    fun copyScript(scrPath: String) {
        FileUtils.makeDirs(ScriptConst.Save_Import_Temp_Path)
        FileUtils.clearDirectory(File(ScriptConst.Save_Import_Temp_Path), false)
        val scrFile = File(scrPath)
        val targetName = getCopyName(scrFile)
        FileUtils.copyAllFolder(
            scrPath, ScriptConst.Save_Import_Temp_Path, null
        )
        val tempFile = File(ScriptConst.Save_Import_Temp_Path + "/" + scrFile.name)
        val desFile = File(ScriptConst.Save_Import_Temp_Path + "/" + targetName)
        tempFile.renameTo(desFile)
        regenerateScriptUidForCopy(originalScriptPath = scrPath, copiedScriptDir = desFile)
        FileUtils.copyAllFolder(
            desFile.path, ScriptConst.Save_Script_Path, null
        )
    }

    private fun regenerateScriptUidForCopy(originalScriptPath: String, copiedScriptDir: File) {
        try {
            val model = ScriptInfoModel().parseMainFile(copiedScriptDir)
            val mate = model.scriptMate ?: return

            val newUid = ScriptMate.generateScriptUid()
            mate.scriptUid = newUid
            model.scriptMate = mate
            // 写入 info（永远可读），保证跨导入/分享稳定
            model.saveMate()

            // 若主文件为明文 main.jds，则同步写回首行 mate
            val plainMain = File(copiedScriptDir, ScriptConst.SCRIPT_MAIN_FILE_NAME)
            if (plainMain.exists()) {
                val lines = plainMain.readLines().toMutableList()
                if (lines.isNotEmpty() && lines[0].startsWith("mate")) {
                    lines[0] = mate.getCommandLines()
                    plainMain.writeText(lines.joinToString("\n"))
                }
            }

            // 若主文件为加密 main.jds.encrypt，且能拿到原脚本的 key，则同步更新加密内容
            val encryptMain = File(copiedScriptDir, ScriptConst.SCRIPT_MAIN_ENCRYPT_FILE_NAME)
            if (encryptMain.exists()) {
                val key = ScriptKeyStoreManager.findKey(originalScriptPath)
                if (!key.isNullOrBlank()) {
                    val decoded = encryptMain.readText().decrypt(key)
                    val lines = decoded.split("\n").toMutableList()
                    if (lines.isNotEmpty() && lines[0].startsWith("mate")) {
                        lines[0] = mate.getCommandLines()
                        val reEncoded = lines.joinToString("\n").encrypt(key = key)
                        encryptMain.writeText(reEncoded)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun copyScriptToShare(scrPath: String): String {
        FileUtils.makeDirs(ScriptConst.Save_Share_Temp_Path)
        FileUtils.clearDirectory(File(ScriptConst.Save_Share_Temp_Path), false)
        FileUtils.copyAllFolder(
            scrPath, ScriptConst.Save_Share_Temp_Path, null
        )
        val scriptName = FileUtils.getLastFoldName(scrPath)
        return ScriptConst.Save_Share_Temp_Path + scriptName + "/"
    }

    private fun getCopyName(scrFile: File): String {
        var targetName = scrFile.name
        val tempName = targetName
        var index = 1
        while (File(ScriptConst.Save_Script_Path + targetName).exists()) {
            targetName = "${tempName}_copy$index"
            index++
        }
        return targetName
    }


    const val PERMISSION_CAPTURE = "android.permission.CAPTURE"
    const val PERMISSION_CAMERA = "android.permission.CAMERA"
    const val PERMISSION_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE"

    const val PERMISSION_BIND_ACCESSIBILITY_SERVICE =
        "android.permission.BIND_ACCESSIBILITY_SERVICE"

    const val PERMISSION_UNLOCK_SERVICE = "android.permission.UNLOCK_SERVICE"

    const val PERMISSION_NOTIFICATION_LISTENER =
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"

    const val PERMISSION_LOCATION = "android.permission.ACCESS_FINE_LOCATION"

    const val PERMISSION_NETWORK = "android.permission.ACCESS_NETWORK_STATE"

    const val PERMISSION_DOWNLOAD = "android.permission.ACCESS_DOWNLOAD_MANAGER"

    const val PERMISSION_RECORD_AUDIO = "android.permission.RECORD_AUDIO"

    val mPermissionMap = mutableMapOf<String, String>().apply {
        put(PERMISSION_CAPTURE, GlobalApp.getString(com.hive.i8n.R.string.sc_permission_capture))
        put(PERMISSION_CAMERA, GlobalApp.getString(com.hive.i8n.R.string.sc_permission_camera))
        put(PERMISSION_STORAGE, GlobalApp.getString(com.hive.i8n.R.string.sc_permission_storage))
        put(
            PERMISSION_BIND_ACCESSIBILITY_SERVICE,
            GlobalApp.getString(com.hive.i8n.R.string.sc_permission_accessibility_service)
        )
        put(
            PERMISSION_UNLOCK_SERVICE,
            GlobalApp.getString(com.hive.i8n.R.string.sc_permission_unlock_service)
        )
        put(
            PERMISSION_NOTIFICATION_LISTENER,
            GlobalApp.getString(com.hive.i8n.R.string.sc_permission_bind_notification_listener)
        )
        put(
            PERMISSION_LOCATION, GlobalApp.getString(com.hive.i8n.R.string.sc_permission_location)
        )
        put(
            PERMISSION_NETWORK, GlobalApp.getString(com.hive.i8n.R.string.sc_permission_network)
        )
        put(
            PERMISSION_DOWNLOAD, GlobalApp.getString(com.hive.i8n.R.string.sc_permission_download)
        )
        put(
            PERMISSION_RECORD_AUDIO, GlobalApp.getString(com.hive.i8n.R.string.sc_permission_record_audio)
        )
    }

    /** 递归收集依赖权限时的最大深度，防止循环依赖或过深调用栈 */
    private const val MAX_PERMISSION_RECURSION_DEPTH = 50

    /**
     * 请求权限并开始运行：若有缺失权限则弹出权限聚合弹窗，否则执行权限回调。
     * 权限来源：mate.permission + 当前脚本命令树 + 依赖脚本命令树（递归），动态合并去重。
     */
    fun checkPermissionAndRights(cmdRoot: ScriptCommandRoot, callback: () -> Unit) {
        val permissionCallback = callback
        val permissions = getRequiredPermissionsMerged(cmdRoot)
        if (permissions.isEmpty()) {
            permissionCallback.invoke()
            return
        }
        val missed = ScriptPermissionManager.checkMissedPermissions(permissions)
        if (missed.isEmpty()) {
            permissionCallback.invoke()
            return
        }
        DialogPermissionAggregate(
            ScriptProvider.getViewContext(),
            missed
        ).show()
    }

    /** bundle 安装时 manifest 保存路径（相对于脚本目录） */
    private const val BUNDLE_MANIFEST_REL_PATH = WorkflowBundleManifest.DEFAULT_MANIFEST_NAME

    /**
     * 动态合并权限：manifest（若有）+ mate（非 bundle 时兜底）+ 当前脚本及依赖命令树（递归），去重后返回。
     */
    fun getRequiredPermissionsMerged(cmdRoot: ScriptCommandRoot): List<String> {
        val scriptDir = cmdRoot.scriptPath?.takeIf { it.isNotBlank() }?.let { File(it) }
        val fromManifest = loadManifestPermissionsFromScriptDir(scriptDir)
        val fromMate = cmdRoot.scriptMate?.permission
            ?.filter { it in mPermissionMap.keys }
            ?.distinct()
            ?: emptyList()
        val fromRecursive = getRequiredPermissionsRecursive(
            root = cmdRoot,
            baseScriptDir = scriptDir,
            visited = mutableSetOf(),
            depth = 0
        )
        // 优先用 manifest（bundle 导入），否则用 mate（本地/非 bundle）
        val fromDeclared = if (fromManifest.isNotEmpty()) fromManifest else fromMate
        return (fromDeclared + fromRecursive)
            .distinct()
            .filter { it in mPermissionMap.keys }
    }

    /**
     * 从脚本目录读取 bundle manifest 的权限列表（若存在）。
     */
    private fun loadManifestPermissionsFromScriptDir(scriptDir: File?): List<String> {
        if (scriptDir == null || !scriptDir.exists() || !scriptDir.isDirectory) return emptyList()
        val manifestFile = File(scriptDir, BUNDLE_MANIFEST_REL_PATH)
        if (!manifestFile.exists() || !manifestFile.isFile) return emptyList()
        return runCatching {
            val manifest = GsonHelper.getInstance()
                .fromJson(manifestFile.readText(), WorkflowBundleManifest::class.java)
            manifest.permissions
                ?.filter { it in mPermissionMap.keys }
                ?.distinct()
                ?: emptyList()
        }.getOrElse { emptyList() }
    }

    /**
     * 递归收集脚本及其依赖的权限（从命令树解析），避免循环依赖。
     */
    private fun getRequiredPermissionsRecursive(
        root: ScriptCommand,
        baseScriptDir: File?,
        visited: MutableSet<String>,
        depth: Int
    ): Set<String> {
        if (depth >= MAX_PERMISSION_RECURSION_DEPTH) return emptySet()
        val collected = mutableSetOf<String>()
        root.forEachAllCommand { cmd ->
            cmd.getPermissionRequest()?.forEach { p ->
                if (p in mPermissionMap.keys) collected.add(p)
            }
            cmd.conditionList?.forEach {
                it.getPermissionRequest()?.forEach { p ->
                    if (p in mPermissionMap.keys) collected.add(p)
                }
            }
            if (cmd is CmdCallScript) {
                val depPath = resolveCallScriptPathForPermission(cmd, baseScriptDir)
                if (!depPath.isNullOrBlank()) {
                    val canonical = runCatching { File(depPath).canonicalPath }.getOrNull()
                    if (canonical != null && visited.add(canonical)) {
                        runCatching {
                            val depRoot = ScriptCommandRoot()
                            ScriptCommandRoot.loadScriptSync(depPath, depRoot)
                            collected.addAll(
                                getRequiredPermissionsRecursive(
                                    root = depRoot,
                                    baseScriptDir = File(depPath).parentFile,
                                    visited = visited,
                                    depth = depth + 1
                                )
                            )
                        }.onFailure {
                            // 加密、损坏等无法加载的依赖，跳过
                        }
                        visited.remove(canonical)
                    }
                }
            }
        }
        return collected
    }

    /**
     * 解析 CmdCallScript 的 scriptPath 为绝对路径，用于权限收集。
     */
    private fun resolveCallScriptPathForPermission(cmd: CmdCallScript, baseScriptDir: File?): String? {
        val raw = cmd.scriptPath?.takeIf { it.isNotBlank() } ?: return null
        if (baseScriptDir == null || !baseScriptDir.exists() || !baseScriptDir.isDirectory) {
            return if (File(raw).exists()) raw else null
        }
        return when {
            ScriptScopeRepository.isScopedScriptRef(raw) ->
                ScriptScopeRepository.resolveScopedScriptPath(
                    currentScriptDir = baseScriptDir,
                    ref = raw
                )
            File(raw).exists() -> raw
            else ->
                ScriptScopeRepository.resolveScopedScriptPath(
                    currentScriptDir = baseScriptDir,
                    ref = raw
                ) ?: raw.takeIf { File(raw).exists() }
        }
    }

    /**
     * 获取脚本必备的权限列表，根据命令种类判定
     */
    fun getRequiredPermissions(script: ScriptCommand): List<Pair<String, String>> {
        val map = mutableMapOf<String, String?>()
        script.forEachAllCommand { it ->
            it.getPermissionRequest()?.forEach {
                map[it] = mPermissionMap[it]
            }
            it.conditionList?.forEach {
                it.getPermissionRequest()?.forEach {
                    map[it] = mPermissionMap[it]
                }
            }
        }
        return map.filter { it.value != null }
            .map { Pair(it.key, it.value) } as List<Pair<String, String>>
    }


    fun listAllScripts(filterEncrypt: Boolean = false): List<ScriptInfoModel>? {
        return LocalResourceListRepository.listWorkflows()
            .map { dir ->
                val mainFile = File(dir, ScriptConst.SCRIPT_MAIN_FILE_NAME)
                val infoFile = File(dir, ScriptConst.SCRIPT_MAIN_INFO_FILE_NAME)
                val path = when {
                    infoFile.exists() -> infoFile.absolutePath
                    mainFile.exists() -> mainFile.absolutePath
                    else -> dir.absolutePath
                }
                if (path.endsWith(ScriptConst.SCRIPT_SUFFIX_INFO)) {
                    getScriptInfoModelByPath(path)
                } else {
                    getScriptMainModelByPath(path)
                }
            }
            .filter {
                if (filterEncrypt) {
                    it.scriptMate?.isEncrypt() == false
                } else {
                    true
                }
            }
            .sortedByDescending { it.scriptMate?.updateTime }
    }

    fun copyToTempDir(path: String): String {
        val newRelativePath = ScriptConst.newMd5RelativePath(path)
        val desPath = ScriptConst.Save_Script_Temp_Path + newRelativePath
        if (FileUtils.isFileExist(desPath)) return newRelativePath;
        FileUtils.makeDirs(File(desPath).parent!! + "/")
        FileUtils.copyFile(
            path, desPath
        )
        return newRelativePath
    }

    fun getJumpPoints(rootCmd: ScriptCommand): List<CmdJumpPoint> {
        val list = mutableListOf<CmdJumpPoint>()
        rootCmd.getRootScript()?.forEachAllCommand {
            if (it is CmdJumpPoint) {
                list.add(it)
            }
        }
        return list
    }

    fun getScriptInfoModelByPath(scriptPath: String): ScriptInfoModel {
        return ScriptInfoModel().parseInfoFile(File(scriptPath))
    }


    fun getScriptMainModelByPath(it: String): ScriptInfoModel {
        return ScriptInfoModel().parseMainFile(File(it))
    }

    /**
     * 保存到相册
     */
    fun saveToGallery(path: String) {
        try {
            MediaStore.Images.Media.insertImage(
                GlobalApp.getContext().contentResolver,
                path,
                GlobalApp.getString(com.hive.i8n.R.string.app_name),
                GlobalApp.getString(com.hive.i8n.R.string.app_name)
            )
            GlobalApp.getContext().sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, getUriFromPath(path)
                )
            ) //path是导出的文件路径
            MediaScannerConnection.scanFile(GlobalApp.getContext(), arrayOf(path), null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getUriFromPath(path: String): Uri? {
        val file = File(path)
        if (!file.exists()) return null
        val uri: Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //如果SDK版本>=24，即：Build.VERSION.SDK_INT >= 24
                FileProvider.getUriForFile(
                    GlobalApp.sContext,
                    BuildConfigHelper.getMapString("appId") + ".fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }
        return uri
    }

    /**
     * 清理智能体任务记录，只保留最新的3个
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun cleanAgentTask(resCount: Int) {
        val tag = com.hive.i8n.R.string.ai_agent_task_tag.string()
        GlobalScope.launch(Dispatchers.IO) {
            val list = listAllScripts()
                ?.filter { it.scriptMate?.tag == tag }
                ?.sortedByDescending { it.scriptMate?.updateTime }
            if (list != null && list.size > resCount) {
                for (i in resCount until list.size) {
                    list[i].delete()
                }
                EventBus.getDefault().post(RefreshScriptListInitEvent())
            }
        }
    }

    fun checkScriptPath(path: String?) {
        if (TextUtils.isEmpty(path)) {
            throw Exception("Script should not been null")
        }
        val file = File(path)
        if (!file.exists()) {
            throw Exception("Script path is not exist!!")
        }
        if (!file.isDirectory) {
            throw Exception("Script should been a directory")
        }
    }
}
