// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.debug

import com.hive.utils.utils.GsonHelper
import java.util.concurrent.ConcurrentHashMap

/**
 * 简化的调试工具
 * 记录工作流执行过程，生成执行报告
 */
class SimpleDebugger {
    
    private val executionLogs = ConcurrentHashMap<String, MutableList<SimpleDebugLog>>()
    private val maxLogsPerTask = 1000
    
    /**
     * 记录执行日志
     */
    fun logExecution(
        taskId: String, 
        stepId: String?, 
        event: String, 
        details: Map<String, String> = emptyMap()
    ) {
        val log = SimpleDebugLog(
            timestamp = System.currentTimeMillis(),
            taskId = taskId,
            stepId = stepId,
            event = event,
            details = details
        )
        
        val logs = executionLogs.getOrPut(taskId) { mutableListOf() }
        logs.add(log)
        
        // 限制日志数量
        if (logs.size > maxLogsPerTask) {
            logs.removeAt(0)
        }
    }
    
    /**
     * 生成简单执行报告
     */
    fun generateSimpleReport(taskId: String): String {
        val logs = executionLogs[taskId] ?: return "工作流 $taskId 没有找到执行日志"
        
        if (logs.isEmpty()) {
            return "工作流 $taskId 没有执行记录"
        }
        
        return buildString {
            appendLine("=== 工作流执行报告 ===")
            appendLine("工作流ID: $taskId")
            appendLine("开始时间: ${formatTimestamp(logs.first().timestamp)}")
            appendLine("结束时间: ${formatTimestamp(logs.last().timestamp)}")
            appendLine("执行时长: ${logs.last().timestamp - logs.first().timestamp}ms")
            appendLine("总步骤数: ${logs.size}")
            appendLine()
            
            appendLine("=== 执行详情 ===")
            logs.forEach { log ->
                val stepInfo = log.stepId?.let { " [步骤: $it]" } ?: ""
                appendLine("${formatTimestamp(log.timestamp)} - ${log.event}$stepInfo")
                if (log.details.isNotEmpty()) {
                    log.details.forEach { (key, value) ->
                        appendLine("  └─ $key: $value")
                    }
                }
            }
        }
    }
    
    /**
     * 导出JSON格式的日志
     */
    fun exportLogsAsJson(taskId: String): String {
        val logs = executionLogs[taskId] ?: emptyList()
        return GsonHelper.getInstance().toJson(logs)
    }
    
    /**
     * 获取所有工作流ID
     */
    fun getAllTaskIds(): List<String> {
        return executionLogs.keys.toList()
    }
    
    /**
     * 获取工作流统计信息
     */
    fun getTaskStats(taskId: String): TaskStats? {
        val logs = executionLogs[taskId] ?: return null
        
        if (logs.isEmpty()) return null
        
        val startTime = logs.first().timestamp
        val endTime = logs.last().timestamp
        val duration = endTime - startTime
        
        val successEvents = logs.count { "成功" in it.event || "完成" in it.event }
        val errorEvents = logs.count { "失败" in it.event || "错误" in it.event }
        
        return TaskStats(
            taskId = taskId,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            totalSteps = logs.size,
            successCount = successEvents,
            errorCount = errorEvents
        )
    }
    
    /**
     * 清理旧日志
     */
    fun clearOldLogs(olderThanMs: Long = 24 * 60 * 60 * 1000L) {
        val cutoffTime = System.currentTimeMillis() - olderThanMs
        
        executionLogs.entries.removeAll { (_, logs) ->
            logs.isNotEmpty() && logs.last().timestamp < cutoffTime
        }
    }
    
    /**
     * 清理特定工作流的日志
     */
    fun clearTaskLogs(taskId: String) {
        executionLogs.remove(taskId)
    }
    
    /**
     * 获取系统统计信息
     */
    fun getSystemStats(): SystemStats {
        val allLogs = executionLogs.values.flatten()
        val totalTasks = executionLogs.size
        val totalSteps = allLogs.size
        
        val successTasks = executionLogs.count { (_, logs) ->
            logs.any { "成功" in it.event || "完成" in it.event }
        }
        
        val errorTasks = executionLogs.count { (_, logs) ->
            logs.any { "失败" in it.event || "错误" in it.event }
        }
        
        val avgDuration = if (executionLogs.isNotEmpty()) {
            executionLogs.values.mapNotNull { logs ->
                if (logs.size >= 2) logs.last().timestamp - logs.first().timestamp else null
            }.average()
        } else 0.0
        
        return SystemStats(
            totalTasks = totalTasks,
            totalSteps = totalSteps,
            successTasks = successTasks,
            errorTasks = errorTasks,
            averageDuration = avgDuration.toLong()
        )
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        return java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
}

/**
 * 调试日志数据类
 */
data class SimpleDebugLog(
    val timestamp: Long,
    val taskId: String,
    val stepId: String?,
    val event: String,
    val details: Map<String, String> = emptyMap()  // 简化为String类型以便序列化
)

/**
 * 工作流统计信息
 */
data class TaskStats(
    val taskId: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val totalSteps: Int,
    val successCount: Int,
    val errorCount: Int
) {
    val successRate: Float get() = if (totalSteps > 0) successCount.toFloat() / totalSteps else 0f
    val errorRate: Float get() = if (totalSteps > 0) errorCount.toFloat() / totalSteps else 0f
}

/**
 * 系统统计信息
 */
data class SystemStats(
    val totalTasks: Int,
    val totalSteps: Int,
    val successTasks: Int,
    val errorTasks: Int,
    val averageDuration: Long
) {
    val taskSuccessRate: Float get() = if (totalTasks > 0) successTasks.toFloat() / totalTasks else 0f
    val taskErrorRate: Float get() = if (totalTasks > 0) errorTasks.toFloat() / totalTasks else 0f
} 