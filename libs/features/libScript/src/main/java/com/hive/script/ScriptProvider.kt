// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import com.hive.annotation.NotProguard
import com.hive.files.model.FileCardData
import com.hive.files.utils.XAppInfoParser
import com.hive.net.BaseResult
import com.hive.net.OnHttpListener
import com.hive.net.RxTransformer
import com.hive.plugin.ComponentConst
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.ExecutionContextFrame
import com.hive.plugin.agent.ExecutionContextType
import com.hive.plugin.agent.ExecutionContexts
import com.hive.plugin.agent.IAgentStateObserver
import com.hive.plugin.agent.IExecutionContextObserver
import com.hive.plugin.agent.ISkillStateObserver
import com.hive.plugin.agent.model.AgentInput
import com.hive.plugin.agent.model.AgentTaskGoal
import com.hive.plugin.agent.model.ExecutionStatus
import com.hive.plugin.agent.model.SkillResult
import com.hive.plugin.agent.model.TaskPriority
import com.hive.plugin.agent.model.TaskResult
import com.hive.plugin.provider.IAgentProvider
import com.hive.plugin.provider.IMcpProvider
import com.hive.plugin.provider.IOcrProvider
import com.hive.plugin.provider.IScriptProvider
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptKeyStoreManager
import com.hive.script.base.ScriptMate
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.core.ScriptParser
import com.hive.script.base.core.ScriptSaver
import com.hive.script.cmd.autoRegisterAllCommands
import com.hive.script.condition.autoRegisterAllConditions
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.driver.ServiceAccessibility
import com.hive.script.event.RefreshScriptListEvent
import com.hive.script.inputmethod.ScriptInputMethodHelper
import com.hive.script.mcp.ScriptMcpRegister
import com.hive.script.mcp.tools.autoRegisterAllMcpTools
import com.hive.script.setting.ScriptSettingActivity
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.agent.ScriptAgentTopView
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.script.views.dialog.DialogCmdDialogInput
import com.hive.script.views.dialog.DialogCmdDialogInput.InputItem
import com.hive.script.views.dialog.DialogScriptAlert
import com.hive.script.views.dialog.DialogScriptListSelector
import com.hive.script.views.dialog.DialogTimerTips
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptMenuManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.tips.BaseScriptTipsHelper
import com.hive.timer.AlarmTaskEntity
import com.hive.timer.TimerProvider
import com.hive.timer.event.OnTimeAlarmEvent
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.utils.extends.string
import com.hive.utils.utils.GsonHelper
import com.hive.utils.utils.IntentUtils
import com.hive.views.DialogAlertHelper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.reactivestreams.Subscriber

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/11/21
 */
@NotProguard
class ScriptProvider : IScriptProvider {
    private val context: Context = GlobalApp.getContext()

    /** 最近一次 Agent 任务录制到的命令列表（含 for 块已展平，供 Session 保存与工作流保存使用） */
    @Volatile
    private var lastRecordedCommands: List<String>? = null

    @Volatile
    private var isExecutionContextObserverRegistered: Boolean = false

    @Volatile
    private var isAgentStateObserverRegistered: Boolean = false

    @Volatile
    private var isSkillStateObserverRegistered: Boolean = false

    @Volatile
    private var lastAgentSkillDepth: Int = 0

    @Volatile
    private var lastAgentTaskResultForTip: TaskResult? = null

    @Volatile
    private var lastAgentGoalForTip: AgentTaskGoal? = null

    @Volatile
    private var agentProviderRef: IAgentProvider? = null
    private val executionContextObserver = object : IExecutionContextObserver {
        override fun onExecutionContextStackChanged(snapshot: List<ExecutionContextFrame>) {
            ScriptHelper.runInMain {
                handleExecutionContextSnapshot(snapshot)
            }
        }
    }

    private fun handleExecutionContextSnapshot(snapshot: List<ExecutionContextFrame>) {
        val topType: ExecutionContextType? = snapshot.lastOrNull()?.type
        if (topType == ExecutionContextType.AGENT || topType == ExecutionContextType.SKILL) {
            ScriptAgentTopView.show()
        } else {
            ScriptAgentTopView.dismiss()
        }

        val agentSkillDepth =
            snapshot.count { it.type == ExecutionContextType.AGENT || it.type == ExecutionContextType.SKILL }
        val prevDepth = lastAgentSkillDepth
        lastAgentSkillDepth = agentSkillDepth

        if (prevDepth == 0 && agentSkillDepth > 0) {
            onAgentSkillSessionStart(topType)
        } else if (prevDepth > 0 && agentSkillDepth == 0) {
            onAgentSkillSessionEnd()
        }
    }

