// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net;


import androidx.annotation.IntDef;

import com.hive.net.interceptor.BaseParamsInterceptor;
import com.hive.net.interceptor.DataPublicParamsInterceptor;
import com.hive.net.interceptor.EncryptRequestInterceptor;
import com.hive.net.interceptor.LoggingInterceptor;
import com.hive.net.interceptor.NetworkCacheInterceptor;
import com.hive.net.interceptor.NetworkDnsInterceptor;
import com.hive.net.interceptor.StatisticPublicParamsInterceptor;
import com.hive.net.interceptor.TimeoutInterceptor;
import com.hive.utils.utils.GsonWrapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.TimeUnit;


import io.reactivex.annotations.NonNull;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClientCreator {
    private static String TAG = "ApiClientCreator";
    public static final int API_TYPE_DATA = 0;
    public static final int API_TYPE_STATISTIC = API_TYPE_DATA + 1;
    public static final int API_TYPE_OTHER = API_TYPE_STATISTIC + 1;
    public static final int API_TYPE_RES = API_TYPE_OTHER + 1;
    private static Cache sHttpCache;

    public static int GLOBAL_TIMEOUT_SECS = -1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({API_TYPE_DATA, API_TYPE_STATISTIC, API_TYPE_OTHER})
    public @interface ApiTypeDef {
    }

    public static <T> T buildStreamingApiService(@ApiTypeDef int apiType, @NonNull String baseURL, @NonNull Class<T> clazz, List<Interceptor> interceptors, List<Interceptor> networkInterceptors) {
        OkHttpClient okHttpClient = buildOkHttpBuilder(apiType, interceptors, networkInterceptors, true).build();
        return new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create(GsonWrapper.buildGson()))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build()
                .create(clazz);
    }

    public static <T> T buildApiService(@ApiTypeDef int apiType, @NonNull String baseURL, @NonNull Class<T> clazz, List<Interceptor> interceptors, List<Interceptor> networkInterceptors) {
        OkHttpClient okHttpClient = buildOkHttpBuilder(apiType, interceptors, networkInterceptors, false).build();
        return new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create(GsonWrapper.buildGson()))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build()
                .create(clazz);
    }


    public static OkHttpClient.Builder buildOkHttpBuilder(@ApiTypeDef int apiType, List<Interceptor> interceptors, List<Interceptor> networkInterceptors, boolean isStreaming) {

        int timeout = 10;
        if (apiType == API_TYPE_DATA) {
            timeout = 12;
        } else if (apiType == API_TYPE_STATISTIC) {
            timeout = 40;
        }
        if (GLOBAL_TIMEOUT_SECS > 0) {
            timeout = GLOBAL_TIMEOUT_SECS;
        }
        sHttpCache = CacheManager.newHttpCache();

        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .cookieJar(new CookiesManager())
                .cache(sHttpCache)
                .readTimeout(timeout, TimeUnit.SECONDS);
        okHttpBuilder.addInterceptor(new NetworkDnsInterceptor(apiType));//域名处理
        okHttpBuilder.addInterceptor(new BaseParamsInterceptor());//基本参数
        okHttpBuilder.addInterceptor(new TimeoutInterceptor());//超时处理
        if (!isStreaming) {
            okHttpBuilder.addInterceptor(new LoggingInterceptor());//日志；
        }
        if (interceptors != null) {
            for (Interceptor inter : interceptors) {
                okHttpBuilder.addInterceptor(inter);//自定义拦截器
            }
        }
        if (networkInterceptors != null) {
            for (Interceptor inter : networkInterceptors) {
                okHttpBuilder.addNetworkInterceptor(inter);//自定义拦截器
            }
        }
        if (!isStreaming) {
            okHttpBuilder.addNetworkInterceptor(new NetworkCacheInterceptor());//缓存处理
        }
        switch (apiType) {
            case API_TYPE_DATA:
                okHttpBuilder.addInterceptor(new DataPublicParamsInterceptor());
                break;
            case API_TYPE_STATISTIC:
                okHttpBuilder.addInterceptor(new StatisticPublicParamsInterceptor());
                break;
            case API_TYPE_OTHER:
            default:
                break;
        }
        okHttpBuilder.addInterceptor(new EncryptRequestInterceptor());//加密服务
        return okHttpBuilder;
    }

    public static Cache getHttpCache() {
        return sHttpCache;
    }
}
