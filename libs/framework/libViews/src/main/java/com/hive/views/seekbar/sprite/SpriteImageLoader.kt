// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar.sprite

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import java.lang.ref.WeakReference

/**
 *
 * @author jiadou
 * @date 2022/9/21
 */
class SpriteImageLoader {

    private val downloader = SpriteImageDownloader()

    suspend fun loadImageFiles(
        indexList: List<Int>,
        images: List<String>,
        xLen: Int,
        yLen: Int
    ): List<SpriteImageBean> {
        val size = getImageSize(images.first())
        return loadImageFiles(indexList, images, size.first, size.second, xLen, yLen)
    }

    private fun loadImageFiles(
        indexList: List<Int>,
        images: List<String>,
        imageWidth: Int,
        imageHeight: Int,
        xLen: Int,
        yLen: Int
    ): List<SpriteImageBean> {
        val spriteImageList = mutableListOf<SpriteImageBean>()
        for (i in images.indices) {
            for (j in 0 until yLen) {
                val size = xLen * yLen
                var frameStartIndex = i * size + j * xLen
                var frameEndIndex = i * size + j * xLen + xLen - 1
                if (frameStartIndex >= indexList.size) {
                    continue
                }
                if (frameEndIndex >= indexList.size) {
                    frameEndIndex = indexList.size - 1
                }
                val frameStartTime = indexList[frameStartIndex]
                val frameEndTime = indexList[frameEndIndex]
                val imageBean = SpriteImageBean()
                imageBean.url = images[i]
                imageBean.startTime = frameStartTime
                imageBean.endTime = frameEndTime
                imageBean.frames = mutableListOf<SpriteFrameBean>().apply {
                    for (index in frameStartIndex..frameEndIndex) {
                        add(SpriteFrameBean().apply {
                            timestamp = indexList[index]
                            width = imageWidth / xLen
                            height = imageHeight / yLen
                            x = (index % xLen) * width
                            y = j * height
                        })
                    }
                }
                imageBean.minX = imageBean.frames.minOf { it.x }
                imageBean.minY = imageBean.frames.minOf { it.y }
                imageBean.maxX = imageBean.frames.maxOf { it.x } + imageWidth / yLen
                imageBean.maxY = imageBean.frames.maxOf { it.y } + imageHeight / yLen
                spriteImageList.add(imageBean)
            }
        }
        return spriteImageList
    }

    suspend fun fetchFrameBitmap(imageBean: SpriteImageBean, frameBean: SpriteFrameBean): Bitmap? {
        var bmp = imageBean.refBitmap?.get()
        if (bmp == null) {
            if (imageBean.path == null) {
                imageBean.path = downloader.downloadImage(imageBean.url)
            }
            imageBean.path ?: return null

            val rect = Rect(imageBean.minX, imageBean.minY, imageBean.maxX, imageBean.maxY)
            try{
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.RGB_565
                bmp = BitmapRegionDecoder.newInstance(imageBean.path, false).decodeRegion(rect, options)
                imageBean.refBitmap = WeakReference(bmp)
            }catch (e:java.lang.Exception){
            }
        }
        bmp ?: return null

        var frameBmp = frameBean.refBitmap?.get()
        if (frameBmp != null) return frameBmp

        frameBmp = cropBitmap(bmp, frameBean.x, 0, frameBean.width, frameBean.height)
        frameBean.refBitmap = WeakReference(frameBmp)
        return frameBmp
    }


    private fun cropBitmap(bitmap: Bitmap, x: Int, y: Int, w: Int, h: Int): Bitmap? {
        try {
            if (x + w > bitmap.width || y + h > bitmap.height) return null
            return Bitmap.createBitmap(bitmap, x, y, w, h, null, false)
        } catch (e: Exception) {
            e.printStackTrace()
            return null;
        }
    }

    suspend fun getImageSize(url: String): Pair<Int, Int> {
        val path = downloader.downloadImage(url)
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        return (options?.outWidth ?: 0) to (options?.outHeight ?: 0)
    }

}