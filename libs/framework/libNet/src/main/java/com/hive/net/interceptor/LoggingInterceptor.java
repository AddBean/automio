// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.interceptor;

import androidx.annotation.NonNull;

import com.hive.utils.debug.DLog;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * 日志拦截器
 */
public class LoggingInterceptor implements Interceptor {

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
//        if (DLog.isDebug())
//            DLog.v("LoggingInterceptor");
        Request request = chain.request();
        long startNs = System.nanoTime();
        StringBuilder builder = new StringBuilder();
        Charset charset = Charset.defaultCharset();
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        if (DLog.isDebug()) {
            builder.append("\nRequest:\n");
            builder.append("\turl:" + request.method() + " " + request.url().url().toString() + "\n");
            builder.append("\theader:");
            Headers headers = request.headers();
            for (String name : headers.names()) {
                builder.append(name + "=" + headers.get(name) + ",");
            }

            if (request.body() != null) {
                Buffer buffer = new Buffer();
                request.body().writeTo(buffer);
                MediaType contentType = request.body().contentType();
                if (contentType != null) {
                    String body = buffer.readString(contentType.charset(charset));
                    builder.append("\n\tbody:");
                    builder.append(body);
                }
            }
            DLog.d(builder.toString());
        }
        Response response = chain.proceed(request);
        if (DLog.isDebug()) {
            ResponseBody responseBody = response.body();
            if (responseBody.source() != null) {
                BufferedSource source = responseBody.source();
                source.request(Long.MAX_VALUE); // Buffer the entire body.
                Buffer buffer = source.buffer();
                MediaType contentType = responseBody.contentType();
                if (contentType != null) {
                    try {
                        charset = contentType.charset(charset);
                    } catch (UnsupportedCharsetException e) {
                        e.printStackTrace();
                    }
                }
                String rBody = buffer.clone().readString(charset);
                builder.append("\nResponse:");
                builder.append("\n\tstatus:" + response.code() + " " + response.message());
                builder.append("\n\tuseTime:" + tookMs + "ms");
                builder.append("\n\tbody:" + rBody);
                DLog.d(builder.toString());
            }
        }
        return response;
    }

}