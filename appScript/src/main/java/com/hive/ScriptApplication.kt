// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentActivity
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.hive.agent.XAgent
import com.hive.agent.skill.SkillPersistence
import com.hive.app.script.BuildConfig
import com.hive.app.script.R
import com.hive.config.BuildConfigHelper
import com.hive.framework.CommonApplication
import com.hive.framework.coper.ScriptManagerImpl
import com.hive.framework.crash.CrashHelper
import com.hive.plugin.ComponentConst
import com.hive.plugin.ComponentManager
import com.hive.plugin.mcp.McpConst
import com.hive.plugin.provider.IAgentProvider
import com.hive.plugin.provider.IMcpProvider
import com.hive.plugin.provider.IScriptProvider
import com.hive.script.base.ScriptConst
import com.hive.script.setting.ScriptSetting
import com.hive.script.views.dialog.ScopeEditProvider
import com.hive.ui.skill.DialogSkillCreate
import com.hive.utils.GlobalApp
import com.hive.utils.darkmode.MiuiDarkModeCompat
import com.hive.utils.debug.DLog
import com.hive.utils.global.SPTools
import com.hive.utils.statusbar.StatusBarCompat
import com.hive.utils.utils.ColorUtils

/**
 *
 * @author jiadou
 * @date 3/24/21
 */
class ScriptApplication : CommonApplication() {

    override fun attachBaseContext(base: Context) {
        // 禁用暗黑模式（包括 MIUI 特殊处理）
        MiuiDarkModeCompat.disableForceDarkMode(base)
        super.attachBaseContext(base)
        GlobalApp.sMainActivityClass = ActivityTab::class.java
        ScriptManagerImpl.stopService()
    }

    override fun onProcessCreate(processName: String?) {
        // 在异步 Component 初始化之前同步注册 Timer DB，避免 Profile 页抢先查询崩溃
        com.hive.timer.TimerProvider.ensureDbInitialized()
        super.onProcessCreate(processName)
    }

    override fun onProcessCreateThread(processName: String) {
        DLog.sEnable = true
        StatusBarCompat.ThemeColor = ColorUtils.toHexColor(resources.getColor(com.hive.i8n.R.color.colorPrimary))
        GlobalApp.isSupportStatusBar = false
        ScriptConst.supportImport = BuildConfigHelper.getMapBoolean("supportImport")
        ScriptConst.runningDialogShow = BuildConfigHelper.getMapBoolean("runningDialogShow")
        CrashHelper.getInstance().init(this)
        ScriptManagerImpl.initScreen()
        ComponentManager.getInstance().register(ComponentConst.XFILE_PROVIDER)
        ComponentManager.getInstance().register(ComponentConst.EDITOR_PROVIDER)
        ComponentManager.getInstance().register(ComponentConst.OPENCV_PROVIDER)
        ComponentManager.getInstance().register(ComponentConst.OCR_PROVIDER)
        ComponentManager.getInstance().register(ComponentConst.PYTHON_PROVIDER)
        // 启动 Chaquopy Python 运行时（PythonProvider 首次调用时也会检查，此处提前初始化）
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(this))
            }
        } catch (e: Exception) {
            DLog.e("Python init failed: ${e.message}")
        }
        ComponentManager.getInstance().register(ComponentConst.SCRIPT_PROVIDER)
        ComponentManager.getInstance().register(ComponentConst.MCP_PROVIDER)
        ComponentManager.getInstance().register(ComponentConst.AGENT_PROVIDER)
        ComponentManager.getInstance().register(ComponentConst.DEPENDENCY_NAVIGATION_PROVIDER)
        ComponentManager.getInstance().register(ComponentConst.MS_AUDIO_ASR_PROVIDER)
        ComponentManager.getInstance().register(ComponentConst.AUDIO_PROVIDER)

        DLog.e("initApp")
        ColorUtils.setDefaultColors(
            intArrayOf(
                Color.BLACK, Color.BLACK
            )
        )
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        val appPermissionGrant = SPTools.getInstance()
            .getBoolean(ScriptConst.SCRIPT_SP_APP_PERMISSION_GRANT, false)
        if (appPermissionGrant) {
            ScriptManagerImpl.updateApp()
        }
        ScriptSetting.init()
        initScopeEditProvider()
        initAgentService()
    }

    private fun initAgentService() {
        Thread {
            val mcpProvider = ComponentManager.getInstance()
                .getProvider(IMcpProvider::class.java) as IMcpProvider
            val scriptProvider = ComponentManager.getInstance()
                .getProvider(IScriptProvider::class.java) as IScriptProvider
            val agentProvider = ComponentManager.getInstance()
                .getProvider(IAgentProvider::class.java) as IAgentProvider
            mcpProvider.startMcpService(
                McpConst.SsePort, McpConst.StreamablePort
            ) {
                DLog.e("agentDebug", "initAgentService: mcp started")
                scriptProvider.registerToLocalAgent()
                agentProvider.initAgentService()
                agentProvider.registerGlobalMcpServer(
                    XAgent.GLOBAL_MCP_NAME,
                    mcpProvider.getStreamableServerUrl()
                )

                // 加载用户自定义 Skill（持久化），并注册到 Agent
                runCatching {
                    SkillPersistence.loadCustomSkills().forEach { spec ->
                        agentProvider.registerSkillSpec(spec)
                    }
                }.onFailure {
                    DLog.e("agentDebug", "load custom skills failed: ${it.message}")
                }

                scriptProvider.initAgentService(agentProvider)
            }

        }.start()

    }

    private fun initScopeEditProvider() {
        ScopeEditProvider.onEditSkill = onEditSkill@{ context, skill, scopeScriptPath, onSaved ->
            val fm = (context as? FragmentActivity)?.supportFragmentManager ?: return@onEditSkill
            DialogSkillCreate.show(
                fragmentManager = fm,
                initial = skill.toSkillSpec(),
                onSaved = onSaved,
                scopeScriptPath = scopeScriptPath
            )
        }
    }

    override fun onMainProcessCreateThread() {
    }

    override fun needShowPermissionDialog() = true

    override fun onTerminate() {
        ScriptManagerImpl.onTerminal()

        // 停止MCP服务
        val mcpProvider = ComponentManager.getInstance()
            .getProvider(IMcpProvider::class.java) as IMcpProvider?
        mcpProvider?.stopMcpService()

        super.onTerminate()
    }

}
