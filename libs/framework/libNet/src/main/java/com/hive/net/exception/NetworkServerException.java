// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.exception;

import com.hive.utils.GlobalApp;

public class NetworkServerException extends Throwable {

    private int code =0;

    public NetworkServerException(String detailMessage) {
        super(detailMessage);
    }

    public NetworkServerException(int code, String detailMessage) {
        super(detailMessage);
        this.code =code;
    }

    public NetworkServerException(Throwable cause, int mCode) {
        super(cause);
        this.code = mCode;

    }

    @Override
    public String getMessage() {
        return GlobalApp.getString(com.hive.i8n.R.string.net_api_service_error);
    }

    public int getCode() {
        return code;
    }


}
