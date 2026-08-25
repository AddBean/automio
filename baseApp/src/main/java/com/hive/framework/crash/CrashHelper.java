// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.framework.crash;

import android.content.Context;

import com.hive.framework.BuildConfig;
import com.hive.net.engineer.EngineerConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeoutException;

/**
 * on 2019-07-30
 */
public class CrashHelper implements Thread.UncaughtExceptionHandler {

    private Context mContext;

    // 系统默认的UncaughtException处理类
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private static volatile CrashHelper instance;

    private CrashHelper() {
    }

    public static CrashHelper getInstance() {
        if (instance == null) {
            synchronized (CrashHelper.class) {
                if (instance == null) {
                    instance = new CrashHelper();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        this.mContext = context;
        this.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        CrashModel crashModel = parseCrash(e);

        if (BuildConfig.DEBUG || EngineerConfig.read().engineerOn) {
            //展示崩溃信息
            CrashDetailActivity.startMe(mContext, crashModel);
        }

        if (t.getName().equals("FinalizerWatchdogDaemon") && e instanceof TimeoutException) {
            //ignore this Exception
        } else {
            if (mDefaultHandler != null) {
                mDefaultHandler.uncaughtException(t, e);
            }
        }
    }

    private CrashModel parseCrash(Throwable ex) {
        CrashModel model = new CrashModel();
        try {
            model.setEx(ex);
            model.setTime(System.currentTimeMillis());
            if (ex.getCause() != null) {
                ex = ex.getCause();
            }
            model.setExceptionMsg(ex.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            pw.flush();
            String exceptionType = ex.getClass().getName();

            if (ex.getStackTrace() != null && ex.getStackTrace().length > 0) {
                StackTraceElement element = ex.getStackTrace()[0];
                model.setLineNumber(element.getLineNumber());
                model.setClassName(element.getClassName());
                model.setFileName(element.getFileName());
                model.setMethodName(element.getMethodName());
                model.setExceptionType(exceptionType);
            }
            model.setFullException(sw.toString());
            pw.close();
            sw.close();
        } catch (Exception e) {
            return model;
        }
        return model;
    }
}