    private fun onAgentSkillSessionStart(topType: ExecutionContextType?) {
        ScriptMcpRegister.startRecord()
        ScriptMenuManager.hiddenMenuView()
        if (topType == ExecutionContextType.AGENT) {
            // Agent 启动时需要抢占执行权，避免脚本仍在跑
            ScriptInterpreter.getDefault().stopExecute()
        }
        ScriptRecordManager.updateRecordView(
            ScriptRecordViewManager.ViewState.default()
                .ofFalse(ScriptRecordViewManager.RecordViewType.MENU)
                .ofTrue(ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW)
        )
        ScriptManager.updateViewLayout()
    }

    private fun onAgentSkillSessionEnd() {
        ScriptRecordManager.updateRecordView(ScriptRecordViewManager.ViewState.default())
        ScriptMenuManager.showMenuView()

        val raw = ScriptMcpRegister.endRecord()
        val flattened = raw.flatMap { it.split("\n").filterNot { it.isBlank() } }
        lastRecordedCommands = flattened.takeIf { it.isNotEmpty() }

        val taskResult = lastAgentTaskResultForTip
        val goal = lastAgentGoalForTip
        lastAgentTaskResultForTip = null
        lastAgentGoalForTip = null
        if (taskResult != null) {
            BaseScriptTipsHelper.showAgentFinishTip(goal, taskResult)
        }
    }


    override fun init(context: Context) {
        TimerProvider.getInstance().init(context)
        ScriptKeyStoreManager.init()
        autoRegisterAllCommands()
        autoRegisterAllConditions()
        ScriptParser.initCommandMap()
        ScriptParser.initConditionMap()
        System.loadLibrary("opencv_java4")
        EventBus.getDefault().register(this)
        DialogAlertHelper.registerDialog(DialogTimerTips::class.java)
        ComponentManager.getInstance().register(ComponentConst.OCR_PROVIDER)
    }

    override fun initAgentService(agentProvider: IAgentProvider?) {
        agentProviderRef = agentProvider
        if (!isExecutionContextObserverRegistered) {
            isExecutionContextObserverRegistered = true
            ExecutionContexts.stack.registerObserver(executionContextObserver)
        }
        if (!isAgentStateObserverRegistered) {
            isAgentStateObserverRegistered = true
            agentProvider?.registerAgentStateObserver(object : IAgentStateObserver {
                override fun onAgentExecuteStart(taskId: String) {
                    lastAgentTaskResultForTip = null
                    lastAgentGoalForTip = null
                }

                override fun onAgentExecuteEnd(taskId: String, taskResult: TaskResult?) {
                    lastAgentTaskResultForTip = taskResult
                    lastAgentGoalForTip = agentProvider.currentAgentGoal
                }

                override fun onAgentStateChanged(taskId: String, status: ExecutionStatus) {

                }
            })
        }

        if (!isSkillStateObserverRegistered) {
            isSkillStateObserverRegistered = true
            agentProvider?.registerSkillStateObserver(object : ISkillStateObserver {
                override fun onSkillExecuteStart(taskId: String) {
                }

                override fun onSkillExecuteEnd(taskId: String, result: SkillResult?) {
                }
            })
        }
    }

    override fun isAccessServiceReady(): Boolean {
        return ScriptEventHelper.get().getAccessService() != null
    }

    override fun getAccessibilityService(): android.accessibilityservice.AccessibilityService? {
        return ScriptEventHelper.get().getAccessService()
    }

    override fun getLastRecordedCommands(): List<String>? = lastRecordedCommands

    override fun saveCommandsToWorkflow(
        commands: List<String>,
        namePrefix: String?,
        callback: IScriptProvider.OnSaveWorkflowCallback?
    ) {
        if (commands.isEmpty()) {
            callback?.onResult(false)
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            val cmdRoot = ScriptCommandRoot()
            val tag = com.hive.i8n.R.string.ai_agent_task_tag.string()
            ScriptCommandRoot.loadCommandList(commands, cmdRoot)
            cmdRoot.scriptMate?.tag = tag
            val rootScript = cmdRoot.getRootScript() ?: run {
                ScriptHelper.runInMain { callback?.onResult(false) }
                return@launch
            }
            if (rootScript.commandQueue.isNullOrEmpty()) {
                ScriptHelper.runInMain { callback?.onResult(false) }
                return@launch
            }
            val prefix = namePrefix?.take(200)?.trim()?.ifEmpty { null } ?: tag
            val saveName = ScriptManager.generateNewSaveName(prefix)
            ScriptSaver.saveToLocalNoLoading(saveName, cmdRoot, null, null) {
                EventBus.getDefault().post(RefreshScriptListEvent())
                ScriptHelper.runInMain { callback?.onResult(true) }
            }
        }
    }

