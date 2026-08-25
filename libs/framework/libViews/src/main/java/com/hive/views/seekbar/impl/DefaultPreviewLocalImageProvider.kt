// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar.impl

import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.OPTION_CLOSEST
import android.os.Message
import android.text.TextUtils
import com.hive.views.seekbar.IPreviewImageProvider
import com.hive.views.seekbar.VideoInfoData
import com.hive.utils.debug.DLog
import com.hive.views.utils.WorkHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2022/9/21
 */
class DefaultPreviewLocalImageProvider :
    IPreviewImageProvider, WorkHandler.IWorkHandler {

    private var progress = -1f

    private var onRetrieveListener: IPreviewImageProvider.OnRetrieveListener? = null

    private var videoInfo: VideoInfoData? = null

    private var retriever = MediaMetadataRetriever()

    private val handler = WorkHandler(this)

    private var disposeLast = -1f

    private var curVideoUrl: String? = null

    override fun handleMessage(msg: Message) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                videoInfo?.run {
                    val curProgress = progress
                    if (TextUtils.isEmpty(videoUrl)) return@launch
                    DLog.e("DefaultPreviewLocalImageProvider", "videoInfo.url=${videoUrl}")
                    if (disposeLast == curProgress) {
                        return@launch
                    }
                    if (curVideoUrl != videoUrl) {
                        curVideoUrl = videoUrl
                        retriever.setDataSource(videoUrl, HashMap())
                    }
                    val bitmap = retriever.getFrameAtTime((duration * progress * 1000L).toLong(),OPTION_CLOSEST)
                    withContext(Dispatchers.Main) {
                        bitmap?.run {
                            onRetrieveListener?.onRetrieved(bitmap)
                        }
                    }
                }
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
        }
    }


    override fun setVideoInfo(videoInfo: VideoInfoData) {
        this.videoInfo = videoInfo
    }

    override fun retrieveBitmapAt(
        progress: Float,
        listener: IPreviewImageProvider.OnRetrieveListener
    ) {
        disposeLast = this.progress
        this.progress = progress
        onRetrieveListener = listener
        handler.removeMessages(0)
        handler.sendEmptyMessageDelayed(0, 100)
    }

    override fun release() {
        retriever.release()
        handler.removeMessages(0)
    }

}