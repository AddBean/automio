// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.module

import android.content.Intent
import android.view.View
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/**
 * 模块基础类
 */
open class Module : ModuleLifecycle, LifecycleObserver {


    protected lateinit var moduleContext: ModuleContext

    private var childModuleManager: ModuleManager? = null


    @CallSuper
    open fun attach(moduleContext: ModuleContext) {
        this.moduleContext = moduleContext
        this.moduleContext.addObserver(this)
    }

    @CallSuper
    open fun detach() {
        this.moduleContext.removeObserver(this)
        childModuleManager?.clearModules()
    }

    @CallSuper
    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        childModuleManager?.dispatchActivityResult(requestCode, resultCode, data)
    }

    open fun finish() {
        childModuleManager?.dispatchFinish()
    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_CREATE)
    override fun onCreate() {

    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_START)
    override fun onStart() {

    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_RESUME)
    override fun onResume() {

    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_PAUSE)
    override fun onPause() {

    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_STOP)
    override fun onStop() {

    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_DESTROY)
    override fun onDestroy() {

    }

    protected fun <T : View?> findViewById(id: Int) = moduleContext.findViewById<T>(id)

    protected fun getChildModuleManager(): ModuleManager? {
        if (childModuleManager == null) {
            childModuleManager = moduleContext.createChildModuleManager()
        }
        return childModuleManager
    }
}