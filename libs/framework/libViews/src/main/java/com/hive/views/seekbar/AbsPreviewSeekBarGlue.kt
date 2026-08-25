// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar

import com.hive.views.seekbar.VideoInfoData

/**
 * @author jiadou
 * @date 2022/9/21
 */
abstract class AbsPreviewSeekBarGlue() {

    abstract fun setVideoInfo(videoInfo: VideoInfoData)

    abstract fun getVideoInfo(): VideoInfoData?

    abstract fun onStartTracking(progress: Float)

    abstract fun onDoTracking(progress: Float)

    abstract fun onFinishTracking(progress: Float)

    abstract fun release()

}