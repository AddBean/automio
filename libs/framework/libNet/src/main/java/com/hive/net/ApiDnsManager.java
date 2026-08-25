// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net;

import android.text.TextUtils;


public class ApiDnsManager {

    public static String sDataUrl;
    public static String sStatisticUrl;
    public static String sOtherUrl;
    public static String sResUrl;


    /**
     * 获取data域名
     *
     * @return
     */
    public static String getDataDomain() {
        if (TextUtils.isEmpty(sDataUrl))
            return NetConfig.DATA_URL;
        return sDataUrl;
    }

    /**
     * 获取Statistic域名
     *
     * @return
     */
    public static String getStatisticDomain() {
        if (TextUtils.isEmpty(sStatisticUrl))
            return NetConfig.STATISTIC_URL;
        return sStatisticUrl;
    }

    /**
     * 获取Other域名
     *
     * @return
     */
    public static String getOtherDomain() {
        if (TextUtils.isEmpty(sOtherUrl))
            return NetConfig.OTHER_URL;
        return sOtherUrl;
    }

    /**
     * 获取Res域名
     *
     * @return
     */
    public static String getResDomain() {
        if (TextUtils.isEmpty(sResUrl))
            return NetConfig.RES_URL;
        return sResUrl;
    }
}
