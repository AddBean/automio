// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net;

import com.hive.net.event.NetExceptionEvent;
import com.hive.net.exception.ErrorCode;
import com.hive.net.exception.NetworkException;
import com.hive.utils.debug.DLog;

import org.greenrobot.eventbus.EventBus;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;


public abstract class OnHttpListener<T> implements Subscriber<T> {
    private Subscription mSubscription = null;

    @Override
    public void onSubscribe(Subscription s) {
        mSubscription = s;
        mSubscription.request(1);

    }

    public abstract void onSuccess(T data) throws Throwable;

    public boolean onFailure(Throwable e) {
        DLog.e(e.getMessage());
        return false;
    }

    @Override
    public void onNext(T t) {
        try {
            onSuccess(t);
        } catch (Throwable e) {
            onFailure(e);
        }
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
        NetworkException exception = NetworkException.parseThrowable(e);
        if (!onFailure(exception)) {
            handleErrorDefault(exception);
        }
    }

    protected void handleErrorDefault(NetworkException exception) {
//        Toast.makeText(GlobalApp.sContext, exception.getMessage(), Toast.LENGTH_SHORT).show();
        if (exception.getCode() == ErrorCode.HTTP.UNAUTHORIZED) {
            EventBus.getDefault().post(new NetExceptionEvent(exception.getCode()));
        }
    }

    @Override
    public void onComplete() {
        if (mSubscription != null)
            mSubscription.cancel();
    }

    public void cancel() {
        if (mSubscription != null)
            mSubscription.cancel();
    }

}