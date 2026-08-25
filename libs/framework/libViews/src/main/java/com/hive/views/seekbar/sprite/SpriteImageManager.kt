// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar.sprite

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/**
 *
 * @author jiadou
 * @date 2022/9/21
 * @see
 * 雪碧图解析原理：
 * 预加载流程：
 *  1，下载索引文件并解析；
 *  2，根据雪碧图数量，解析按行解析（仅计算对应时间、位置），每行对应一个SpriteImageBean；
 *  3，逐行解析贴图（仅计算对应时间、位置），每个贴图对应一个SpriteFrameBean;
 * 注意：预加载流程仅需加载首图，用于获取雪碧图大小，方便后面计算，其余皆不会阻塞线程；
 *
 * 查找及加载贴图流程：
 *  1，根据视频时间查找对应帧信息，即搜索最近值：SpriteImageBean->SpriteFrameBean；
 *  2，找到对应雪碧图和位置后，下载雪碧图到本地；
 *  3，根据SpriteImageBean信息，找到雪碧图的对应行；
 *  4，仅加载对应行的bitmap，并缓存到内存；
 *  5，根据SpriteFrameBean坐标信息，截取对应视频贴图；
 *  6，返回视频贴图，并更新视频缓存；
 * 注意:第4步很关键，避免全图加载，雪碧图大小约4800*2700，一次加载需占用内存约40MB
 *
 */
class SpriteImageManager {

    private val indexLoader = SpriteIndexLoader()

    private val imageLoader = SpriteImageLoader()

    private var spriteImageList: MutableList<SpriteImageBean>? = null

    private val defaultFrameBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.RGB_565).apply {
        Canvas(this).drawColor(Color.BLACK)
    }

    @Volatile
    private var isLoadReady = false

    private val WaitOverTime = 3 * 2000L//超过3s则退出

    private var currentIndexUrl: String? = null

    private var currentFrameBean: SpriteFrameBean? = null

    fun preloadSprite(
        indexUrl: String,
        images: List<String>,
        xLen: Int,
        yLen: Int,
        onPreloadFinished: (() -> Unit)?
    ) {
        if (currentIndexUrl == indexUrl) return
        isLoadReady = false
        GlobalScope.launch(Dispatchers.IO) {
            currentIndexUrl = indexUrl
            spriteImageList?.clear()
            val indexList = indexLoader.loadIndexFile(indexUrl)
            indexList ?: return@launch
            spriteImageList =
                imageLoader.loadImageFiles(indexList, images, xLen, yLen).toMutableList()
            isLoadReady = true
            onPreloadFinished?.invoke()
        }
    }

    /**
     * 根据位置获取截图
     * @param timestamp 单位秒
     */
    @OptIn(DelicateCoroutinesApi::class)
    @Synchronized
    fun fetchFrame(timestamp: Int, result: (bmp: Bitmap?) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            var waitTime = 0
            while (!isLoadReady) {
                Thread.sleep(10)
                waitTime += 10
                if (waitTime > WaitOverTime) {
                    result.invoke(null)
                    return@launch
                }
            }
            val frameBean = fetchFrame(timestamp)
            withContext(Dispatchers.Main) {
                result.invoke(frameBean?.refBitmap?.get() ?: defaultFrameBitmap)
            }
        }
    }

    private suspend fun fetchFrame(timestamp: Int): SpriteFrameBean? {
        var image = spriteImageList?.find { timestamp >= it.startTime && timestamp <= it.endTime }
        if (image == null) {
            image = spriteImageList?.find { timestamp <= it.startTime }
        }
        val frames = image?.frames
        frames?.sortBy { it.timestamp }
        frames?.forEach {
            if (timestamp <= it.timestamp) {
                if (currentFrameBean == it && currentFrameBean?.refBitmap?.get() != null) return currentFrameBean
                currentFrameBean = it
                if (it.refBitmap?.get() != null) return it
                it.refBitmap = WeakReference(imageLoader.fetchFrameBitmap(image!!, it))
                return it
            }
        }
        return null
    }

    fun release() {

    }
}