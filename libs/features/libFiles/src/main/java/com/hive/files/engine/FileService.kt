// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.engine

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.hive.plugin.ComponentConst
import com.hive.plugin.ComponentManager
import com.hive.utils.GlobalApp
import com.raizlabs.android.dbflow.config.FlowManager

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/24/21
 */
class FileService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ComponentManager.getInstance().register(ComponentConst.XFILE_PROVIDER)
        FileEngine.instance.startIndexing()
    }

    companion object {
        fun start() {
            GlobalApp.sContext.startService(Intent(GlobalApp.sContext, FileService::class.java))
        }
    }
}