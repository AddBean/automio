// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.list

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 水平 RecyclerView 的 Item 间距装饰器
 * 支持设置每个 Item 之间的间距和第一个 Item 的左边距
 *
 * @param itemSpacing 每个 Item 之间的间距（dp）
 * @param firstItemStartMargin 第一个 Item 的左边距（dp）
 */
class HorizontalItemSpacingDecoration(
    private val itemSpacing: Int,
    private val firstItemStartMargin: Int = 0,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount

        if (position == RecyclerView.NO_POSITION) {
            return
        }

        if (position == 0) {
            // 第一个 Item：设置左边距
            outRect.left = firstItemStartMargin
            // 设置右边距（完整的 Item 间距）
            outRect.right = itemSpacing
        } else if (position == itemCount - 1) {
            // 最后一个 Item：左边距为 0（间距由前一个 Item 的右边距提供）
            outRect.left = 0
            outRect.right = 0
        } else {
            // 中间的 Item：左边距为 0（间距由前一个 Item 的右边距提供），右边距为完整间距
            outRect.left = 0
            outRect.right = itemSpacing
        }
    }
}
