// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.thread;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

/**
 * handler 助手
 * Created by kuaigeng01 on 2018/1/6.
 */
public class UIHandlerUtils extends Handler {

    private UIHandlerUtils() {
        super(Looper.getMainLooper());
    }

    private static UIHandlerUtils instance;

    public static UIHandlerUtils getInstance() {
        if (null == instance) {
            synchronized (UIHandlerUtils.class) {
                if (null == instance) {
                    instance = new UIHandlerUtils();
                }
            }
        }

        return instance;
    }

    public static void runUI(Runnable runnable) {
        getInstance().executeInMainThread(runnable);
    }


    /**
     * 执行在主线程
     *
     * @param runnable 工作流
     * @return ture 已经在主线程 or false
     */
    public final boolean executeInMainThread(@NonNull Runnable runnable) {
        if (null == runnable) {
            return false;
        }

        if (isOnMainThread()) {

            runnable.run();
            return true;
        } else {

            post(runnable);
            return false;
        }
    }

    /**
     * 执行在主线程
     *
     * @param runnable 工作流
     * @return ture 已经在主线程 or false
     */
    public final boolean executeInMainThread(@NonNull Runnable runnable, long delay) {
        if (null == runnable) {
            return false;
        }

        if (isOnMainThread()) {
            postDelayed(runnable, delay);
            return true;
        } else {

            postDelayed(runnable, delay);
            return false;
        }
    }

    public static boolean isOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

}
