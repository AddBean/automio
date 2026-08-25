// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.interceptor;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.hive.net.engineer.EngineerConfig;
import com.hive.net.ApiClientCreator;
import com.hive.net.ApiDnsManager;
import com.hive.net.NetConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;


/**
 * 负责将域名切换成工程模式
 */
public class NetworkDnsInterceptor implements Interceptor {

    private int mApiType;

    public NetworkDnsInterceptor(int apiType) {
        mApiType = apiType;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
//        if (DLog.isDebug())
//            DLog.v("NetworkDnsInterceptor");
        Request request = chain.request();
        String httpUrl = request.url().toString();
        if (mApiType == ApiClientCreator.API_TYPE_DATA && !TextUtils.isEmpty(ApiDnsManager.sDataUrl) && httpUrl.contains(NetConfig.DATA_URL)) {
            httpUrl = httpUrl.replace(NetConfig.DATA_URL, ApiDnsManager.sDataUrl);
        }
        if (mApiType == ApiClientCreator.API_TYPE_STATISTIC && !TextUtils.isEmpty(ApiDnsManager.sStatisticUrl) && httpUrl.contains(NetConfig.STATISTIC_URL)) {
            httpUrl = httpUrl.replace(NetConfig.STATISTIC_URL, ApiDnsManager.sStatisticUrl);
        }
        if (mApiType == ApiClientCreator.API_TYPE_OTHER && !TextUtils.isEmpty(ApiDnsManager.sOtherUrl) && httpUrl.contains(NetConfig.OTHER_URL)) {
            httpUrl = httpUrl.replace(NetConfig.OTHER_URL, ApiDnsManager.sOtherUrl);
        }
        if (mApiType == ApiClientCreator.API_TYPE_RES && !TextUtils.isEmpty(ApiDnsManager.sResUrl) && httpUrl.contains(NetConfig.RES_URL)) {
            httpUrl = httpUrl.replace(NetConfig.RES_URL, ApiDnsManager.sResUrl);
        }

        if (EngineerConfig.read().engineerOn) {
            if (mApiType == ApiClientCreator.API_TYPE_DATA && httpUrl.contains(ApiDnsManager.getDataDomain())) {
                httpUrl = httpUrl.replace(ApiDnsManager.getDataDomain(), EngineerConfig.read().dataUrl);
            }

            if (mApiType == ApiClientCreator.API_TYPE_STATISTIC && httpUrl.contains(ApiDnsManager.getStatisticDomain())) {
                httpUrl = httpUrl.replace(ApiDnsManager.getStatisticDomain(), EngineerConfig.read().statisticUrl);
            }

            if (mApiType == ApiClientCreator.API_TYPE_OTHER && httpUrl.contains(ApiDnsManager.getOtherDomain())) {
                httpUrl = httpUrl.replace(ApiDnsManager.getOtherDomain(), EngineerConfig.read().otherUrl);
            }

            if (mApiType == ApiClientCreator.API_TYPE_RES && httpUrl.contains(ApiDnsManager.getResDomain())) {
                httpUrl = httpUrl.replace(ApiDnsManager.getResDomain(), EngineerConfig.read().resUrl);
            }
        }
        request = request.newBuilder().url(httpUrl).build();
        return chain.proceed(request);
    }
}
