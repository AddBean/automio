// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.extension

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2021/10/14
 */
fun <T : RecyclerView> T.closeDefaultAnimator(): T {
    this.itemAnimator?.addDuration = 0
    this.itemAnimator?.changeDuration = 0
    this.itemAnimator?.moveDuration = 0
    this.itemAnimator?.removeDuration = 0
    (this.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations =false
    return this
}