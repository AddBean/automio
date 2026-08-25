// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

object FrameRateMonitor {
    private const val TAG = "FrameRateMonitor"
    private const val MONITOR_INTERVAL: Long = 1000
    private var lastFrameTimeNanos: Long = 0
    private var frameCount: Long = 0
    private var monitoringStartTime: Long = 0
    private var frameCallback: Choreographer.FrameCallback? = null
    private var frameView : Any? = null

    fun startMonitoring() {
        monitoringStartTime = SystemClock.elapsedRealtime()

        if(BuildConfig.DEBUG){
            GlobalApp.getApp().registerActivityLifecycleCallbacks(object :Application.ActivityLifecycleCallbacks{
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

                override fun onActivityStarted(activity: Activity) {
                    frameView = activity.window.decorView.findViewWithTag<TextView>("frameView_left_top")

                    if(frameView == null){
                        activity.window.decorView.findViewById<FrameLayout>(android.R.id.content)?.apply {
                            frameView = TextView(context).apply {
                                tag = "frameView_left_top"
                                background = null
                                textSize = 30f
                                typeface = Typeface.DEFAULT_BOLD
                                setTextColor(Color.RED)
                            }
                            addView(
                                frameView as View, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT)
                                .apply {
                                    gravity = Gravity.START or Gravity.TOP
                                    setMargins(30, 30,0,0)
                                })
                        }
                    }
                }

                override fun onActivityResumed(activity: Activity) {
                }

                override fun onActivityPaused(activity: Activity) {
                }

                override fun onActivityStopped(activity: Activity) {
                    frameView = null
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                }

                override fun onActivityDestroyed(activity: Activity) {}

            })
        }

        frameCallback = Choreographer.FrameCallback { frameTimeNanos: Long ->
            if (lastFrameTimeNanos != 0L) {
                val frameTimeMillis = (frameTimeNanos - lastFrameTimeNanos) / 1000000
                //val frameRate = 1000f / frameTimeMillis
                frameCount++
                val elapsedTime = SystemClock.elapsedRealtime() - monitoringStartTime
                if (elapsedTime >= MONITOR_INTERVAL) {
                    val averageFrameRate = frameCount / (elapsedTime / 1000f)
                    //Log.d(TAG, "Average Frame Rate in the last minute: " + averageFrameRate + " FPS");
                    (frameView as? TextView)?.text = String.format("%.1f FPS",averageFrameRate)
                    frameCount = 0
                    monitoringStartTime = SystemClock.elapsedRealtime()
                }
            }
            lastFrameTimeNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stopMonitoring() {
        if (frameCallback != null) {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        }
        lastFrameTimeNanos = 0
        frameCount = 0
        monitoringStartTime = 0
    }
}