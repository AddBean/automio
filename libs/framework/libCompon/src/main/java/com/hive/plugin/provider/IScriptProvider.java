// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.provider;

import com.hive.plugin.IComponentProvider;
import com.hive.plugin.mcp.model.McpTool;

import java.util.List;

/**
 * @author jiadou
 * @date 6/11/21
 */
public interface IScriptProvider extends IComponentProvider {

    void checkPermissionAndRights(String path, OnCheckPermissionsCallback callback);

    void executeScript(String path, Boolean clearMode);

    void executeAgentTask(String goal);

    void updateAppList(OnAppListCallback callback);

    void registerToLocalAgent();

    void unregisterCustomTool(String toolName);

    void startRegisterCustomTools(OnToolsRegisterToolsListener listener);

    void startRegisterCustomTools(String scriptPath, OnToolsRegisterToolsListener listener);

    void initAgentService(IAgentProvider provider);

    boolean isAccessServiceReady();

    /**
     * 获取辅助功能服务实例
     * @return AccessibilityService 实例，如果未运行返回 null
     */
    android.accessibilityservice.AccessibilityService getAccessibilityService();

    /**
     * 返回最近一次 Agent 任务录制到的命令列表（含 for 块，已展平），无则返回 null。
     */
    List<String> getLastRecordedCommands();

    /**
     * 将命令列表保存为工作流。commands 需为已展平格式（每行一个元素）。
     * @param namePrefix 工作流名称前缀，可为 null
     * @param callback 保存完成后回调，success 表示是否成功
     */
    void saveCommandsToWorkflow(List<String> commands, String namePrefix, OnSaveWorkflowCallback callback);

    /**
     * 清理孤儿 skill/tool：sources 中所有 scriptUid 指向的脚本目录均已不存在时，卸载并移除。
     * @return 清理结果（各类型移除数量）
     */
    OrphanCleanupResult cleanupOrphanSkillsAndTools();

    interface OnCheckPermissionsCallback {
        void onSuccess();
    }

    interface OnAppListCallback {
        void onSuccess();
    }

    interface OnToolsRegisterToolsListener {
        void onToolsRegisterFinish(List<String> scriptPaths);
    }

    interface OnSaveWorkflowCallback {
        void onResult(boolean success);
    }

    interface OnWorkflowSelectedCallback {
        void onWorkflowSelected(String scriptPath, String scriptName);
        void onDismissed();
    }

    /**
     * Show workflow selector dialog.
     * @param context the context to show the dialog
     * @param title dialog title
     * @param callback result callback
     */
    void showWorkflowSelector(android.content.Context context, String title, OnWorkflowSelectedCallback callback);
}
