// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

object ScriptColorHelper {
    private val colorMap = mutableSetOf<Int>().apply {
        listOf(
            "#FF0000",
            "#00FF00",
            "#0000FF",
            "#FFFF00",
            "#00FFFF",
            "#FF00FF"
        ).map { android.graphics.Color.parseColor(it) }.forEach { add(it) }
    }

    fun addColorToFirst(color: Int?) {
        color ?: return
        if (colorMap.contains(color)) {
            colorMap.remove(color)
        }
        colorMap.add(color)
    }

    fun getColorList(): List<Int> {
        return colorMap.toList().reversed()
    }
}