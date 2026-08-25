// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.event;

public class TabEvent {
    public static final int INDEX_SWITCH = 0;

    public int type;
    public Object obj;

    public TabEvent(int type) {
        this.type = type;
    }
}
