// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.image;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * 拦截捕获所有错误，避免非IOException造成 crash
 * Created by gangzhiguo
 * on 2018/6/29
 */
public class OkHttpExceptionInterceptor implements Interceptor {

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        try {
            return chain.proceed(chain.request());
        } catch (Throwable e) {
            if (e instanceof IOException) {
                throw e;
            } else {
                throw new IOException(e);
            }
        }
    }
}
