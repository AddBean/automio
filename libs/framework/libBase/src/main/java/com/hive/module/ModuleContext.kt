// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.module

import android.content.Context
import android.view.View
import androidx.lifecycle.*

class ModuleContext(private val host: ModuleHost) {


    fun addObserver(observer: LifecycleObserver) {
        getLifecycle().addObserver(observer)
    }

    fun removeObserver(observer: LifecycleObserver) {
        getLifecycle().removeObserver(observer)
    }

    fun getLifecycle(): Lifecycle {
        return getLifecycleOwner().lifecycle
    }

    fun getLifecycleOwner(): LifecycleOwner {
        return host.retrieveLifecycleOwner()
    }

    fun getContext(): Context = host.getContext()

    fun <T : View?> findViewById(id: Int) = host.findView<T>(id)

    fun getResources() = getContext().resources!!

    fun getFragmentManager() = host.retrieveFragmentManager()

    fun <T> getDataProvider() = host.getDataProvider<T>()

    fun <T : ViewModel?> getViewModel(clazz: Class<T>): T = host.getViewModel(clazz)

    internal fun createChildModuleManager() = ModuleManagerImpl.newInstance(host)
}