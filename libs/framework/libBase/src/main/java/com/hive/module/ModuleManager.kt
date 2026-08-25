// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.module

import android.content.Intent

/**
 * 模块管理功能接口
 */
interface ModuleManager {

    /**
     * 添加一个模块
     */
    fun addModule(module: Module)

    /**
     * 添加一个模块
     * @param clazz 目标模块类，只能有默认无参构造方法
     */
    fun <T : Module> addModule(clazz: Class<T>)

    /**
     * 移除一个模块
     */
    fun removeModule(module: Module)

    /**
     * 移除一个模块
     * @param clazz 目标模块类
     */
    fun <T : Module> removeModule(clazz: Class<T>)

    /**
     * 清除所有的Module
     */
    fun clearModules()

    /**
     * 找到某个module
     */
    fun <T : Module> findModule(clazz: Class<T>): T?

    /**
     * 分发Activity的[android.app.Activity.onActivityResult]事件
     */
    fun dispatchActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

    /**
     * 分发Activity的[android.app.Activity.finish]事件
     */
    fun dispatchFinish()
}