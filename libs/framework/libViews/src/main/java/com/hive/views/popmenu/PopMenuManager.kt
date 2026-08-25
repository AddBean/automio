// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.popmenu

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.dp
import com.hive.utils.system.UIUtils
import com.hive.views.R
import java.lang.Exception

class PopMenuManager {

    companion object {
        val instance: PopMenuManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            PopMenuManager()
        }
    }

    fun <T> showMenu(anchorView: View, adapter: PopMenuAdapter<T>): PopMenuView<T> {
        return showMenu(anchorView, 6.dp, -2.dp, Gravity.BOTTOM, PopMenuView(anchorView.context), adapter)
    }


    fun showMenu(anchorView: View, dataList: List<String>, listener: OnItemClickListener<String>):PopMenuView<String>  {
        val adapter = object : DefaultPopMenuAdapter<String>(anchorView.context) {
            override fun onItemClicked(view: View, data: String, pos: Int) {
                listener.onItemClicked(view, data, pos)
            }
        }.apply { setDataList(dataList) }
      return  showMenu(anchorView, adapter)
    }

    fun showMenu(
        anchorView: View,
        xoff: Int,
        yoff: Int,
        dataList: List<String>,
        listener: OnItemClickListener<String>
    ) {
        val adapter = object : DefaultPopMenuAdapter<String>(anchorView.context) {
            override fun onItemClicked(view: View, data: String, pos: Int) {
                listener.onItemClicked(view, data, pos)
            }
        }.apply { setDataList(dataList) }
        showMenu(anchorView, xoff, yoff, Gravity.BOTTOM, PopMenuView(anchorView.context), adapter)
    }

    fun showMenu(
        anchorView: View,
        dataList: List<String>,
        popMenuView: PopMenuView<String>,
        listener: OnItemClickListener<String>
    ) {
        val adapter = object : DefaultPopMenuAdapter<String>(anchorView.context) {
            override fun onItemClicked(view: View, data: String, pos: Int) {
                listener.onItemClicked(view, data, pos)
            }
        }.apply { setDataList(dataList) }
        showMenu(anchorView, 0, 0, Gravity.BOTTOM, popMenuView, adapter)
    }

    fun <T> showMenu(
        anchorView: View,
        xoff: Int,
        yoff: Int,
        gravity: Int,
        popMenuView: PopMenuView<T>,
        adapter: PopMenuAdapter<T>
    ): PopMenuView<T> {
        popMenuView.setAdapter(adapter)
        adapter.notifyDataSets()
        popMenuView.width = ViewGroup.LayoutParams.WRAP_CONTENT
        popMenuView.height = ViewGroup.LayoutParams.WRAP_CONTENT
        try {
            popMenuView.showAsDropDown(anchorView, xoff, yoff, gravity)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return popMenuView;
    }

    open class DefaultPopMenuAdapter<T>(var context: Context) : PopMenuAdapter<T>() {
        override fun getItemView(): View =
            LayoutInflater.from(context).inflate(R.layout.defaut_popmenu_layout, null)

        override fun bindItemView(itemView: View, data: T, pos: Int) {
            itemView.findViewById<TextView>(R.id.tv_name).text = data.toString()
            itemView.findViewById<View>(R.id.view_line).visibility = if (pos == ((mDataList?.size
                    ?: 1) - 1)
            ) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        override fun onItemClicked(view: View, data: T, pos: Int) {

        }
    }

    interface OnItemClickListener<T> {
        fun onItemClicked(view: View, data: T, pos: Int);
    }

}