// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.extends

import android.widget.Toast
import com.hive.utils.GlobalApp
import com.hive.utils.thread.UIHandlerUtils


/**
 * 保留指定小数位数
 */
fun String.takeLastWords(count: Int): String {
    return if (this.length > count) this.takeLast(count) else this
}

fun String.toastShort() {
    toastOnMain(this, Toast.LENGTH_SHORT)
}

fun String.toastLong() {
    toastOnMain(this, Toast.LENGTH_LONG)
}

private fun toastOnMain(msg: String, duration: Int) {
    fun toastInner() {
        Toast.makeText(GlobalApp.getContext(), msg, duration).show()
    }

    if (UIHandlerUtils.isOnMainThread()) {
        toastInner()
    } else {
        UIHandlerUtils.runUI { toastInner() }
    }

}