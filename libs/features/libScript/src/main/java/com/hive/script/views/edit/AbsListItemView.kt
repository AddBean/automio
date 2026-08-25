// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit

import android.content.Context
import android.widget.RelativeLayout

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/22/21
 */
abstract class AbsListItemView(context: Context) : RelativeLayout(context) {

    var onItemEventListener: OnItemEventListener? = null

    var itemData: Any? = null

    var pos = 0

    var size = 0

    fun bindItemData(data: Any?) {
        itemData = data
        bindData(data)
    }

    protected abstract fun bindData(data: Any?)

    open fun onDragClear() {}

    open fun onDragSelected() {}

    fun postEvent(eventData: Any?) {
        onItemEventListener?.onItemEvent(itemData, eventData)
    }

    interface OnItemEventListener {
        fun onItemEvent(itemData: Any?, eventData: Any?)
    }

}