// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.provider;

import android.content.Context;

import com.hive.plugin.IComponentProvider;
import com.hive.plugin.agent.model.SkillSpec;

public interface IDependencyNavigationProvider extends IComponentProvider {

    void openSkillDetail(Context context, SkillSpec skillSpec);

    void openToolDetail(
            Context context,
            String toolId,
            String toolDisplayName,
            String toolDescription,
            String toolType,
            String toolSchema,
            String customScriptPath
    );
}
