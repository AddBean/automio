// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.extensions

import android.view.View
import com.hive.views.list_view.ListRecyclerView

val viewStateMap = mutableMapOf<Int, Int>()

fun View.saveViewState() {
    viewStateMap[this.hashCode()] = this.visibility
}

fun View.restoreViewState() {
    viewStateMap[this.hashCode()]?.let {
        this.visibility = it
        viewStateMap.remove(this.hashCode())
    }
}

fun ListRecyclerView.submitDataSetsWithType(dataSets: List<Pair<Int, Any?>>) {
    this.submitDataSetsWithType(dataSets.map { android.util.Pair<Int, Any?>(it.first, it.second) })
}