// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2022/9/19
 */
interface IPreviewSeekBar {

    /**
     * 设置进度条进度，使用归一化数值
     */
    fun setProgress(progress: Float)

    /**
     * 设置分段显示信息
     */
    fun setSegments(segments: List<PreviewSegmentData>)

    /**
     * 获取分段信息
     */
    fun getSegments():List<PreviewSegmentData>

    /**
     * 获取原始原始分段信息
     */
    fun setOriginalSegments(segments: List<PreviewSegmentData>)

    /**
     * 获取原始分段信息
     */
    fun getOriginalSegments():List<PreviewSegmentData>

    /**
     * 设置状态
     */
    fun setState(state: PreviewSeekBar.State)

    /**
     * 获取当前状态
     */
    fun getCurrentState(): PreviewSeekBar.State

    /**
     * 是否允许滑动的操作
     */
    fun setTouchEnable(enable: Boolean)

    /**
     * 启用全区域触摸
     */
    fun setGlobalTouchEnable(enable: Boolean)

    /**
     * 设置定期器的周期，此定时器会定时取视频当前进度及其他信息
     */
    fun setTimeInterval(interval: Long)

    /**
     * 设置进度条聚合管理器，用于处理预览窗等和其他控件的交互
     */
    fun setPreviewSeekBarGlue(glue: AbsPreviewSeekBarGlue?)

    /**
     * 获取当前聚合器
     */
    fun getPreviewSeekBarGlue():AbsPreviewSeekBarGlue?

    /**
     * 设置自定义的绘图器，如果没有设置会使用默认风格的绘图器
     *
     * @param state 绘图器对应的状态 Loading, Playing, Paused, Tracking
     *  @param drawer 自定义绘图器
     */
    fun setStateDrawer(state: PreviewSeekBar.State, drawer: AbsPreviewSeekBarDrawer)

    /**
     * 设置进度条监听，其中[onRetrievePlayerProgress]方法必须实现，进度条会定时取进度
     */
    fun setOnStatusChangedListener(listener: OnSeekBarStateListener?)

    /**
     * 释放seekBar
     */
    fun release()

}