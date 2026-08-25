// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.cards

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import com.hive.script.R
import com.hive.script.views.widgets.ScriptColorRecentView
import com.hive.views.list_view.ListRecyclerItemView

/**
 * @author jiadou
 * @date 4/7/21
 */
class ScriptColorCard(context: Context, val colorView: ScriptColorRecentView) :
    ListRecyclerItemView(context) {

    private val view = LayoutInflater.from(context).inflate(R.layout.script_color_card, this)

    override fun bindData(data: Any?) {
        itemData = data as Int
        val iv_icon = view.findViewById<ImageView>(R.id.iv_icon)
        iv_icon?.setBackgroundColor(itemData as Int)
        iv_icon?.isSelected = itemData == colorView.selectColor
    }
}