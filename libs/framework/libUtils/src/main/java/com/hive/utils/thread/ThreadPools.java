// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 辅助线程池
 * Created by gzg on 2015/12/19.
 */
public class ThreadPools {
    private static final String TAG_NAME = "kg_t_pools";
    private ExecutorService mBackgroundExecutorService;

    private ThreadPools() {
        PriorityThreadFactory priorityThreadFactory = new PriorityThreadFactory(android.os.Process.THREAD_PRIORITY_BACKGROUND, TAG_NAME);

        mBackgroundExecutorService = new ThreadPoolExecutor(4, 8,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), priorityThreadFactory);
    }

    private static class SingleHolder {
        static ThreadPools threadPools = new ThreadPools();
    }

    public static ThreadPools getInstance() {
        if (null == SingleHolder.threadPools) {
            synchronized (ThreadPools.class) {
                if (null == SingleHolder.threadPools) {
                    SingleHolder.threadPools = new ThreadPools();
                }
            }
        }

        return SingleHolder.threadPools;
    }


    public Future<?> post(Runnable runnable) {
        return mBackgroundExecutorService.submit(runnable);
    }


//    public void exit() {
//        mBackgroundExecutorService.shutdown();
//    }


    public void postDelay(final Runnable businessRunnable, long delay) {
        UIHandlerUtils.getInstance().postDelayed(new Runnable() {
            @Override
            public void run() {

                ThreadPools.getInstance().post(businessRunnable);
            }
        }, delay);
    }
}
