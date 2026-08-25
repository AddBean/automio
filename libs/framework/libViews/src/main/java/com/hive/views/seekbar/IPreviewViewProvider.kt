// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2022/9/21
 */
interface IPreviewViewProvider {

    fun setVideoInfo(videoInfo: VideoInfoData)

    fun setPreviewImageProvider(previewImageProvider: IPreviewImageProvider)

    fun setViewSize(width: Int, height: Int)

    fun showAtWindow(x: Int, y: Int)

    fun seekTo(progress: Float)

    fun dismiss()

}