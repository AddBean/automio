// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.utils;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

public class WorkHandler extends Handler {
    private WeakReference<IWorkHandler> mRef;

    public WorkHandler(IWorkHandler IWorkHandler) {
        this.mRef = new WeakReference<>(IWorkHandler);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if (mRef.get() == null) return;
        mRef.get().handleMessage(msg);
    }

    public interface IWorkHandler {
        void handleMessage(Message msg);
    }
}