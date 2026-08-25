// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import okhttp3.Interceptor;

public abstract class IBaseApiClient<D, S, O> {

    private S mApiStatistic;
    private D mApiData;
    private D mApiStreamingData;
    private O mApiOther;

    public D getDataService() {
        return getDataService(null, null);
    }

    public S getStatisticService() {
        return getStatisticService(null, null);
    }

    public O getOtherService() {
        return getOtherService(null, null);
    }

    public D getDataStreamingService(List<Interceptor> interceptors, List<Interceptor> networkInterceptors) {
        if (mApiStreamingData == null) {
            Class<D> rawType = (Class<D>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            mApiStreamingData = ApiClientCreator.buildStreamingApiService(ApiClientCreator.API_TYPE_DATA, ApiDnsManager.getDataDomain(), rawType, interceptors, networkInterceptors);
        }
        return mApiStreamingData;
    }

    public D getDataService(List<Interceptor> interceptors, List<Interceptor> networkInterceptors) {
        if (mApiData == null) {
            Class<D> rawType = (Class<D>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            mApiData = ApiClientCreator.buildApiService(ApiClientCreator.API_TYPE_DATA, ApiDnsManager.getDataDomain(), rawType, interceptors, networkInterceptors);
        }
        return mApiData;
    }

    public S getStatisticService(List<Interceptor> interceptors, List<Interceptor> networkInterceptors) {
        if (mApiStatistic == null) {
            Class<S> rawType = (Class<S>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            mApiStatistic = ApiClientCreator.buildApiService(ApiClientCreator.API_TYPE_STATISTIC, ApiDnsManager.getStatisticDomain(), rawType, interceptors, networkInterceptors);
        }
        return mApiStatistic;
    }

    public O getOtherService(List<Interceptor> interceptors, List<Interceptor> networkInterceptors) {
        if (mApiOther == null) {
            Class<O> rawType = (Class<O>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            mApiOther = ApiClientCreator.buildApiService(ApiClientCreator.API_TYPE_OTHER, ApiDnsManager.getOtherDomain(), rawType, interceptors, networkInterceptors);
        }
        return mApiOther;
    }
}
