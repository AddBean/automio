// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.exception;

import android.content.Context;

import com.google.gson.JsonParseException;
import com.hive.compon.R;

import org.json.JSONException;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.text.ParseException;

import retrofit2.HttpException;

public class NetworkException extends Throwable {

    private int code = 0;
    private Context context;

    public NetworkException(String detailMessage) {
        super(detailMessage);
    }

    public NetworkException(int code, String detailMessage) {
        super(detailMessage);
        this.code = code;
    }

    public NetworkException(Throwable cause) {
        super(cause);
    }

    public NetworkException(Context context, Throwable cause) {
        super(cause);
        this.context = context;
    }


    public String getDetailMessage() {
        return super.getMessage();
    }

    @Override
    public String getMessage() {
        if (context == null) {
            return super.getMessage();
        }
        
        switch (code) {
            case ErrorCode.NETWORK.NETWORK_ERROR:
                return context.getString(com.hive.i8n.R.string.network_error_retry);
            case ErrorCode.NETWORK.PARSE_ERROR:
                return context.getString(com.hive.i8n.R.string.network_parse_error);
            case ErrorCode.NETWORK.SSL_ERROR:
                return context.getString(com.hive.i8n.R.string.network_ssl_error);
            case ErrorCode.NETWORK.UNKNOWN:
                return context.getString(com.hive.i8n.R.string.network_unknown);
            case ErrorCode.NETWORK.API_ERROR: //如果是api自定义异常，则直接使用NetworkServerException处理；
                return super.getMessage();

            case ErrorCode.HTTP.UNAUTHORIZED:
                return context.getString(com.hive.i8n.R.string.network_unauthorized);
            case ErrorCode.HTTP.FORBIDDEN:
                return context.getString(com.hive.i8n.R.string.network_forbidden);
            case ErrorCode.HTTP.NOT_FOUND:
                return context.getString(com.hive.i8n.R.string.network_not_found);
            case ErrorCode.HTTP.REQUEST_TIMEOUT:
                return context.getString(com.hive.i8n.R.string.network_timeout);
            case ErrorCode.HTTP.GATEWAY_TIMEOUT:
                return context.getString(com.hive.i8n.R.string.network_gateway_timeout);
            case ErrorCode.HTTP.INTERNAL_SERVER_ERROR:
                return context.getString(com.hive.i8n.R.string.network_internal_error);
            case ErrorCode.HTTP.BAD_GATEWAY:
                return context.getString(com.hive.i8n.R.string.network_bad_gateway);
            case ErrorCode.HTTP.SERVICE_UNAVAILABLE:
                return context.getString(com.hive.i8n.R.string.network_service_unavailable);
        }
        return context.getString(com.hive.i8n.R.string.network_default_error);
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }


    public static NetworkException parseThrowable(Throwable e) {
        NetworkException ex = new NetworkException(e);
        if (e instanceof HttpException) {
            HttpException httpException = (HttpException) e;
            ex.setCode(httpException.code());
        } else if (e instanceof UnknownHostException) {
            ex.setCode(ErrorCode.NETWORK.NETWORK_ERROR);
        } else if (e instanceof JsonParseException
                || e instanceof JSONException
                || e instanceof ParseException) {
            ex.setCode(ErrorCode.NETWORK.PARSE_ERROR);
        } else if (e instanceof ConnectException) {
            ex.setCode(ErrorCode.NETWORK.NETWORK_ERROR);
        } else if (e instanceof javax.net.ssl.SSLHandshakeException) {
            ex.setCode(ErrorCode.NETWORK.SSL_ERROR);
        } else if (e instanceof NetworkServerException) {
            ex.setCode(ErrorCode.NETWORK.API_ERROR);
        } else {
            ex.setCode(ErrorCode.NETWORK.UNKNOWN);
        }
        return ex;
    }

    public static NetworkException parseThrowable(Context context, Throwable e) {
        NetworkException ex = new NetworkException(context, e);
        if (e instanceof HttpException) {
            HttpException httpException = (HttpException) e;
            ex.setCode(httpException.code());
        } else if (e instanceof UnknownHostException) {
            ex.setCode(ErrorCode.NETWORK.NETWORK_ERROR);
        } else if (e instanceof JsonParseException
                || e instanceof JSONException
                || e instanceof ParseException) {
            ex.setCode(ErrorCode.NETWORK.PARSE_ERROR);
        } else if (e instanceof ConnectException) {
            ex.setCode(ErrorCode.NETWORK.NETWORK_ERROR);
        } else if (e instanceof javax.net.ssl.SSLHandshakeException) {
            ex.setCode(ErrorCode.NETWORK.SSL_ERROR);
        } else if (e instanceof NetworkServerException) {
            ex.setCode(ErrorCode.NETWORK.API_ERROR);
        } else {
            ex.setCode(ErrorCode.NETWORK.UNKNOWN);
        }
        return ex;
    }
}
