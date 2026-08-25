// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.statusbar;

import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 适配小米
 */

class StatusBarCompatFlavorRom {

    interface ILightStatusBar {
        void setLightStatusBar(Window window, boolean lightStatusBar);
    }

    static final ILightStatusBar IMPL;

    static {
        if (MIUILightStatusBarImpl.isMe()) {
            IMPL = new MIUILightStatusBarImpl();
        } else if (MeizuLightStatusBarImpl.isMe()) {
            IMPL = new MeizuLightStatusBarImpl();
        } else {
            IMPL = new ILightStatusBar() {
                @Override
                public void setLightStatusBar(Window window, boolean lightStatusBar) {
                }
            };
        }
    }

    static void setLightStatusBar(Window window, boolean lightStatusBar) {
        IMPL.setLightStatusBar(window, lightStatusBar);
    }

    /**
     * https://www.zhihu.com/question/22102139
     */
    static class MIUILightStatusBarImpl implements ILightStatusBar {
        private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
        private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
        private static final String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";

        static boolean isMe() {
            try {
                final BuildProperties prop = BuildProperties.newInstance();
                return prop.getProperty(KEY_MIUI_VERSION_CODE, null) != null
                        || prop.getProperty(KEY_MIUI_VERSION_NAME, null) != null
                        | prop.getProperty(KEY_MIUI_INTERNAL_STORAGE, null) != null;
            } catch (final IOException e) {
                        return false;
            }
        }

        public void setLightStatusBar(Window window, boolean lightStatusBar) {
            MIUISetStatusBarLightMode(window,lightStatusBar);

//            Class<? extends Window> clazz = window.getClass();
//            try {
//                Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
//                Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
//                int darkModeFlag = field.getInt(layoutParams);
//                Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
//                extraFlagField.invoke(window, lightStatusBar ? darkModeFlag : 0, darkModeFlag);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
    }

    static class MeizuLightStatusBarImpl implements ILightStatusBar {
        static boolean isMe() {
            final Method method;
            try {
                method = Build.class.getMethod("hasSmartBar");
                return method != null;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            return false;
        }

        public void setLightStatusBar(Window window, boolean lightStatusBar) {
            FlymeSetStatusBarLightMode(window,lightStatusBar);
//            WindowManager.LayoutParams params = window.getAttributes();
//            try {
//                Field darkFlag = WindowManager.LayoutParams.class.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
//                Field meizuFlags = WindowManager.LayoutParams.class.getDeclaredField("meizuFlags");
//                darkFlag.setAccessible(true);
//                meizuFlags.setAccessible(true);
//                int bit = darkFlag.getInt(null);
//                int value = meizuFlags.getInt(params);
//                if (lightStatusBar) {
//                    value |= bit;
//                } else {
//                    value &= ~bit;
//                }
//                meizuFlags.setInt(params, value);
//                window.setAttributes(params);
//                darkFlag.setAccessible(false);
//                meizuFlags.setAccessible(false);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
    }


    /**
     * 设置状态栏图标为深色和魅族特定的文字风格，Flyme4.0以上
     * 可以用来判断是否为Flyme用户
     * @param window 需要设置的窗口
     * @param dark 是否把状态栏字体及图标颜色设置为深色
     * @return  boolean 成功执行返回true
     *
     */
    public static boolean FlymeSetStatusBarLightMode(Window window, boolean dark) {
        boolean result = false;
        if (window != null) {
            try {
                WindowManager.LayoutParams lp = window.getAttributes();
                Field darkFlag = WindowManager.LayoutParams.class
                        .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
                Field meizuFlags = WindowManager.LayoutParams.class
                        .getDeclaredField("meizuFlags");
                darkFlag.setAccessible(true);
                meizuFlags.setAccessible(true);
                int bit = darkFlag.getInt(null);
                int value = meizuFlags.getInt(lp);
                if (dark) {
                    value |= bit;
                } else {
                    value &= ~bit;
                }
                meizuFlags.setInt(lp, value);
                window.setAttributes(lp);
                result = true;
            } catch (Exception e) {

            }
        }
        return result;
    }

    /**
     * 设置状态栏字体图标为深色，需要MIUIV6以上
     * @param window 需要设置的窗口
     * @param dark 是否把状态栏字体及图标颜色设置为深色
     * @return  boolean 成功执行返回true
     *
     */
    public static boolean MIUISetStatusBarLightMode(Window window, boolean dark) {
        boolean result = false;
        if (window != null) {
            Class clazz = window.getClass();
            try {
                int darkModeFlag = 0;
                Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
                Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
                darkModeFlag = field.getInt(layoutParams);
                Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
                if(dark){
                    extraFlagField.invoke(window,darkModeFlag,darkModeFlag);//状态栏透明且黑色字体
                }else{
                    extraFlagField.invoke(window, 0, darkModeFlag);//清除黑色字体
                }
                result=true;
            }catch (Exception e){

            }
        }
        return result;
    }
}
