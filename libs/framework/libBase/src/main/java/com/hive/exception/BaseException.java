// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.exception;

public class BaseException extends Throwable {

    public BaseException() {
    }

    public BaseException(String message) {
        super(message);
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public BaseException(Throwable cause) {
        super(cause);
    }

}
