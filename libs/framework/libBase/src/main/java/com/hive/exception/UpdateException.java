// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.exception;

import com.hive.utils.GlobalApp;

public class UpdateException extends BaseException {
    public enum Error {
        Download_error,
        Md5_error
    }

    private Error type;

    public UpdateException(Error type) {
        this.type = type;
    }

    @Override
    public String getMessage() {
        switch (type) {
            case Download_error:
                return GlobalApp.getString(com.hive.i8n.R.string.base_download_error);
            case Md5_error:
                return GlobalApp.getString(com.hive.i8n.R.string.base_file_verify_error);
        }
        return super.getMessage();
    }

}
