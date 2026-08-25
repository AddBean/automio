// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.interceptor;

import android.text.TextUtils;

import com.hive.net.CacheManager;
import com.hive.net.ServerTimeHelper;
import com.hive.utils.GlobalApp;
import com.hive.utils.debug.DLog;
import com.hive.utils.net.NetworkUtils;
import com.hive.utils.utils.StringUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 负责拦截缓存
 * 当Cache-Control有配置时，遵从以下缓存策略：1，有网时遵从其缓存配置；2，无网时强制走缓存,并且将缓存时间设置为最大。
 * 当Cache-Control无配置时，不管是否之前有缓存强制走网络；
 */
public class NetworkCacheInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        CacheControl cacheControl = request.cacheControl();
        if (DLog.isDebug())
            DLog.v("cache:" + cacheControl.toString());

        if (cacheControl == null || TextUtils.isEmpty(cacheControl.toString())) {
            cacheControl = CacheControl.FORCE_NETWORK;
            request = request.newBuilder().cacheControl(cacheControl).build();
            Response response = chain.proceed(request);
            try {
                String respContent = StringUtils.getServerContent(response);
                if (!TextUtils.isEmpty(respContent)) {
                    JSONObject jsonObject = new JSONObject(respContent);
                    long serverTime = jsonObject.optLong("_t");
                    if (serverTime > 0) {
                        ServerTimeHelper.updateServerTime(serverTime);
                        if (DLog.isDebug()) {
                            DLog.e("DataPublicParamsInterceptor", "current server time is " + new Date(serverTime).toString());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }

        boolean connected = NetworkUtils.isNetworkAvailabe(GlobalApp.sContext);
        if (!connected) {
            //如果没有网络,从缓存获取数据
            request = request.newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .build();
        }
        Response response = chain.proceed(request);
        if (connected) {
            Response.Builder builder = response.newBuilder()
                    .removeHeader("Pragma");
            if (response.code() == 200 || response.code() == 201) {
                if (getBusinessCode(response) == 200) {
                    builder.header("Cache-Control", cacheControl.toString());
                }
            }
            return builder.build();
        } else {
            return response.newBuilder()
                    .header("Cache-Control", "public, max-age=" + Integer.MAX_VALUE)
                    .removeHeader("Pragma")
                    .build();
        }

    }

    private int getBusinessCode(Response response) {
        try {
            String respContent = StringUtils.getServerContent(response);
            JSONObject jsonObject = new JSONObject(respContent);
            int code = jsonObject.optInt("code");
            return code;
        } catch (Exception e) {
            return 200;
        }
    }
}
