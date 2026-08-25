// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

import com.hive.net.OnHttpListener;
import com.hive.net.exception.NetworkException;

import java.lang.ref.WeakReference;

public abstract class OnHttpStateListener<T> extends OnHttpListener<T> {
    private WeakReference<Object> mRefObject;
    public final int TYPE_NORMAL = 0, TYPE_REF = 1;
    private int type;

    public OnHttpStateListener(Object refObj) {
        mRefObject = new WeakReference<>(refObj);
        type = TYPE_REF;
    }

    public OnHttpStateListener() {
        type = TYPE_NORMAL;
    }

    @Override
    public void onNext(T t) {
        if (type == TYPE_REF && (mRefObject == null || mRefObject.get() == null)) return;
        try {
            onSuccess(t);
        } catch (Throwable e) {
            onFailure(e);
        }
    }

    @Override
    public void onError(Throwable e) {
        if (type == TYPE_REF && (mRefObject == null || mRefObject.get() == null)) return;
        e.printStackTrace();
        NetworkException exception = NetworkException.parseThrowable(e);
        if (!onFailure(exception)) {
            handleErrorDefault(exception);
        }
    }


}
