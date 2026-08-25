// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import com.hive.script.BuildConfig

object DebugTools {

    class SlowMonitor{
        companion object{
            const val Threshold = 5000L
            const val BEGIN_TAG = ">>>>> Dispatching"
            const val END_TAG = "<<<<< Finished"
        }
        private var start = 0L
        @Volatile
        private var mHasEnd = false
        private var handlerThreadWrapper = HandlerThreadWrapper()
        private var collectRunnable = CollectRunnable()

        fun watch(msg :String){
            if (msg.isEmpty()) {
                return
            }
            if(msg.startsWith(BEGIN_TAG)){
                start = System.currentTimeMillis()
                mHasEnd = false

                //需要单独搞个线程来获取堆栈
                handlerThreadWrapper.handler.postDelayed(
                    collectRunnable,
                    Threshold
                )
            }else{
                mHasEnd = true
                if (System.currentTimeMillis() - start < Threshold) {
                    handlerThreadWrapper.handler.removeCallbacks(collectRunnable)
                }
            }
        }

        fun getMainThreadStackTrace(): String {
            val stackTrace = Looper.getMainLooper().thread.stackTrace
            return StringBuilder().apply {
                for (stackTraceElement in stackTrace) {
                    append(stackTraceElement.toString())
                    append("\n")
                }
            }.toString()
        }

        inner class CollectRunnable : Runnable {
            override fun run() {
                if (!mHasEnd) {
                    //主线程堆栈给拿出来，打印一下
                    if(BuildConfig.DEBUG){
                        Log.e("ScriptSlowMonitor", "${System.currentTimeMillis() - start}ms\n" + getMainThreadStackTrace())
                    }
                }
            }
        }

        class HandlerThreadWrapper {
            var handler: Handler
            init {
                val handlerThread = HandlerThread("LooperHandlerThread")
                handlerThread.start()
                handler = Handler(handlerThread.looper)
            }
        }

    }

    init {
        if(BuildConfig.DEBUG){
            val sm = SlowMonitor()
            Looper.getMainLooper().setMessageLogging {
                sm.watch(it)
            }
        }
    }

    private var point = 0L
    fun start(){
        point = System.currentTimeMillis()
    }

    fun end(msg: String = ""){
        if(BuildConfig.DEBUG) {
            val stack = Throwable()
            Log.e(
                "DebugTools",
                        "/${msg}/ at ${stack.stackTrace[1].className}.${stack.stackTrace[1].methodName}(${stack.stackTrace[1].fileName}:${stack.stackTrace[1].lineNumber}) " +
                        "- ${System.currentTimeMillis() - point}ms"
            )
        }
    }

    fun stack(){
        if(BuildConfig.DEBUG) {
            Log.e("DebugTools","\n")
            Throwable().stackTrace.forEachIndexed { _, stackTraceElement ->
                "at ${stackTraceElement.className}.${stackTraceElement.methodName}(${stackTraceElement.fileName}:${stackTraceElement.lineNumber}) "
            }
        }
    }

    fun debugView(view : View){
        if(BuildConfig.DEBUG){
            view.viewTreeObserver.addOnPreDrawListener (object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    end()
                    view.viewTreeObserver.removeOnPreDrawListener(this)
                    return false
                }
            })

            view.viewTreeObserver.addOnGlobalLayoutListener (object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    end()
                    view.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                }
            })

            view.post {
                end()
            }
        }
    }
}