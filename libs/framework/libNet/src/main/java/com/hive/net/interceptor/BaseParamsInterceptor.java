// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.interceptor;

import androidx.annotation.NonNull;

import com.hive.utils.debug.DLog;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 所有请求的公共参数
 */
public class BaseParamsInterceptor implements Interceptor {

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        if (DLog.isDebug())
            DLog.v("BaseParamsInterceptor");
        if (BaseParamsMap.get() == null)
            return chain.proceed(chain.request());
        Request.Builder builder = chain.request().newBuilder();
        for (String key : BaseParamsMap.get().keySet()) {
            if (DLog.isDebug())
                DLog.v("BaseParamsInterceptor header key=" + key + " value=" + BaseParamsMap.get(key));
            builder.addHeader(key, BaseParamsMap.get(key));
        }
        return chain.proceed(builder.build());
    }
}