// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files;

import android.content.Context;

import com.hive.plugin.provider.IXFileProvider;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.config.xfilesGeneratedDatabaseHolder;

/**
 * @author jiadou
 * @date 4/23/21
 */
public class XFileProvider implements IXFileProvider {
    @Override
    public void init(Context context) {
        FlowManager.initModule(xfilesGeneratedDatabaseHolder.class);
    }
}
