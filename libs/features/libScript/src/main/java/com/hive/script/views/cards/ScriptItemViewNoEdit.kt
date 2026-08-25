// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.cards

import android.content.Context
import android.view.View
import android.widget.TextView
import com.hive.script.R

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/17/21
 */
class ScriptItemViewNoEdit(context: Context) : ScriptItemView(context), View.OnClickListener {

    init {
        findViewById<TextView>(R.id.tvName).setCompoundDrawables(null, null, null, null)
    }

    override fun getItemContentId(): Int {
        return R.layout.fragment_script_item_layout2
    }

}

