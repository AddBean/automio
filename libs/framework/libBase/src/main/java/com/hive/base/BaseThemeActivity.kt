// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.hive.utils.darkmode.MiuiDarkModeCompat
import com.hive.utils.debug.DLog

/**
 * Activity动画类型枚举
 */
enum class ActivityAnimationType {
    NONE,       // 无动画
    FADE,       // 淡入淡出
    SLIDE_UP,   // 上下滑动
    SLIDE_RIGHT // 左右滑动
}

/**
 * 基础主题Activity
 * 提供状态栏和导航栏适配，兼容不同手机设备
 */
abstract class BaseThemeActivity : AppCompatActivity() {

    private var isDarkMode: Boolean = true // 将在onCreate中动态检测

    private var animationType: ActivityAnimationType = ActivityAnimationType.SLIDE_RIGHT

    override fun onCreate(savedInstanceState: Bundle?) {
        // MIUI 暗黑模式兼容处理
        MiuiDarkModeCompat.applyDisableDarkMode(this)

        super.onCreate(savedInstanceState)

        setupAnimation()

        setAppThemeResource()

        onCreateView(savedInstanceState)

        setupSystemBars()

        setupWindow()

    }

    private fun setupAnimation(){
        animationType =
            intent.getIntExtra("animationType", ActivityAnimationType.SLIDE_RIGHT.ordinal)
                .let { ordinal ->
                    ActivityAnimationType.values()
                        .getOrElse(ordinal) { ActivityAnimationType.SLIDE_RIGHT }
                }
    }

    abstract fun onCreateView(savedInstanceState: Bundle?)

    private fun setAppThemeResource() {
        if (isDarkMode) {
            setTheme(R.style.Theme_BaseApp) // 设置应用主题
        } else {
            setTheme(R.style.Theme_BaseAppLight) // 设置应用主题
        }
    }


    /**
     * 设置深色模式
     * @param darkMode true-深色模式，false-浅色模式
     */
    fun setDarkMode(darkMode: Boolean) {
        isDarkMode = darkMode
        updateSystemBarsAppearance()
    }

    /**
     * 设置系统栏（状态栏和导航栏）
     */
    protected open fun setupSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // 设置深色主题的状态栏和导航栏样式
        updateSystemBarsAppearance()

