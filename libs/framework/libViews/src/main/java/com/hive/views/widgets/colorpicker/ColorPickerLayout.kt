// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.colorpicker

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.hive.views.R
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @date 4/16/21
 */
class ColorPickerLayout(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    var view = LayoutInflater.from(context).inflate(R.layout.color_picker_layout, this)
    var color_picker_view: ColorPickerView = view.findViewById(R.id.color_picker_view)

    init {
        post {

            color_picker_view.color = Color.YELLOW
            color_picker_view.borderColor=Color.TRANSPARENT

            color_picker_view.setAlphaSliderVisible(true)
//            color_picker_view. setUpAlphaRect()
            color_picker_view.setOnColorChangedListener {
                CommonToast.getInstance().showToast("it"+it)
            }
        }
    }
}