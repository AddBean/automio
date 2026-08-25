// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.global

object GlobalSaveTools {

    fun putString(key: String, value: String) {
        MMKVTools.getInstance().putString(key, value)
    }

    fun getString(key: String): String {
        return MMKVTools.getInstance().getString(key, "")
    }

    fun hasMarked(key: String): Boolean {
        return MMKVTools.getInstance().getBoolean(key, false)
    }

    fun mark(key: String) {
        MMKVTools.getInstance().putBoolean(key, true)
    }

    fun unmark(key: String) {
        MMKVTools.getInstance().putBoolean(key, false)
    }
}