        // 确保状态栏文字颜色正确（深色主题下为浅色文字）
        if (isDarkMode) {
            setStatusBarTextColor(true)
        }
    }


    /**
     * 设置fitsSystemWindows适配
     * @param enabled true-内容避开系统栏区域，false-内容可延伸到系统栏区域
     */
    fun setFitsSystemWindows(enabled: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, enabled)
    }


    /**
     * 设置窗口配置
     */
    protected open fun setupWindow() {
        // 设置窗口背景颜色
        window.setBackgroundDrawableResource(
            if (isDarkMode) com.hive.i8n.R.color.app_main_color else com.hive.i8n.R.color.app_main_color_light
        )

        // 设置状态栏颜色
        window.statusBarColor = getStatusBarColor()

        // 设置导航栏颜色
        window.navigationBarColor = getNavigationBarColor()

        // 设置窗口标志
        setupWindowFlags()
    }

    /**
     * 设置窗口标志
     */
    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用新的API
            window.insetsController?.let { controller ->
                if (isDarkMode) {
                    // 深色主题：状态栏文字和图标为浅色
                    controller.setSystemBarsAppearance(
                        0, // 不设置任何浅色标志
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                } else {
                    // 浅色主题：状态栏文字和图标为深色
                    controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                }
            }
        }
    }

    /**
     * 更新系统栏外观
     */
    protected open fun updateSystemBarsAppearance() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (isDarkMode) {
            // 深色主题：状态栏和导航栏使用深色背景，文字和图标为浅色
            windowInsetsController?.isAppearanceLightStatusBars = false
            windowInsetsController?.isAppearanceLightNavigationBars = false
        } else {
            // 浅色主题：状态栏和导航栏使用浅色背景，文字和图标为深色
            windowInsetsController?.isAppearanceLightStatusBars = true
            windowInsetsController?.isAppearanceLightNavigationBars = true
        }

        // 设置状态栏和导航栏颜色
        window.statusBarColor = getStatusBarColor()
        window.navigationBarColor = getNavigationBarColor()

        // 对于Android 11+，使用新的API确保状态栏文字颜色正确
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                if (isDarkMode) {
                    // 深色主题：状态栏文字和图标为浅色
                    controller.setSystemBarsAppearance(
                        0, // 不设置任何浅色标志
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                } else {
                    // 浅色主题：状态栏文字和图标为深色
                    controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                }
            }
        }
    }

    /**
     * 获取状态栏颜色
     */
    protected open fun getStatusBarColor(): Int {
        return if (isDarkMode) {
            ContextCompat.getColor(this, com.hive.i8n.R.color.app_main_color)
        } else {
            ContextCompat.getColor(this, com.hive.i8n.R.color.app_main_color_light)
        }
    }

    /**
     * 获取导航栏颜色
     */
    protected open fun getNavigationBarColor(): Int {
        return if (isDarkMode) {
            ContextCompat.getColor(this, com.hive.i8n.R.color.app_main_color)
        } else {
            ContextCompat.getColor(this, com.hive.i8n.R.color.app_main_color_light)
        }
    }


    /**
     * 设置沉浸式模式
     */
    fun setImmersiveMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        setFitsSystemWindows(false)
    }

    /**
     * 设置系统栏透明
     * @param statusBar 是否设置状态栏透明
     * @param navigationBar 是否设置导航栏透明
     */
    fun setTransparentSystemBars(statusBar: Boolean = true, navigationBar: Boolean = true) {
        if (statusBar) {
            window.statusBarColor = Color.TRANSPARENT
        }
        if (navigationBar) {
            window.navigationBarColor = Color.TRANSPARENT
        }
    }

    /**
     * 设置状态栏文字颜色
     * @param light true-浅色文字（深色主题），false-深色文字（浅色主题）
     */
    fun setStatusBarTextColor(light: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.isAppearanceLightStatusBars = !light
        windowInsetsController?.isAppearanceLightNavigationBars = !light

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                if (light) {
                    // 深色主题：清除浅色标志
                    controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                } else {
                    // 浅色主题：设置浅色标志
                    controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                }
            }
        }

        // 同步更新内部状态
        isDarkMode = light
    }

    /**
     * 检查当前是否为深色模式
     */
    fun isDarkMode(): Boolean {
        return isDarkMode
    }


    override fun finish() {
        super.finish()
        applyExitAnimation(this, animationType)
    }

    companion object {

        /**
         * 应用Activity出场动画
         * 在finish()之前调用此方法
         * @param animationType 动画类型
         */
        fun applyExitAnimation(activity: Activity, animationType: ActivityAnimationType) {
            DLog.d("ActivityAnimation", "Applying exit animation: $animationType")

            when (animationType) {
                ActivityAnimationType.NONE -> {
                    // 完全禁用动画
                    activity.overridePendingTransition(0, 0)
                }

                ActivityAnimationType.FADE -> {
                    activity.overridePendingTransition(R.anim.anim_default_none, R.anim.fade_out)
                }

                ActivityAnimationType.SLIDE_UP -> {
                    activity.overridePendingTransition(R.anim.anim_default_none, R.anim.slide_up_out)
                }

                ActivityAnimationType.SLIDE_RIGHT -> {
                    activity.overridePendingTransition(R.anim.anim_default_none, R.anim.slide_right_out)
                }
            }
        }

        /**
         * 应用Activity启动动画
         * @param animationType 动画类型
         */
        fun applyActivityAnimation(activity: Activity, animationType: ActivityAnimationType) {
            DLog.d("ActivityAnimation", "Applying enter animation: $animationType")

            when (animationType) {
                ActivityAnimationType.NONE -> {
                    // 完全禁用动画
                    activity.overridePendingTransition(0, 0)
                }

                ActivityAnimationType.FADE -> {
                    activity.overridePendingTransition(R.anim.fade_in, R.anim.anim_default_none)
                }

                ActivityAnimationType.SLIDE_UP -> {
                    activity.overridePendingTransition(R.anim.slide_up_in, R.anim.anim_default_none)
                }

                ActivityAnimationType.SLIDE_RIGHT -> {
                    activity.overridePendingTransition(R.anim.slide_right_in, R.anim.anim_default_none)
                }
            }
        }
    }

}