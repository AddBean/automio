// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.module

/**
 * 模块生命周期接口
 */
interface ModuleLifecycle {


    /**
     * 对应Activity的[android.app.Activity.onRestart]
     */
    fun onRestart() {

    }


    /**
     * 对应Activity的[android.app.Activity.onCreate]
     */
    fun onCreate() {

    }

    /**
     * 对应Activity的[android.app.Activity.onStart]
     */
    fun onStart() {

    }

    /**
     * 对应Activity的[android.app.Activity.onResume]
     */
    fun onResume() {

    }

    /**
     * 对应Activity的[android.app.Activity.onPause]
     */
    fun onPause() {

    }

    /**
     * 对应Activity的[android.app.Activity.onStop]
     */
    fun onStop() {

    }

    /**
     * 对应Activity的[android.app.Activity.onDestroy]
     */
    fun onDestroy() {

    }
}