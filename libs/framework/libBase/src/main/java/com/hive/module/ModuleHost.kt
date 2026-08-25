// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.module

import android.content.Context
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel

interface ModuleHost {

    fun getContext(): Context

    fun <T : View?> findView(id: Int): T

    fun retrieveLifecycleOwner(): LifecycleOwner

    fun retrieveFragmentManager(): FragmentManager

    fun <T> getDataProvider(): T

    fun <T : ViewModel?> getViewModel(clazz: Class<T>): T = getDataProvider()

}