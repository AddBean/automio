// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar

import android.content.Context
import android.graphics.Canvas
import android.view.Gravity
import android.widget.FrameLayout
import com.hive.utils.system.UIUtils
import com.hive.views.widgets.loading.DYLoadingView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2022/9/20
 */
class PreviewSeekBarLoadingDrawer : PreviewSeekBarPlayingDrawer() {

    private var loadingView: DYLoadingView? = null


    override fun onDraw(canvas: Canvas, progress: Float, segments: List<PreviewSegmentData>) {}

    override fun onShow() {
        if (loadingView == null) {
//            app:dyMinHeight="2dp"
//            app:dyMinProgressWidth="10dp"
//            app:dyMinWidth="600dp"
//            app:dyProgressColor="#eeeeee"
            loadingView = DYLoadingView(getContext());
            loadingView?.mDefaultWidth = 600f * dp
            loadingView?.mDefaultHeight = 2f * dp
            loadingView?.mColor = "#eeeeee"
        }
        hostView?.removeAllViews()
        hostView?.addView(
            loadingView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (2f * dp).toInt()
            ).apply {
                this.gravity = Gravity.CENTER_VERTICAL
            }
        )
        loadingView?.start()
    }

    override fun onHidden() {
        loadingView?.stop()
        hostView?.removeAllViews()
    }


    override fun onDetached() {
        super.onDetached()
        loadingView?.destroyDrawingCache()
    }
}