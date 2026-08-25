// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.views

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import com.hive.anim.AnimUtils
import com.hive.files.XFileListFragment
import com.hive.files.config.XFileConfig
import com.hive.libfiles.R
import com.hive.utils.utils.BaseSPClass
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/8/21
 */
class XFileFilterMenu(var context: Context, var parentFragment: XFileListFragment) : PopupWindow(context), View.OnClickListener {

    private var switch_layout_sort_time: ImageView
    private var switch_layout_sort_class: ImageView
    private var switch_layout_sort_size_desc: ImageView
    private var switch_layout_sort_size_asc: ImageView
    private var switch_layout_sort_name: ImageView
    private var switch_layout_grid: ImageView
    private var switch_layout_list: ImageView

    init {
        contentView = View.inflate(context, R.layout.x_file_filter_menu, null)
        isOutsideTouchable = false
        val dw = ColorDrawable(-0)
        setBackgroundDrawable(dw)
        isFocusable = true
        isTouchable = true
        switch_layout_list = contentView.findViewById(R.id.switch_layout_list)
        switch_layout_grid = contentView.findViewById(R.id.switch_layout_grid)
        switch_layout_sort_name = contentView.findViewById(R.id.switch_layout_sort_name)
        switch_layout_sort_size_asc = contentView.findViewById(R.id.switch_layout_sort_size_asc)
        switch_layout_sort_size_desc = contentView.findViewById(R.id.switch_layout_sort_size_desc)
        switch_layout_sort_class = contentView.findViewById(R.id.switch_layout_sort_class)
        switch_layout_sort_time = contentView.findViewById(R.id.switch_layout_sort_time)

        switch_layout_list?.setOnClickListener(this)
        switch_layout_grid?.setOnClickListener(this)
        switch_layout_sort_name?.setOnClickListener(this)
        switch_layout_sort_size_asc?.setOnClickListener(this)
        switch_layout_sort_size_desc?.setOnClickListener(this)
        switch_layout_sort_class?.setOnClickListener(this)
        switch_layout_sort_time?.setOnClickListener(this)

        updateSettingStatus()

    }

    private fun switchSortModel(sortType: Int) {
        var setting = BaseSPClass.read(XFileConfig())
        if (setting.sortType != sortType) {
            setting.sortType = sortType
            setting.save()
            parentFragment.updateDataState()
        }
        updateSettingStatus()
        when(sortType){
            0->CommonToast.getInstance().showToast(context.resources.getString(com.hive.i8n.R.string.x_file_sort_msg_0))
            1->CommonToast.getInstance().showToast(context.resources.getString(com.hive.i8n.R.string.x_file_sort_msg_1))
            2->CommonToast.getInstance().showToast(context.resources.getString(com.hive.i8n.R.string.x_file_sort_msg_2))
            3->CommonToast.getInstance().showToast(context.resources.getString(com.hive.i8n.R.string.x_file_sort_msg_3))
            4->CommonToast.getInstance().showToast(context.resources.getString(com.hive.i8n.R.string.x_file_sort_msg_4))
        }
    }

    private fun switchModel(inGrid: Boolean) {
        var setting = BaseSPClass.read(XFileConfig())
        if (setting.inGrid != inGrid) {
            setting.inGrid = inGrid
            setting.save()
            parentFragment.updateDataState()
        }
        updateSettingStatus()
    }

    private fun updateSettingStatus() {
        var setting = BaseSPClass.read(XFileConfig())
        switch_layout_list?.isSelected = !setting.inGrid
        switch_layout_grid?.isSelected = setting.inGrid

        switch_layout_sort_name?.isSelected = false
        switch_layout_sort_size_asc?.isSelected = false
        switch_layout_sort_size_desc?.isSelected = false
        switch_layout_sort_class?.isSelected = false
        switch_layout_sort_time?.isSelected = false

        when (setting.sortType) {
            0 -> switch_layout_sort_name?.isSelected = true
            1 -> switch_layout_sort_size_asc?.isSelected = true
            2 -> switch_layout_sort_size_desc?.isSelected = true
            3 -> switch_layout_sort_class?.isSelected = true
            4 -> switch_layout_sort_time?.isSelected = true
        }
    }

    companion object {
        fun showMenu(anchorView: View, frag: XFileListFragment, xoff: Int, yoff: Int, gravity: Int): XFileFilterMenu {
            var popMenuView = XFileFilterMenu(anchorView.context, frag)
            popMenuView.parentFragment = frag
            popMenuView.width = ViewGroup.LayoutParams.WRAP_CONTENT
            popMenuView.height = ViewGroup.LayoutParams.WRAP_CONTENT
            try {
                popMenuView.showAsDropDown(anchorView, xoff, yoff, gravity)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            return popMenuView;
        }
    }

    override fun onClick(v: View?) {
        AnimUtils.scaleAnim(v)
        when (v?.id) {
            R.id.switch_layout_list -> {
                switchModel(false)
            }
            R.id.switch_layout_grid -> {
                switchModel(true)
            }
            R.id.switch_layout_sort_name -> {
                switchSortModel(0)
            }
            R.id.switch_layout_sort_size_asc -> {
                switchSortModel(1)
            }
            R.id.switch_layout_sort_size_desc -> {
                switchSortModel(2)
            }
            R.id.switch_layout_sort_class -> {
                switchSortModel(3)
            }
            R.id.switch_layout_sort_time -> {
                switchSortModel(4)
            }
        }
    }


}