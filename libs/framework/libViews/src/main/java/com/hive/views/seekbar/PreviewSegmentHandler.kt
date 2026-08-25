// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar

/**
 *
 * @author jiadou
 * @date 2022/9/20
 */
object PreviewSegmentHandler {

    /**
     * 处理SegmentData
     * 规则1：分段数最高为38，否则不展示分段
     */
    fun handleSegmentData0(
        segments: List<PreviewSegmentData>
    ): List<PreviewSegmentData> {
        segments.forEach {
            if (it.inPoint < 0f) {
                it.inPoint = 0f
            }
            if (it.outPoint > 1f) {
                it.outPoint = 1f
            }
        }

        val newSegments = segments.toMutableList()
        if (segments.size > 38) {
            newSegments.clear()
            return newSegments
        }
        return newSegments
    }

    /**
     * 处理SegmentData
     * 规则1：重叠部部分合并为一个
     * 递归处理重叠
     */
    fun handleSegmentData1(
        newSegments: MutableList<PreviewSegmentData>,
        processWhite: Boolean
    ): List<PreviewSegmentData> {
        val clips = newSegments.filter { it.isCanReplace == processWhite }
        //合并相交的不可替换部分
        for (i in clips.indices) {
            for (j in clips.indices) {
                if (i != j) {
                    val c1 = clips[i]
                    val c2 = clips[j]
                    if (c1.inPoint <= c2.inPoint && c1.outPoint >= c2.outPoint) {
                        newSegments.remove(c2)
                        return handleSegmentData1(newSegments, processWhite)
                    } else if (c1.outPoint < c2.outPoint && c1.outPoint > c2.inPoint) {
                        c1.outPoint = c2.outPoint
                        newSegments.remove(c2)
                        return handleSegmentData1(newSegments, processWhite)
                    } else if (c1.inPoint > c2.inPoint && c1.inPoint < c2.outPoint) {
                        c1.inPoint = c2.inPoint
                        newSegments.remove(c2)
                        return handleSegmentData1(newSegments, processWhite)
                    }
                }
            }
        }
        return newSegments
    }


    /**
     * 如果黑白存在相交，则裁剪黑色部分
     */
    fun handleSegmentData2(segments: List<PreviewSegmentData>): List<PreviewSegmentData> {
        val newSegments = segments.toMutableList()
        newSegments.sortBy { it.inPoint }
        val replaceClips = segments.filter { it.isCanReplace }
        val unReplaceClips = segments.filter { !it.isCanReplace }

        for (i in unReplaceClips.indices) {
            for (j in replaceClips.indices) {
                val c1 = unReplaceClips[i]
                val c2 = replaceClips[j]
                if (c1.inPoint >= c2.inPoint && c1.outPoint <= c2.outPoint) {//c2包含c1
                    newSegments.remove(c1)
                    return handleSegmentData2(newSegments)
                } else if (c1.inPoint <= c2.inPoint && c1.outPoint >= c2.outPoint) {//c1包含c2
                    newSegments.add(PreviewSegmentData(false, c2.outPoint, c1.outPoint, null, null))
                    c1.outPoint = c2.inPoint
                    return handleSegmentData2(newSegments)
                } else if (c1.outPoint < c2.outPoint && c1.outPoint > c2.inPoint) {
                    c1.outPoint = c2.inPoint
                    return handleSegmentData2(newSegments)
                } else if (c1.inPoint > c2.inPoint && c1.inPoint < c2.outPoint) {
                    c1.inPoint = c2.outPoint
                    return handleSegmentData2(newSegments)
                }
            }
        }
        return newSegments
    }

    /**
     * 处理SegmentData
     * 规则2：如果分段存在gap，则补上不可替换分段
     */
    fun handleSegmentData3(segments: List<PreviewSegmentData>): List<PreviewSegmentData> {
        val newSegments = segments.toMutableList()
        newSegments.sortBy { it.inPoint }
        var newSegments2 = newSegments.toList().toMutableList()
        val size = newSegments.size
        var insertStep = 1
        for (i in 0 until size) {
            val curInPoint = newSegments[i].inPoint
            val curOutPoint = newSegments[i].outPoint
            if (i == 0 && curInPoint > 0) {
                newSegments2.add(
                    0,
                    PreviewSegmentData(false, 0f, curInPoint, null, null)
                )
                insertStep++
            }
            val nextInPoint = newSegments.takeIf { it.size > i + 1 }?.get(i + 1)?.inPoint ?: 1f
            if (curOutPoint < nextInPoint) {
                newSegments2.add(
                    i + insertStep,
                    PreviewSegmentData(false, curOutPoint, nextInPoint, null, null)
                )
                insertStep++
            }
        }
        newSegments2 = newSegments2.filter { it.inPoint < 1f }.toMutableList()
        newSegments2.forEach {
            if (it.outPoint > 1f) {
                it.outPoint = 1f
            }
        }
        return newSegments2
    }

}