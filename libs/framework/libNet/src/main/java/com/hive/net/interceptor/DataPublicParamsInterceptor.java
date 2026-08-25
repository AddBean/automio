// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.interceptor;

import androidx.annotation.NonNull;

import com.hive.utils.debug.DLog;
import com.hive.utils.utils.GsonHelper;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 数据请求的公共参数;
 */
public class DataPublicParamsInterceptor implements Interceptor {

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        if (DLog.isDebug())
            DLog.v("DataPublicParamsInterceptor");
        if (BaseParamsMap.get() == null)
            return chain.proceed(chain.request());
        Request.Builder builder = chain.request().newBuilder();
        Map<String, Object> map = BaseStatisticsParamsUtils.getInstance().get();
        if (map != null) {
           String json= GsonHelper.getInstance().toJson(map);
            if (DLog.isDebug())
                DLog.v("BaseParamsInterceptor publicParams=" + json);
            builder.addHeader("publicParams", GsonHelper.getInstance().toJson(map));
        }
        return chain.proceed(builder.build());
    }

}