    override fun registerToLocalAgent() {
        autoRegisterAllMcpTools()
        ScriptMcpRegister.registerAll()
        // 启动期统一重建已安装 workflow/custom tool 的运行时注册结果。
        runCatching { com.hive.script.scope.GlobalScriptRegistry.registerAllInstalled() }
    }

    override fun unregisterCustomTool(toolName: String?) {
        ScriptMcpRegister.unregisterCustomTool(toolName ?: return)
    }

    override fun cleanupOrphanSkillsAndTools(): com.hive.plugin.provider.OrphanCleanupResult {
        val r = com.hive.script.scope.GlobalScriptRegistry.cleanupOrphans()
        return com.hive.plugin.provider.OrphanCleanupResult(
            r.skillsRemoved,
            r.toolsRemoved,
            r.customToolsRemovedFromSp + r.customSkillsRemovedFromSp
        )
    }

    override fun startRegisterCustomTools(
        scriptPath: String,
        listener: IScriptProvider.OnToolsRegisterToolsListener?
    ) {
        ScriptHelper.checkScriptPath(scriptPath)
        GlobalScope.launch(Dispatchers.Main) {
            val model: ScriptInfoModel = ScriptHelper.getScriptMainModelByPath(scriptPath)
            val input1 = InputItem(
                id = "toolName",
                context.getString(com.hive.i8n.R.string.script_provider_tool_name),
                context.getString(com.hive.i8n.R.string.script_provider_tool_name),
                true,
                android.text.InputType.TYPE_CLASS_TEXT,
                model.scriptName ?: ""
            )
            val input2 = InputItem(
                id = "toolDes",
                context.getString(com.hive.i8n.R.string.script_provider_tool_description),
                context.getString(com.hive.i8n.R.string.script_provider_tool_description),
                true,
                android.text.InputType.TYPE_CLASS_TEXT,
                ""
            )
            val inputItems = mutableListOf(input1, input2)
            val dialog = DialogCmdDialogInput(getViewContext())
            dialog.setTitle(context.getString(com.hive.i8n.R.string.script_provider_add_tool))
            dialog.setInputItems(inputItems)
            dialog.setInputListener(object : DialogCmdDialogInput.OnInputListener {
                override fun onConfirmed(
                    dialog: DialogCmdDialogInput, inputs: List<InputItem>
                ) {

                    val name = inputs.find { it.id == "toolName" }?.value ?: return
                    val desc = inputs.find { it.id == "toolDes" }?.value ?: return
                    val scriptPath = model.scriptPath ?: return

                    fun ensureToolIdAndPersistUidIfMissing(path: String): String {
                        val infoModel = ScriptInfoModel().parseMainFile(java.io.File(path))
                        val mate =
                            infoModel.scriptMate ?: ScriptMate().also { infoModel.scriptMate = it }
                        var uid = mate.scriptUid
                        if (uid.isNullOrBlank()) {
                            uid = ScriptMate.generateScriptUid()
                            mate.scriptUid = uid
                            infoModel.scriptMate = mate
                            infoModel.saveMate()
                            val plainMainFile =
                                java.io.File(path, ScriptConst.SCRIPT_MAIN_FILE_NAME)
                            if (plainMainFile.exists()) {
                                runCatching {
                                    val lines = plainMainFile.readLines().toMutableList()
                                    if (lines.isNotEmpty() && lines[0].startsWith("mate")) {
                                        lines[0] = mate.getCommandLines()
                                        plainMainFile.writeText(lines.joinToString("\n"))
                                    }
                                }
                            }
                        }
                        return "${ScriptConst.SCRIPT_TOOL_ID_PREFIX}$uid"
                    }

                    val toolId = ensureToolIdAndPersistUidIfMissing(scriptPath)
                    val mcpProvider = ComponentManager.getInstance()
                        .getProvider(IMcpProvider::class.java) as? IMcpProvider
                    val alreadyExists = runCatching {
                        mcpProvider?.getRegisteredTools()?.any { it.name == toolId } == true
                    }.getOrDefault(false)

                    val agentProvider = ComponentManager.getInstance()
                        .getProvider(IAgentProvider::class.java) as IAgentProvider?
                    val finish = {
                        agentProvider?.refreshAllMcpServer {
                            listener?.onToolsRegisterFinish(mutableListOf(scriptPath))
                        }
                    }

                    val doRegister = { overwrite: Boolean ->
                        ScriptMcpRegister.registerCustomTool(
                            scriptName = name,
                            scriptDesc = desc,
                            scriptPath = scriptPath,
                            toolId = toolId,
                            overwriteIfExists = overwrite,
                            persistToSp = true
                        )
                        finish()
                    }

                    if (alreadyExists) {
                        DialogScriptAlert(getViewContext())
                            .setTitle(com.hive.i8n.R.string.sc_upload_same_name_title)
                            .setContent(com.hive.i8n.R.string.sc_upload_same_name_content)
                            .setOnDialogEventListener(object :
                                DialogScriptAlert.OnDialogEventListener {
                                override fun onClickEvent(
                                    dialog: DialogScriptAlert,
                                    isCancel: Boolean
                                ) {
                                    dialog.dismiss()
                                    if (!isCancel) {
                                        doRegister(true)
                                    }
                                }
                            })
                            .show()
                    } else {
                        doRegister(false)
                    }
                }

                override fun onCancel() {
                }
            })
            dialog.show()
        }
    }

