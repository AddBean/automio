// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net;

public class BaseApiService extends IBaseApiClient<BaseDataApi, BaseDataApi, BaseDataApi> {

    private BaseApiService() {
    }

    private static class SingleHolder {
        static BaseApiService instance = new BaseApiService();
    }

    public static BaseApiService getInstance() {
        if (null == SingleHolder.instance) {
            synchronized (IBaseApiClient.class) {
                if (null == SingleHolder.instance) {
                    SingleHolder.instance = new BaseApiService();
                }
            }
        }
        return SingleHolder.instance;
    }


    public static BaseDataApi data() {
        return getInstance().getDataService();
    }
}
