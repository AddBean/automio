// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.card

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.hive.timer.R
import com.hive.views.list_view.ListRecyclerItemView

class AlarmAddView(context: Context) : ListRecyclerItemView(context), View.OnClickListener {

    val layout = LayoutInflater.from(context).inflate(R.layout.alarm_item_add_view, this).apply {
        setOnClickListener(this@AlarmAddView)
    }

    override fun onClick(p0: View?) {
        postEvent("add")
    }

    override fun bindData(data: Any?) {
    }


}