    override fun startRegisterCustomTools(
        listener: IScriptProvider.OnToolsRegisterToolsListener?
    ) {
        // 直接启动新的 ActivityCreateMcpTool 页面
        // 注意：listener 的回调由 ActivityCreateMcpTool 在创建成功后通过 EventBus 触发
        // 这里不调用 listener?.onComplete()，因为页面刚启动，尚未完成工具创建
        try {
            val activityClass = Class.forName("com.hive.ui.mcp.ActivityCreateMcpTool")
            IntentUtils.safeStartActivity(getViewContext(), Intent(getViewContext(), activityClass))
        } catch (e: Exception) {
            DLog.e("ScriptProvider", "启动 ActivityCreateMcpTool 失败: ${e.message}")
            // fallback: 使用旧的流程（仅在页面不存在时）
            DialogScriptListSelector(getViewContext(), true)
                .setTitle(context.getString(com.hive.i8n.R.string.script_provider_select_script))
                .setOnScriptSelectListener(object :
                    DialogScriptListSelector.OnScriptSelectListener {
                    override fun onSelected(
                        dialog: DialogScriptListSelector, model: ScriptInfoModel
                    ) {
                        dialog.dismiss()
                        model.scriptPath?.run {
                            startRegisterCustomTools(this, listener)
                        }
                    }

                    override fun onDismissed() {

                    }
                }).show()
        }
    }

    override fun showWorkflowSelector(
        context: android.content.Context,
        title: String,
        callback: IScriptProvider.OnWorkflowSelectedCallback
    ) {
        DialogScriptListSelector(context, false)
            .setTitle(title)
            .setOnScriptSelectListener(object :
                DialogScriptListSelector.OnScriptSelectListener {
                override fun onSelected(
                    dialog: DialogScriptListSelector, model: ScriptInfoModel
                ) {
                    dialog.dismiss()
                    callback.onWorkflowSelected(model.scriptPath ?: "", model.scriptName ?: "")
                }

                override fun onDismissed() {
                    callback.onDismissed()
                }
            }).show()
    }

