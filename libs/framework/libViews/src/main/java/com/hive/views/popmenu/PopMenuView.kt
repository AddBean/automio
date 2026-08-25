// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.popmenu

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import com.hive.views.R

open class PopMenuView<T>(context: Context) : PopupWindow(context) {
    private var mLayoutMenu: ViewGroup? = null
    private var mAdapter: PopMenuAdapter<T>? = null

    init {
        contentView = View.inflate(context, getLayoutId(), null)
        mLayoutMenu = contentView.findViewById(R.id.layout_menu)
        val dw = ColorDrawable(-0)
        setBackgroundDrawable(dw)
        isOutsideTouchable = false
        isFocusable = true
        isTouchable = true
    }

    open fun getLayoutId() = R.layout.pop_menu_base_layout

    fun setAdapter(adapter: PopMenuAdapter<T>) {
        mAdapter = adapter
        mAdapter?.setPopMenuView(this)
    }

    fun notifyDataSets() {
        mLayoutMenu?.removeAllViews()
        mAdapter?.run {
            for (i in 0 until getItemCount()) {
                var itemView = getItemView()
                bindItemView(itemView, mDataList!![i], i)
                itemView.setOnClickListener {
                    onItemClicked(it, mDataList!![i], i)
                    dismiss()
                }
                var lp = getItemLayoutParams()
                if (lp != null) {
                    mLayoutMenu?.addView(itemView, lp)
                } else {
                    mLayoutMenu?.addView(itemView)
                }
            }
        }
    }

    open fun getItemLayoutParams(): LinearLayout.LayoutParams? = null
}