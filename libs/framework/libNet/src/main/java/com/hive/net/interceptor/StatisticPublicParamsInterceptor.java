// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.interceptor;

import androidx.annotation.NonNull;

import com.hive.utils.debug.DLog;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * 统计上报的公共参数；
 */
public class StatisticPublicParamsInterceptor implements Interceptor {
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
//        if (DLog.isDebug())
//            DLog.v("StatisticPublicParamsInterceptor");
        return chain.proceed(chain.request());
    }
}
