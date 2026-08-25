// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.views

import android.content.Context
import com.hive.files.model.XFileSetting
import com.hive.libfiles.R
import com.hive.views.SampleDialog
import com.hive.views.widgets.TextDrawableView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/30/21
 */
class SampleDeleteDialog(context: Context) : SampleDialog(context) {

    private var bin_check_box: TextDrawableView? = null

    override fun initView() {
        super.initView()
        bin_check_box = findViewById(R.id.bin_check_box)
        updateSelectStatus(XFileSetting.instance.enableRecyclerBin)
        bin_check_box?.setOnClickListener {
            updateSelectStatus(bin_check_box?.isSelected == false)
        }
    }

    private fun updateSelectStatus(isSelected: Boolean) {
        bin_check_box?.isSelected = isSelected
        if (bin_check_box?.isSelected == true) {
            bin_check_box?.setDrawableLeft(context.resources.getDrawable(R.drawable.x_file_selector_selected))
        } else {
            bin_check_box?.setDrawableLeft(context.resources.getDrawable(R.drawable.x_file_selector_unselected))
        }
    }

    override fun getLayoutId() = R.layout.sample_delete_dialog

    fun isRecycleToBin() = bin_check_box?.isSelected ?: true
}