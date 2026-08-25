// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar.impl

import android.app.Activity
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.view.View
import com.hive.views.seekbar.AbsPreviewSeekBarGlue
import com.hive.views.seekbar.IPreviewImageProvider
import com.hive.views.seekbar.IPreviewViewProvider
import com.hive.views.seekbar.VideoInfoData
import com.hive.utils.debug.DLog
import com.hive.utils.system.UIUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *
 * @author jiadou
 * @date 2022/9/21
 */
open class DefaultPreviewSeekBarGlue(
    private val activity: Activity,
    private var videoInfoData: VideoInfoData,
    private val previewViewProvider: IPreviewViewProvider,
    private val previewImageProvider: IPreviewImageProvider
) : AbsPreviewSeekBarGlue() {

    public val dp = UIUtils.dp2px(activity,1)

    private var retriever: MediaMetadataRetriever? = MediaMetadataRetriever()

    private var retrieveCallback: ((videoInfo: VideoInfoData?) -> Unit)? = null

    private var isShowing = false

    init {
        setVideoInfo(videoInfoData)
    }

    override fun setVideoInfo(videoInfo: VideoInfoData) {
        videoInfoData = videoInfo
        previewViewProvider.setVideoInfo(videoInfo)
        previewImageProvider.setVideoInfo(videoInfo)
        DLog.e("DefaultPreviewSeekBarGlue", "videoInfo.url=${videoInfo.videoUrl}")
    }

    override fun getVideoInfo(): VideoInfoData? = videoInfoData

    override fun onStartTracking(progress: Float) {
        previewViewProvider.setPreviewImageProvider(previewImageProvider)

        if (videoInfoData != null && videoInfoData!!.videoWidth > 0) {
            val size = calculateViewSize()
            previewViewProvider.setViewSize(
                size.first,
                size.second
            )
            val p = getShowLocation(size.first, size.second)
            previewViewProvider.showAtWindow(p.x, p.y)
            previewViewProvider.seekTo(progress)
            isShowing = true
        } else if (videoInfoData?.videoWidth == 0) {
            isShowing = true
            retrieveVideoInfo(videoInfoData!!.videoUrl) {
                if (!isShowing) return@retrieveVideoInfo
                val size = calculateViewSize()
                previewViewProvider.setViewSize(
                    size.first,
                    size.second
                )

                val p = getShowLocation(size.first, size.second)
                previewViewProvider.showAtWindow(p.x, p.y)
                previewViewProvider.seekTo(progress)

            }
        } else {
            previewViewProvider.setViewSize(
                140 * dp,
                140 * dp,
            )
            val p = getShowLocation(140 * dp, 140 * dp)
            previewViewProvider.showAtWindow(p.x, p.y)
            previewViewProvider.seekTo(progress)
            isShowing = true
        }
    }

    override fun onDoTracking(progress: Float) {
        previewViewProvider.seekTo(progress)
        isShowing = true
    }

    override fun onFinishTracking(progress: Float) {
        previewViewProvider.dismiss()
        isShowing = false
    }

    private fun calculateViewSize(): Pair<Int, Int> {
        var width = 80f * dp
        var height = 80f * dp
        if (videoInfoData.videoWidth > videoInfoData.videoHeight) {
            width =
                80f * dp * (videoInfoData.videoWidth / videoInfoData.videoHeight.toFloat())
            height = 80f * dp
        } else {
            width =
                140 * dp * (videoInfoData.videoWidth / videoInfoData.videoHeight.toFloat())
            height = 140f * dp
        }

        return width.toInt() to height.toInt()
    }


    private fun retrieveVideoInfo(url: String, callback: (videoInfo: VideoInfoData?) -> Unit) {
        retrieveCallback = callback
        DLog.e("DefaultPreviewSeekBarGlue", "videoInfo.url=${url}")
        GlobalScope.launch(Dispatchers.IO) {
            try {
                retriever?.release()
                retriever = MediaMetadataRetriever()
                retriever?.setDataSource(url, HashMap())
                val width =
                    retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        ?.toIntOrNull()
                val height =
                    retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        ?.toIntOrNull()
                videoInfoData.videoWidth = width ?: 0
                videoInfoData.videoHeight = height ?: 0
                withContext(Dispatchers.Main) {
                    retrieveCallback?.invoke(videoInfoData)
                }
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
        }
    }


    open fun getShowLocation(floatViewWidth: Int, floatViewHeight: Int): Point {
        val rootView = activity.findViewById<View>(android.R.id.content)
        val x = rootView.measuredWidth / 2 - floatViewWidth / 2
        val y = rootView.measuredHeight - 200 * dp - floatViewHeight
        return Point(x, y)
    }

    override fun release() {
        retriever?.release()
        retrieveCallback = null
        previewViewProvider.dismiss()
        previewImageProvider.release()
    }
}