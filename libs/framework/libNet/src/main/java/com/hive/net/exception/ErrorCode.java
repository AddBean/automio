// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.exception;

public class ErrorCode {

    /**
     * API自定义异常
     */
    public static class API{
        /**
         * 成功
         */
        public static final int SUCCESS = 200;

        /**
         * 服务器处理失败
         */
        public static final int EXCEPTION = 500;

        /**
         * 权限问题
         */
        public static final int UNAUTHORIZED = 400;
    }

    /**
     * 自定义异常类型
     */
    public static class NETWORK {
        /**
         * 未知错误
         */
        public static final int UNKNOWN = 1000;
        /**
         * 解析错误
         */
        public static final int PARSE_ERROR = 1001;
        /**
         * 网络错误
         */
        public static final int NETWORK_ERROR = 1002;

        /**
         * 证书出错
         */
        public static final int SSL_ERROR = 1005;

        /**
         * API协议出错
         */
        public static final int API_ERROR = 1003;
    }

    /**
     * HTTP错误；
     */
    public static class HTTP {
        public static final int UNAUTHORIZED = 401;
        public static final int FORBIDDEN = 403;
        public static final int NOT_FOUND = 404;
        public static final int REQUEST_TIMEOUT = 408;
        public static final int INTERNAL_SERVER_ERROR = 500;
        public static final int BAD_GATEWAY = 502;
        public static final int SERVICE_UNAVAILABLE = 503;
        public static final int GATEWAY_TIMEOUT = 504;
    }
}
