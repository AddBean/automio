// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.interceptor;

import androidx.annotation.NonNull;

import com.hive.utils.debug.DLog;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * 对请求返回解密
 */
public class DecryptResponseInterceptor implements Interceptor {
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
//        if (DLog.isDebug())
//            DLog.v("DecryptResponseInterceptor");
        return chain.proceed(chain.request());
    }
}
