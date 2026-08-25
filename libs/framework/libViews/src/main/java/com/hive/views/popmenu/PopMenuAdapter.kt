// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.popmenu

import android.view.View

abstract class PopMenuAdapter<T> {

    private var mPopMenuView: PopMenuView<T>? = null


    var mDataList: List<T>? = null

    fun setDataList(dataList: List<T>) {
        this.mDataList = dataList;
    }

    fun getItemCount() = mDataList?.size ?: 0

    abstract fun getItemView(): View

    abstract fun bindItemView(itemView: View, data: T, pos: Int)

    abstract fun onItemClicked(view: View, data: T, pos: Int)

    fun notifyDataSets() {
        mPopMenuView?.notifyDataSets()
    }

    fun setPopMenuView(popMenuView: PopMenuView<T>) {
        this.mPopMenuView = popMenuView
    }
}