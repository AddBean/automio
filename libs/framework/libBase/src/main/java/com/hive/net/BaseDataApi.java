// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net;

import com.hive.annotation.NotProguard;
import com.hive.global.GlobalConfigModel;
import com.hive.net.resp.VersionInfoResp;

import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;
@NotProguard
public interface BaseDataApi {
    @Headers({"Content-Type: application/json;charset=UTF-8", "Accept: application/json"})
    @GET
    Flowable<ResponseBody> getList(@Url String url, @HeaderMap Map<String, String> headerMap, @QueryMap Map<String, String> maps);

    @Headers({"Content-Type: application/json;charset=UTF-8", "Accept: application/json"})
    @GET
    Observable<ResponseBody> get(@Url String url, @HeaderMap Map<String, String> headerMap, @QueryMap Map<String, String> maps);

    @Headers({"Content-Type: application/json;charset=UTF-8", "Accept: application/json"})
    @GET
    Flowable<GlobalConfigModel> getGlobalConfig(@Url String url);

    @Headers({"Content-Type: application/json;charset=UTF-8", "Accept: application/json"})
    @GET
    Flowable<ResponseBody> getRepluginConfig(@Url String url);

}