    @Subscribe
    fun onTimeAlarmEvent(e: OnTimeAlarmEvent) {
        //展示运行提示框
        if (e.isBeforeRunning && !ScriptInterpreter.getDefault().isRunning()) {
            ScriptAlarmTaskHelper.showAlarmTipsDialog()
        } else {
            //开始运行
            e.alarmEntity?.run {
                val taskInfo =
                    GsonHelper.getInstance().fromJson(taskInfo, AlarmTaskEntity::class.java)
                taskInfo ?: return
                alarmId ?: return
                ScriptAlarmTaskHelper.startAlarmTask(taskInfo, alarmId)
            }
        }

    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun checkPermissionAndRights(
        path: String?,
        callback: IScriptProvider.OnCheckPermissionsCallback?
    ) {
        path ?: return
        GlobalScope.launch(Dispatchers.IO) {
            val rootScript = ScriptCommandRoot().apply {
                ScriptCommandRoot.loadScriptSync(
                    path,
                    this
                )
            }
            withContext(Dispatchers.Main) {
                ScriptHelper.checkPermissionAndRights(rootScript) {
                    callback?.onSuccess()
                }
            }
        }
    }

    override fun executeScript(path: String?, clearMode: Boolean) {
        path ?: return
        DLog.d(
            "ScriptProvider",
            "executeScript() path=$path, isRunning=${ScriptInterpreter.getDefault().isRunning()}"
        )
        if (ScriptManager.checkAccessibility()) return
        if (ScriptManager.checkServerEnable()) {
            if (ScriptInterpreter.getDefault().isRunning()) {
                DLog.w("ScriptProvider", "executeScript() was already running, stopping first")
                ScriptMenuManager.disableStopDialogOnce()
                ScriptManager.stopPlay()
                ScriptHelper.runInMain({
                    ScriptMenuManager.resetStopDialogOnce()
                    ScriptManager.startPlay(path)
                }, 300)
            } else {
                ScriptMenuManager.resetStopDialogOnce()
                ScriptManager.startPlay(path)
            }
        }
    }

    override fun executeAgentTask(goal: String) {
        val agentProvider = ComponentManager.getInstance()
            .getProvider(IAgentProvider::class.java) as IAgentProvider
        val taskGoal = AgentTaskGoal(
            id = "agent_task_${System.currentTimeMillis()}",
            userInput = goal,
            priority = TaskPriority.NORMAL,
            input = AgentInput(mutableListOf())
        )
        agentProvider.executeAgentTask(taskGoal, null)
    }

    @SuppressLint("CheckResult")
    override fun updateAppList(callback: IScriptProvider.OnAppListCallback?) {
        getAppList().observeOn(AndroidSchedulers.mainThread()).subscribe { ls ->
            sAppList = ls
            callback?.onSuccess()
        }
    }

    private fun getAppList(): Observable<MutableList<FileCardData>> {
        return Observable.create<MutableList<FileCardData>> { observableEmitter ->
            observableEmitter.onNext(
                XAppInfoParser.getAppInfoList(GlobalApp.getContext())
//                    .filter { it.isUserApp }
                    .map {
                        FileCardData.parsePath(it.apkPath!!).apply {
                            cardData = it
                        }
                    }.sortedByDescending { it.fileName }.toMutableList()
            )
            observableEmitter.onComplete()
        }.subscribeOn(Schedulers.io())
    }


    companion object {

        var sAppList: MutableList<FileCardData>? = null

        fun findAppIcon(pkg: String): Drawable? {
            if (sAppList == null) return null
            sAppList?.find {
                (it.cardData as XAppInfoParser.AppInfo?)?.packageName == pkg
            }?.run {
                return@findAppIcon (cardData as XAppInfoParser.AppInfo?)?.icon
            }
            return null
        }

        fun updateApp(callback: IScriptProvider.OnAppListCallback?) {
            ScriptProvider().updateAppList(callback)
        }

        @JvmStatic
        fun getAccessService(): ServiceAccessibility? = ScriptEventHelper.get().getAccessService()

        @JvmStatic
        fun getViewContext(): Context {
            var ctx: Context? =
                ScriptEventHelper.get().getAccessService() ?: GlobalApp.getTopActivity()
            if (ctx == null) {
                ctx = GlobalApp.getAvailableActivity()
            }
            if (ctx == null) {
                ctx = GlobalApp.getContext()
            }
            return ctx!!
        }

        @JvmStatic
        fun isServiceReady() = ScriptEventHelper.get().getAccessService() != null

        /**
         * 检查输入法是否已设置为系统输入法
         * 
         * @return true 如果输入法已启用，false 如果未启用
         */
        @JvmStatic
        fun isInputMethodEnabled(): Boolean {
            return ScriptInputMethodHelper.isInputMethodEnabled(GlobalApp.getContext())
        }

        @JvmStatic
        fun startToSetting() {
            ScriptSettingActivity.start(getViewContext())
        }

        fun startToAccessibilitySetting() {
            getViewContext()
                .startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                })
            FloatDialog().showByActivity()
        }

        /**
         * 检查输入法是否已设置为系统输入法
         *
         * @return true 如果输入法已启用，false 如果未启用
         */
        fun isScriptInputMethodEnabled(): Boolean {
            return ScriptInputMethodHelper.isInputMethodEnabled(GlobalApp.getContext())
        }

        @JvmStatic
        fun getOcrProvider(): IOcrProvider {
            return ComponentManager.getInstance()
                .getProvider(IOcrProvider::class.java) as IOcrProvider
        }

        fun stopService() {
            ScriptManager.stopPlay()
            ServiceAccessibility.stopServiceIntent()
        }
    }
}
