// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.debug;

public interface DLogProxyInterface {
    void e(String tag, String header, String msg);

    void i(String tag, String header, String msg);

    void v(String tag, String header, String msg);

    void d(String tag, String header, String msg);

    void w(String tag, String header, String msg);
}
