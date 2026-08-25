// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

public interface CommomListener {
    interface Callback {
        void onEvent(int event, Object object);
    }

    interface Callback2 {
        boolean onEvent(Object object);
    }
}
