// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.provider;

/**
 * 孤儿 skill/tool 清理结果。
 * 孤儿定义：sources 中所有 scriptUid 指向的脚本目录均已不存在。
 */
public class OrphanCleanupResult {
    public final int skillsRemoved;
    public final int toolsRemoved;
    public final int customToolsRemovedFromSp;

    public OrphanCleanupResult(int skillsRemoved, int toolsRemoved, int customToolsRemovedFromSp) {
        this.skillsRemoved = skillsRemoved;
        this.toolsRemoved = toolsRemoved;
        this.customToolsRemovedFromSp = customToolsRemovedFromSp;
    }

    public int getTotalRemoved() {
        return skillsRemoved + toolsRemoved + customToolsRemovedFromSp;
    }
}
