// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar.impl

import android.os.Message
import com.hive.views.seekbar.IPreviewImageProvider
import com.hive.views.seekbar.VideoInfoData
import com.hive.views.seekbar.sprite.SpriteImageManager
import com.hive.views.utils.WorkHandler

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2022/9/21
 */
class DefaultPreviewImageProvider : IPreviewImageProvider, WorkHandler.IWorkHandler {

    private var onRetrieveListener: IPreviewImageProvider.OnRetrieveListener? = null
    private var progress: Float = 0f
    private var duration: Long = 0

    private val imageManager = SpriteImageManager()

    private val handler = WorkHandler(this)

    fun setSpriteInfo(indexUrl: String, images: List<String>, xLen: Int, yLen: Int) {
        imageManager.preloadSprite(indexUrl, images, xLen, yLen) {
            imageManager.fetchFrame(0) {}//预加载第一帧
        }
    }

    override fun setVideoInfo(videoInfo: VideoInfoData) {
        duration = videoInfo.duration
    }

    override fun retrieveBitmapAt(
        progress: Float,
        listener: IPreviewImageProvider.OnRetrieveListener
    ) {
        this.progress = progress
        onRetrieveListener = listener
        handler.removeMessages(0)
        handler.sendEmptyMessageDelayed(0, 10)
    }

    override fun release() {
        imageManager.release()
    }

    override fun handleMessage(msg: Message) {
        imageManager.fetchFrame(((progress * duration) / 1000).toInt()) {
            it?.run {
                onRetrieveListener?.onRetrieved(it)
            }
        }
    }
}