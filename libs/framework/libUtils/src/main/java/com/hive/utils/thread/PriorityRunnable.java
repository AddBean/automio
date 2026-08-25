// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.thread;

/**
 * Created by gangzhiguo
 * on 2018/7/4
 */

public abstract class PriorityRunnable implements Runnable {
    private final int mPriority;

    public PriorityRunnable(int priority) {
        mPriority = priority;
    }

    public int getPriority() {
        return mPriority;
    }
}
