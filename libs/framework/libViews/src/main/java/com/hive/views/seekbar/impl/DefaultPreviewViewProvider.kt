// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar.impl

import android.app.Activity
import android.graphics.Bitmap
import android.view.ViewGroup
import com.hive.views.seekbar.IPreviewImageProvider
import com.hive.views.seekbar.IPreviewViewProvider
import com.hive.views.seekbar.VideoInfoData

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2022/9/21
 */
class DefaultPreviewViewProvider(val activity: Activity) : IPreviewViewProvider {
    private var videoInfo: VideoInfoData? = null
    private lateinit var previewImageProvider: IPreviewImageProvider

    private val previewView = VideoPreviewFloatView(activity)

    override fun setVideoInfo(videoInfo: VideoInfoData) {
        this.videoInfo = videoInfo
    }

    override fun setPreviewImageProvider(previewImageProvider: IPreviewImageProvider) {
        this.previewImageProvider = previewImageProvider
    }

    override fun setViewSize(width: Int, height: Int) {
        previewView.setViewSize(width, height)
        previewView.requestLayout()
    }

    override fun showAtWindow(x: Int, y: Int) {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        removeView()
        previewView.translationX = x.toFloat()
        previewView.translationY = y.toFloat()
        rootView?.addView(previewView)
    }

    override fun seekTo(progress: Float) {
        previewImageProvider.retrieveBitmapAt(progress,
            object : IPreviewImageProvider.OnRetrieveListener {
                override fun onRetrieved(bmp: Bitmap) {
                    previewView.loadBitmap(bmp)
                }
            })
    }

    override fun dismiss() {
        removeView()
    }

    private fun removeView() {
        if (previewView.parent != null) {
            (previewView.parent as ViewGroup).removeView(previewView)
        }
    }
}