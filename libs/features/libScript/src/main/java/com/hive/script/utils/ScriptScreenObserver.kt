// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager

class ScriptScreenObserver(private val mContext: Context?) {
    private var mScreenReceiver: ScreenBroadcastReceiver? = null
    private var mScreenStateListener: ScreenStateListener? = null

    init {
        mScreenReceiver = ScreenBroadcastReceiver()
    }

    fun startObserver(listener: ScreenStateListener?) {
        mScreenStateListener = listener
        registerListener()
        screenState
    }

    fun shutdownObserver() {
        unregisterListener()
    }

    private val screenState: Unit
        /**
         * 获取screen状态
         */
        get() {
            if (mContext == null) {
                return
            }
            val manager = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (manager.isScreenOn) {
                if (mScreenStateListener != null) {
                    mScreenStateListener!!.onScreenOn()
                }
            } else {
                if (mScreenStateListener != null) {
                    mScreenStateListener!!.onScreenOff()
                }
            }
        }

    private fun registerListener() {
        if (mContext != null) {
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_SCREEN_ON)
            filter.addAction(Intent.ACTION_SCREEN_OFF)
            filter.addAction(Intent.ACTION_USER_PRESENT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mContext.registerReceiver(mScreenReceiver, filter, Context.RECEIVER_EXPORTED)
            }else{
                mContext.registerReceiver(mScreenReceiver, filter)
            }
        }
    }

    private fun unregisterListener() {
        if (mScreenStateListener == null) return
        mContext?.unregisterReceiver(mScreenReceiver)
        mScreenStateListener = null
    }

    private inner class ScreenBroadcastReceiver : BroadcastReceiver() {
        private var action: String? = null
        override fun onReceive(context: Context, intent: Intent) {
            action = intent.action
            if (Intent.ACTION_SCREEN_ON == action) { // 开屏
                mScreenStateListener?.onScreenOn()
            } else if (Intent.ACTION_SCREEN_OFF == action) { // 锁屏
                mScreenStateListener?.onScreenOff()
            } else if (Intent.ACTION_USER_PRESENT == action) { // 解锁
                mScreenStateListener?.onUserPresent()
            }
        }
    }

    interface ScreenStateListener {
        // 返回给调用者屏幕状态信息
        fun onScreenOn()
        fun onScreenOff()
        fun onUserPresent()
    }
}