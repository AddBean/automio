// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.view.accessibility.AccessibilityNodeInfo

/**
 *
 * @author jiadou
 * @date 2021/10/26
 */

fun List<AccessibilityNodeInfo>.sortByPriority(
    targetText: String?,
    targetId: String?,
    targetTag: String?
): List<AccessibilityNodeInfo> {
    return this.sortedByDescending {
        it.getPriority(targetText, targetId, targetTag)
    }
}


fun AccessibilityNodeInfo.getPriority(
    targetText: String?,
    targetId: String?,
    targetTag: String?
): Int {
//    var outBounds = Rect()
//    this.getBoundsInScreen(outBounds)
//    outBounds.width() * outBounds.height()
    var priority = 0
    if (this.text == targetText) {
        priority += 100
    }
    if (this.viewIdResourceName == targetId) {
        priority += 10
    }
    if (this.contentDescription == targetTag) {
        priority += 1
    }
    return priority
}