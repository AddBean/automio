// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.debug;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hive.utils.BuildConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class DLog {
    private static final int ERROR = 0, INF = 1, VERBOSE = 2, DEBUG = 3, WRANING = 4;
    public static final String TAG = "DLog";
    public static Gson sGson = new Gson();

    public static boolean sEnable = false || BuildConfig.DEBUG;
    private static List<DLogProxyInterface> mProxyList = new ArrayList<>();

    public static void registerProxy(DLogProxyInterface proxyInterface) {
        if (!mProxyList.contains(proxyInterface))
            mProxyList.add(proxyInterface);
    }

    public static void unregisterProxy(DLogProxyInterface proxyInterface) {
        if (mProxyList.contains(proxyInterface))
            mProxyList.remove(proxyInterface);
    }

    public static void debug(Object object) {
        printfObj("", ERROR, object);
    }

    private static String buildHeader() {
        StackTraceElement[] stackList = Thread.currentThread().getStackTrace();
        StackTraceElement stack = stackList[4];
        //查找实际堆栈位置
        if (stack.getClassName().contains(DLog.class.getName()) && BuildConfig.DEBUG) {
            for (int i = stackList.length - 1; i >= 0; i--) {
                stack = stackList[i];
                if (stack.getClassName().contains(DLog.class.getName())) {
                    stack = Thread.currentThread().getStackTrace()[i + 1];
                    break;
                }
            }
        }
        return String.format("%s>%s>%d", new Object[]{stack.getClassName(), stack.getMethodName(), Integer.valueOf(stack.getLineNumber())}) + ":<-->:";
    }

    public static void e(Object obj) {
        printfObj("", ERROR, obj);
    }

    public static void e(Object obj, Exception e) {
        printfObj("", ERROR, obj);
    }

    public static void e(String tag, Object obj) {
        printfObj(tag, ERROR, obj);
    }

    public static void e(String tag, Object obj, Exception e) {
        printfObj(tag, ERROR, obj);
    }

    public static void i(Object obj) {
        printfObj("", INF, obj);
    }

    public static void i(String tag, Object obj) {
        printfObj(tag, INF, obj);
    }

    public static void w(Object obj) {
        printfObj("", WRANING, obj);
    }

    public static void w(String tag, Object obj) {
        printfObj(tag, WRANING, obj);
    }

    public static void w(String tag, Object obj,Exception e) {
        printfObj(tag, WRANING, obj);
    }

    public static void v(Object obj) {
        printfObj("", VERBOSE, obj);
    }

    public static void v(String tag, Object obj) {
        printfObj(tag, VERBOSE, obj);
    }

    public static void d(Object obj) {
        printfObj("", DEBUG, obj);
    }

    public static void d(String tag, Object obj) {
        printfObj(tag, DEBUG, obj);
    }

    private synchronized static void printfObj(String tag, int type, Object obj) {
        if (!DLog.sEnable)
            return;
        if (sGson == null) {
            sGson = new GsonBuilder().create();
        }
        String msg = "";
        if (obj instanceof Throwable) {
            printf(type, tag, getExceptionMsg((Throwable) obj));
        }
        if (obj instanceof String) {
            printf(type, tag, (String) obj);
            return;
        }
        if (obj != null) {
            msg = sGson.toJson(obj);
            printf(type, tag, msg);
        }
    }


    private static void printf(int type, String tag, String msg) {
        String header = buildHeader();
        if (TextUtils.isEmpty(tag)) {
            tag = TAG;
        }
        switch (type) {
            case ERROR:
                Log.e(tag, header + " " + msg);
                if (mProxyList != null)
                    for (DLogProxyInterface proxyInterface : mProxyList)
                        proxyInterface.e(tag, header, msg);
                break;
            case INF:
                Log.i(tag, header + " " + msg);
                if (mProxyList != null)
                    for (DLogProxyInterface proxyInterface : mProxyList)
                        proxyInterface.i(tag, header, msg);
                break;
            case VERBOSE:
                Log.v(tag, header + " " + msg);
                if (mProxyList != null)
                    for (DLogProxyInterface proxyInterface : mProxyList)
                        proxyInterface.v(tag, header, msg);
                break;
            case DEBUG:
                Log.d(tag, header + " " + msg);
                if (mProxyList != null)
                    for (DLogProxyInterface proxyInterface : mProxyList)
                        proxyInterface.d(tag, header, msg);
                break;
            case WRANING:
                Log.w(tag, header + " " + msg);
                if (mProxyList != null)
                    for (DLogProxyInterface proxyInterface : mProxyList)
                        proxyInterface.w(tag, header, msg);
                break;
        }
    }

    private static String getExceptionMsg(Throwable e) {
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            //将出错的栈信息输出到printWriter中
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
        } finally {
            if (sw != null) {
                try {
                    sw.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (pw != null) {
                pw.close();
            }
        }
        return sw.toString();
    }

    public static void setPretty(boolean enable) {
        sGson = enable ? new GsonBuilder().setPrettyPrinting().create() : new GsonBuilder().create();
    }

    public static boolean isDebug() {
        return sEnable;
    }
}