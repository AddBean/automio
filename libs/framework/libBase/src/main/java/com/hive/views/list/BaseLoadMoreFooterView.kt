// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.list

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * LoadMoreView 抽象基类
 * 继承此类的 View 可以根据状态自动更新显示
 *
 * @author jiadou
 */
abstract class BaseLoadMoreFooterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * 更新加载更多状态
     * @param state 当前状态
     */
    abstract fun updateState(state: LoadMoreState)

    /**
     * LoadMoreView 状态枚举
     */
    enum class LoadMoreState {
        /**
         * 加载中
         */
        LOADING,

        /**
         * 没有更多数据
         */
        NO_MORE,

        /**
         * 加载失败
         */
        ERROR,

        /**
         * 准备加载（上拉中）
         */
        PREPARE,

        /**
         * 空闲状态（通常隐藏）
         */
        IDLE
    }

}

