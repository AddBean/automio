// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.net.Proxy
import android.os.Build
import android.os.Process
import android.os.StrictMode
import android.text.TextUtils
import androidx.multidex.MultiDex
import com.hive.config.BuildConfigHelper
import com.hive.engineer.LoggerView
import com.hive.net.INetInterface
import com.hive.net.engineer.EngineerConfig
import com.hive.utils.DefaultSPTools
import com.hive.utils.GlobalApp
import com.hive.utils.LanguageManager
import com.hive.utils.LanguageManager.loadLanguage
import com.hive.utils.debug.DLog
import com.hive.utils.utils.ProcessUtils
import com.hive.views.widgets.CommonToast
import com.raizlabs.android.dbflow.config.FlowManager

abstract class BaseApplication : Application(), INetInterface {
    protected var mCurrentProcessName: String? = null
    private var isApplicationInit = false

    var isPermissionGranted
        get() = DefaultSPTools.getInstance()
            .getBoolean(DefaultSPTools.APP_GLOBAL_PERMISSION_GRANT, false)
        set(value) = DefaultSPTools.getInstance()
            .putBoolean(DefaultSPTools.APP_GLOBAL_PERMISSION_GRANT, value)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        LanguageManager.attachBaseContext(this)
        GlobalApp.init(this)
        GlobalApp.setFlavorName(BuildConfigHelper.getMapNoNullString("flavorName"))
        MultiDex.install(this)
        initStrictMode()
    }

    private fun initStrictMode() {
        if (!BuildConfig.DEBUG) return
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build()
        )
    }

    override fun onCreate() {
        super.onCreate()
        loadLanguage(this)
        if (isPermissionGranted || !needShowPermissionDialog()) {
            startInitApplication()
        }
    }

    open fun needShowPermissionDialog(): Boolean = true

    fun startInitApplication() {
        if (isApplicationInit) return
        isApplicationInit = true
        FlowManager.init(this)
        mCurrentProcessName = ProcessUtils.getCurrentProcessName(this)
        DLog.e("正在启动进程**********$mCurrentProcessName**********")
        onProcessCreate(mCurrentProcessName)
        if (TextUtils.equals(mCurrentProcessName, packageName)) {
            onMainProcessCreate()
        }
        initLoggerView()
        if (enableProxyCheck()) checkWifiProxy()
    }

    private fun checkWifiProxy(): Boolean {
        val proxyAddress: String?
        val proxyPort: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            proxyAddress = System.getProperty("http.proxyHost")
            proxyPort = System.getProperty("http.proxyPort")?.toIntOrNull() ?: -1
        } else {
            proxyAddress = Proxy.getHost(this)
            proxyPort = Proxy.getPort(this)
        }
        val isProxy = !TextUtils.isEmpty(proxyAddress) && proxyPort != -1
        if (isProxy && !BuildConfig.DEBUG) {
            CommonToast.getInstance().showToast(
                GlobalApp.getContext().getString(com.hive.i8n.R.string.base_app_proxy_error)
            )
            Process.killProcess(Process.myPid())
        }
        return isProxy
    }

    open fun enableProxyCheck() = false

    open fun onMainProcessCreate() = Unit

    private fun initLoggerView() {
        if (TextUtils.equals(mCurrentProcessName, packageName)) {
            val config = EngineerConfig.read()
            if (config.engineerOn && config.loggerOn && LoggerView.getInstance() != null) {
                LoggerView.getInstance().attachToWindow(null)
            }
        }
    }

    abstract fun onProcessCreate(processName: String?)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        loadLanguage(this)
    }

    val isMainProcess: Boolean
        get() = TextUtils.equals(
            ProcessUtils.getCurrentProcessName(GlobalApp.getContext()),
            GlobalApp.getContext().packageName
        )
}
