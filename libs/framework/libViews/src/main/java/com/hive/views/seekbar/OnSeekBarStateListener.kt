// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2022/9/19
 */
interface OnSeekBarStateListener {
    fun onTouchStatusChanged(action: PreviewSeekBar.Action, progress: Float)

    fun onProgressChanged(
        progress: Float,
        segments: List<PreviewSegmentData>?,
        originalSegments: List<PreviewSegmentData>?
    ) {
    }

    fun onPostProcessSegments(segments: List<PreviewSegmentData>): List<PreviewSegmentData> {
        return segments
    }

    fun onRetrievePlayerProgress(): Float

    fun shouldInterceptTracking(): Boolean = false
}