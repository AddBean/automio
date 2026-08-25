// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.common

import android.os.Handler
import android.os.Looper
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.ISkillStateObserver
import com.hive.plugin.agent.model.SkillResult
import com.hive.plugin.provider.IAgentProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.views.manager.ScriptManager
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

object ResourceRunStateStore : ISkillStateObserver,
    ScriptInterpreterObserver.InterpreterExecuteObserver {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<() -> Unit>()
    private val runningSkillTasks = ConcurrentHashMap<String, String>()
    private val runningToolJobs = ConcurrentHashMap<String, Job>()
    private val customToolScriptPaths = ConcurrentHashMap<String, String>()
    private var observerRegistered = false

    fun ensureRegistered() {
        if (observerRegistered) return
        observerRegistered = true
        (ComponentManager.getInstance().getProvider(IAgentProvider::class.java) as? IAgentProvider)
            ?.registerSkillStateObserver(this)
        ScriptInterpreterObserver.registerInterpreterObserver(this)
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun isSkillRunning(skillId: String): Boolean {
        return runningSkillTasks.containsKey(skillId)
    }

    fun getSkillTaskId(skillId: String): String? {
        return runningSkillTasks[skillId]
    }

    fun isToolRunning(toolName: String): Boolean {
        if (runningToolJobs.containsKey(toolName)) return true
        val scriptPath = customToolScriptPaths[toolName] ?: return false
        val runningPath = ScriptManager.getRunningScript()?.scriptPath ?: return false
        return runningPath.trimEnd('/') == scriptPath.trimEnd('/')
    }

    fun startToolJob(toolName: String, job: Job) {
        runningToolJobs[toolName] = job
        notifyChanged()
    }

    fun finishToolJob(toolName: String, job: Job?) {
        if (job == null) {
            runningToolJobs.remove(toolName)
        } else {
            runningToolJobs.remove(toolName, job)
        }
        notifyChanged()
    }

    fun rememberCustomToolScript(toolName: String, scriptPath: String) {
        customToolScriptPaths[toolName] = scriptPath
        notifyChanged()
    }

    fun stopSkill(skillId: String): Boolean {
        val taskId = runningSkillTasks[skillId] ?: return false
        (ComponentManager.getInstance().getProvider(IAgentProvider::class.java) as? IAgentProvider)
            ?.stopTask(taskId)
        return true
    }

    fun stopTool(toolName: String): Boolean {
        runningToolJobs[toolName]?.cancel()
        val scriptPath = customToolScriptPaths[toolName]
        val runningPath = ScriptManager.getRunningScript()?.scriptPath
        if (!scriptPath.isNullOrBlank() && !runningPath.isNullOrBlank() &&
            runningPath.trimEnd('/') == scriptPath.trimEnd('/')
        ) {
            ScriptManager.stopPlay()
            return true
        }
        return runningToolJobs.containsKey(toolName)
    }

    override fun onSkillExecuteStart(taskId: String) {
        parseSkillId(taskId)?.let { skillId ->
            runningSkillTasks[skillId] = taskId
            notifyChanged()
        }
    }

    override fun onSkillExecuteEnd(taskId: String, result: SkillResult?) {
        parseSkillId(taskId)?.let { skillId ->
            runningSkillTasks.remove(skillId, taskId)
            notifyChanged()
        }
    }

    override fun onInterpreterStart(cmd: ScriptCommand) {
        notifyChanged()
    }

    override fun onInterpreterEnd(cmd: ScriptCommand) {
        notifyChanged()
    }

    override fun onInterpreterTryStop(cmd: ScriptCommand) {
        notifyChanged()
    }

    private fun parseSkillId(taskId: String): String? {
        if (!taskId.startsWith("skill-")) return null
        val body = taskId.removePrefix("skill-")
        val split = body.lastIndexOf('-')
        if (split <= 0) return null
        return body.substring(0, split)
    }

    private fun notifyChanged() {
        mainHandler.post {
            listeners.forEach { it.invoke() }
        }
    }
}
