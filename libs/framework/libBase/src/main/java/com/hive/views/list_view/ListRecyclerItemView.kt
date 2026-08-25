// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.list_view

import android.content.Context
import android.widget.RelativeLayout

/**
 *
 * @author jiadou
 * @date 4/22/21
 */
abstract class ListRecyclerItemView(context: Context) : RelativeLayout(context) {

    var onItemEventListener: OnItemEventListener? = null

    var itemData: Any? = null

    var itemPosition: Int = -1

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

    fun postEvent(eventData: Any?, itemData: Any?) {
        onItemEventListener?.onItemEvent(itemData, eventData)
    }

    fun bindPosition(position: Int) {
        itemPosition = position
    }

    interface OnItemEventListener {
        fun onItemEvent(itemData: Any?, eventData: Any?)
    }

}