// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import com.hive.script.scope.ScopedSkillSpec

/**
 * 依赖列表中 Skill 编辑的回调提供者。
 * 由 app 模块在启动时注入，用于复用 DialogSkillCreate 进行 skill 编辑。
 * 若不注入，则 fallback 到 DialogCmdDialogInput 简单表单。
 */
object ScopeEditProvider {

    /**
     * @param context 用于获取 FragmentManager 等
     * @param skill 待编辑的 skill
     * @param scopeScriptPath 主脚本目录路径
     * @param onSaved 保存完成后的回调，用于刷新依赖列表
     */
    var onEditSkill: ((
        context: Context,
        skill: ScopedSkillSpec,
        scopeScriptPath: String,
        onSaved: () -> Unit
    ) -> Unit)? = null
}
