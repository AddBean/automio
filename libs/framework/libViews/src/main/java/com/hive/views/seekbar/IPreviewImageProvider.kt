// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar

import android.graphics.Bitmap

/**
 *
 * @author jiadou
 * @date 2022/9/21
 */
interface IPreviewImageProvider {

    fun setVideoInfo(videoInfo: VideoInfoData)

    fun retrieveBitmapAt(progress: Float,listener: OnRetrieveListener)

    fun release()

    interface OnRetrieveListener {
        fun onRetrieved(bmp: Bitmap)
    }
}