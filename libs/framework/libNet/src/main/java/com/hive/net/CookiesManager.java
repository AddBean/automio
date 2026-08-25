// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net;

import com.hive.utils.debug.DLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class CookiesManager implements CookieJar {

    private HashMap<HttpUrl, List<Cookie>> cookieStore = new HashMap<>();

    private HttpUrl url;

    @Override
    public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
        if (httpUrl == null) return;
        cookieStore.put(httpUrl, list);
        url = httpUrl;
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl httpUrl) {
        if (cookieStore == null) return new ArrayList<>();
        List<Cookie> cookies = cookieStore.get(url);
        if (cookies == null || cookies.isEmpty()) return new ArrayList<>();
        DLog.e("CookiesManager", cookies.get(0).value());
        return cookies != null ? cookies : new ArrayList<Cookie>();
    }
}