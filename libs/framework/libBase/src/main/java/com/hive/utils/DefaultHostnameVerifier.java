// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

import android.text.TextUtils;

import com.hive.global.GlobalConfig;
import com.hive.utils.utils.CollectionUtil;

import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class DefaultHostnameVerifier implements HostnameVerifier {

    @Override
    public boolean verify(String hostname, SSLSession session) {
        return true;
//        if (TextUtils.isEmpty(hostname)) {
//            return false;
//        } else if (checkHostName(hostname)) {
//            return true;
//        } else {
//            return true;
////            HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
////            return hv.verify(hostname, session);
//        }
    }

    public static boolean checkHostName(String hostName) {
        List<String> urls = GlobalConfig.getInstance().getListObject(GlobalConfig.CONFIG_HTTPS_HOST_VERIFIER, String.class, Arrays.asList(BaseConfig.DATA_URL));
        if (CollectionUtil.empty(urls)) return true;
        for (int i = 0; i < urls.size(); i++) {
            if (urls.get(i).contains(hostName)) {
                return true;
            }
        }
        return false;
    }
}
