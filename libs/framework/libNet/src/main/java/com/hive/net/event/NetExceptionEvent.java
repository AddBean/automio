// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.event;

public class NetExceptionEvent {
    private int code;

    public NetExceptionEvent(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
