// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.text.TextUtils;

import com.hive.utils.io.IoUtil;

import java.io.FileInputStream;
import java.util.Iterator;
import java.util.List;


/**
 *
 */
public final class ProcessUtils {
    private static String sCurrentProcessName = "";

    /**
     * 判断当前进程是否是UI进程
     *
     * @param context
     * @return
     */
    public static boolean isMainProcess(Context context) {
        return TextUtils.equals(context.getPackageName(), getCurrentProcessName(context));
    }

    /**
     * 判断当前进程是否是UI进程
     *
     * @param context
     * @param sCurrentProcessName
     * @return
     */
    public static boolean isMainProcess(Context context, String sCurrentProcessName) {
        return TextUtils.equals(context.getPackageName(), sCurrentProcessName);
    }

    /**
     * 获取当前进程名
     *
     * @param context 如果为空默认读取（/proc/self/cmdline）获取当前进程名
     * @return
     * @since 1.0.0
     */
    public static String getCurrentProcessName(Context context) {
        if (TextUtils.isEmpty(sCurrentProcessName)) {
            if (TextUtils.isEmpty((sCurrentProcessName = getCurrentProcessNameByNormal(context)))) {
                sCurrentProcessName = getCurrentProcessNameByCmd();
            }
        }
        return sCurrentProcessName;
    }

    ///////////////////////////////////////////////////////////////////////////
    // inner method
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @param context
     * @return
     */
    private static String getCurrentProcessNameByNormal(Context context) {
        if (context != null) {
            try {
                ActivityManager e = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                List infos = e.getRunningAppProcesses();
                Iterator iterator = infos.iterator();
                ActivityManager.RunningAppProcessInfo info;
                do {
                    if (!iterator.hasNext()) {
                        return null;
                    }
                    info = (ActivityManager.RunningAppProcessInfo) iterator.next();
                } while (info.pid != android.os.Process.myPid());
                return info.processName;
            } catch (Exception e) {
                //ignore
            }
        }
        return null;
    }

    private static String getCurrentProcessNameByCmd() {
        FileInputStream in = null;
        try {
            in = new FileInputStream("/proc/self/cmdline");
            byte[] buffer = IoUtil.read(in, 256);
            return new String(buffer, "UTF-8");
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            IoUtil.closeQuietly(in);
        }
        return null;
    }
}
