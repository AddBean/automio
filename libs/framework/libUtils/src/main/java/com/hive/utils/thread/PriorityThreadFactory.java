// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.thread;

import android.os.Process;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.util.concurrent.ThreadFactory;

import com.hive.utils.debug.DLog;

public class PriorityThreadFactory implements ThreadFactory {

    private final int mThreadPriority;
    private final String mThreadName;

    public PriorityThreadFactory(int threadPriority, String name) {
        this.mThreadPriority = threadPriority;
        this.mThreadName = TextUtils.isEmpty(name) ? "kgThread" : name;
    }

    @Override
    public Thread newThread(@NonNull final Runnable runnable) {

        final int priority = runnable instanceof PriorityRunnable ? ((PriorityRunnable) (runnable)).getPriority() : mThreadPriority;

        Runnable wrapperRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Process.setThreadPriority(priority);
                    runnable.run();
                } catch (Throwable t) {
                    t.printStackTrace();

                    if (DLog.isDebug()) {
                        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), t);
                    }
                }
            }
        };

        return new Thread(wrapperRunnable, mThreadName);
    }

}
