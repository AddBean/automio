// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.module

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import java.lang.IllegalArgumentException

class ModuleManagerImpl(host: ModuleHost) : ModuleManager, ModuleLifecycle, LifecycleObserver {
    private val moduleContext: ModuleContext = ModuleContext(host)

    companion object {
        @JvmStatic
        fun newInstance(host: ModuleHost): ModuleManager {
            val manager = ModuleManagerImpl(host)
            host.retrieveLifecycleOwner().lifecycle.addObserver(manager)
            return manager
        }
    }

    private val modules = arrayListOf<Module>()

    override fun addModule(module: Module) {
        modules.add(module)
        module.attach(moduleContext)
    }

    override fun <T : Module> addModule(clazz: Class<T>) {
        addModule(clazz.newInstance())
    }

    override fun removeModule(module: Module) {
        if (modules.remove(module)) {
            module.detach()
            return
        }
        throw IllegalArgumentException("can't find module:${module.javaClass.name}")
    }

    override fun <T : Module> removeModule(clazz: Class<T>) {
        findModule(clazz)?.let {
            removeModule(it)
        }
    }

    override fun clearModules() {
        modules.forEach {
            it.detach()
        }
        modules.clear()
    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_DESTROY)
    override fun onDestroy() {
        super.onDestroy()
        modules.clear()
    }

    override fun <T : Module> findModule(clazz: Class<T>): T? {
        return modules.find {
            it.javaClass.name == clazz.name
        } as? T
    }

    override fun dispatchActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        modules.forEach {
            it.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun dispatchFinish() {
        modules.forEach {
            it.finish()
        }
    }
}