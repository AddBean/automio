// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.core

class ScriptThread private constructor(target: Runnable?, name: String) : Thread(target, name) {

    var isAlreadyStop = false

    override fun start() {
        super.start()
        scriptThreads.add(this)
    }

    override fun interrupt() {
        super.interrupt()
        scriptThreads.remove(this)
        isAlreadyStop = true
    }

    companion object {

        private val scriptThreads = mutableListOf<ScriptThread>()

        fun stopAll() {
            scriptThreads.forEach {
                if (!it.isInterrupted)
                    it.interrupt()
            }
            scriptThreads.clear()
        }

        fun newThread(target: Runnable?): ScriptThread {
            return ScriptThread(target, "ScriptThread-${System.currentTimeMillis()}")
        }

        fun currentThread(): ScriptThread? {
            if (Thread.currentThread() is ScriptThread) {
                return Thread.currentThread() as ScriptThread
            } else {
                return null
            }
        }
    }
}