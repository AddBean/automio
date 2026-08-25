// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.darkmode;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.hive.utils.permission.RomUtils;

import java.lang.reflect.Method;

/**
 * MIUI 暗黑模式兼容处理
 * 解决 MIUI 系统强制暗黑模式不遵循 Android 标准 API 的问题
 *
 * 使用方式：
 * 1. Application.attachBaseContext 中调用 {@link #disableForceDarkMode(Context)}
 * 2. Activity.onCreate 中调用 {@link #applyDisableDarkMode(Activity)}
 */
public class MiuiDarkModeCompat {

    private static final String TAG = "MiuiDarkModeCompat";

    /**
     * MIUI 暗黑模式相关系统属性
     */
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    private static final String KEY_MIUI_DARK_MODE = "ro.miui.ui.darkmode";

    private MiuiDarkModeCompat() {
    }

    /**
     * 检测是否是 MIUI 设备
     */
    public static boolean isMiuiDevice() {
        return RomUtils.isMiuiRom();
    }

    /**
     * 获取 MIUI 版本号
     *
     * @return MIUI 版本号，如 12、13、14 等，非 MIUI 设备返回 -1
     */
    public static int getMiuiVersion() {
        return RomUtils.getMiuiVersion();
    }

    /**
     * 检测 MIUI 系统是否开启了暗黑模式
     * 注意：这检测的是系统级暗黑模式开关，不是应用内的配置
     *
     * @param context Context
     * @return true 表示系统开启了暗黑模式
     */
    public static boolean isMiuiDarkModeEnabled(@NonNull Context context) {
        // 方法1：通过系统属性检测（MIUI 特有）
        String darkModeProp = RomUtils.getSystemProperty(KEY_MIUI_DARK_MODE);
        if (!TextUtils.isEmpty(darkModeProp)) {
            return "1".equals(darkModeProp) || "true".equalsIgnoreCase(darkModeProp);
        }

        // 方法2：通过 Configuration 检测（Android 标准方式）
        int currentNightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * 禁用 MIUI 强制暗黑模式
     * 应在 Application.attachBaseContext 中调用
     *
     * @param context Context
     */
    public static void disableForceDarkMode(@NonNull Context context) {
        // 标准方式：禁用 Android 强制暗黑模式
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // MIUI 特殊处理
        if (isMiuiDevice()) {
            tryDisableMiuiForceDarkMode(context);
        }
    }

    /**
     * 尝试通过反射禁用 MIUI 强制暗黑模式
     */
    private static void tryDisableMiuiForceDarkMode(@NonNull Context context) {
        try {
            // 尝试修改 Configuration 中的 uiMode
            Configuration config = context.getResources().getConfiguration();
            int oldUiMode = config.uiMode;

            // 清除 NIGHT_MASK 并设置为 NIGHT_NO
            config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                    | Configuration.UI_MODE_NIGHT_NO;

            // 只有在配置确实发生变化时才更新
            if (config.uiMode != oldUiMode) {
                context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
                Log.d(TAG, "MIUI dark mode disabled via Configuration");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to disable MIUI dark mode: " + e.getMessage());
        }
    }

    /**
     * 在 Activity 中应用禁用暗黑模式的设置
     * 应在 Activity.onCreate 的 super.onCreate 之前调用
     *
     * @param activity Activity
     */
    public static void applyDisableDarkMode(@NonNull Activity activity) {
        if (isMiuiDevice()) {
            applyMiuiDarkModeWorkaround(activity);
        }
    }

    /**
     * MIUI 暗黑模式 Workaround
     * 通过反射设置 Window 相关参数
     */
    private static void applyMiuiDarkModeWorkaround(@NonNull Activity activity) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }

        try {
            // 尝试通过 MIUI 特有的 Window 方法禁用暗黑模式
            // MIUI 12+ 可能支持的方法
            Class<?> miuiWindowClass = Class.forName("android.view.MiuiWindow");

            // 尝试调用 setDisableForceDark 方法（如果存在）
            try {
                Method setDisableForceDark = miuiWindowClass.getDeclaredMethod(
                        "setDisableForceDark", boolean.class);
                setDisableForceDark.setAccessible(true);
                setDisableForceDark.invoke(window, true);
                Log.d(TAG, "MIUI force dark disabled via MiuiWindow.setDisableForceDark");
            } catch (Exception e) {
                // 方法不存在或调用失败，尝试其他方式
                Log.d(TAG, "setDisableForceDark not available: " + e.getMessage());
            }

        } catch (ClassNotFoundException e) {
            // MiuiWindow 类不存在，尝试其他方式
            Log.d(TAG, "MiuiWindow class not found, trying alternative method");
        }

        // 额外的保险措施：通过 WindowInsetsController 设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                activity.getWindow().getInsetsController().setSystemBarsAppearance(
                        0,
                        0
                );
            } catch (Exception e) {
                Log.d(TAG, "Failed to set insets controller: " + e.getMessage());
            }
        }

        // 确保 Configuration 正确
        try {
            Configuration config = activity.getResources().getConfiguration();
            config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                    | Configuration.UI_MODE_NIGHT_NO;
            activity.getResources().updateConfiguration(config, activity.getResources().getDisplayMetrics());
        } catch (Exception e) {
            Log.e(TAG, "Failed to update configuration: " + e.getMessage());
        }
    }

    /**
     * 获取 MIUI 暗黑模式状态的描述信息（用于调试）
     *
     * @param context Context
     * @return 描述信息
     */
    @NonNull
    public static String getDarkModeDebugInfo(@NonNull Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("MIUI Device: ").append(isMiuiDevice()).append("\n");
        sb.append("MIUI Version: ").append(getMiuiVersion()).append("\n");
        sb.append("MIUI Dark Mode Enabled: ").append(isMiuiDarkModeEnabled(context)).append("\n");

        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        sb.append("Config Night Mode: ");
        switch (nightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                sb.append("NO");
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                sb.append("YES");
                break;
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                sb.append("UNDEFINED");
                break;
            default:
                sb.append(nightMode);
        }
        return sb.toString();
    }
}