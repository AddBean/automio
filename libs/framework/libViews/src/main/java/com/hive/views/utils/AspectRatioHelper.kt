// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.utils

import android.view.View.MeasureSpec

/**
 * @Desc: 宽高比帮助类
 * @Author: jiadou
 */
object AspectRatioHelper {
    fun parseAspectRatio(ratioStr: String?): AspectRatioConfig? {
        if (ratioStr.isNullOrBlank() || ratioStr.equals("default", ignoreCase = true)) {
            return null
        }

        val parts = ratioStr.split(",")
        if (parts.size != 2) {
            return null
        }

        val dimension = parts[0].trim().lowercase()
        val ratioParts = parts[1].trim().split(":")
        if (ratioParts.size != 2) {
            return null
        }

        val widthRatio = ratioParts[0].toFloatOrNull()
        val heightRatio = ratioParts[1].toFloatOrNull()

        if (widthRatio == null || heightRatio == null || widthRatio <= 0 || heightRatio <= 0) {
            return null
        }

        return when (dimension) {
            "h" -> AspectRatioConfig.HeightFixed(widthRatio, heightRatio)
            "w" -> AspectRatioConfig.WidthFixed(widthRatio, heightRatio)
            else -> null
        }
    }

    fun getAdjustedMeasureSpecs(
        aspectRatioConfig: AspectRatioConfig?,
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ): Pair<Int, Int> {
        if (aspectRatioConfig == null) {
            return widthMeasureSpec to heightMeasureSpec
        }
        val adjusted = when (aspectRatioConfig) {
            is AspectRatioConfig.WidthFixed -> {
                val widthMode = MeasureSpec.getMode(widthMeasureSpec)
                val widthSize = MeasureSpec.getSize(widthMeasureSpec)
                val heightMode = MeasureSpec.getMode(heightMeasureSpec)

                if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY && widthSize > 0) {
                    val targetHeight = (widthSize.toFloat() * aspectRatioConfig.heightRatio / aspectRatioConfig.widthRatio).toInt()
                    val heightSpec = MeasureSpec.makeMeasureSpec(targetHeight, MeasureSpec.EXACTLY)
                    widthMeasureSpec to heightSpec
                } else {
                    widthMeasureSpec to heightMeasureSpec
                }
            }

            is AspectRatioConfig.HeightFixed -> {
                val heightMode = MeasureSpec.getMode(heightMeasureSpec)
                val heightSize = MeasureSpec.getSize(heightMeasureSpec)
                val widthMode = MeasureSpec.getMode(widthMeasureSpec)

                if (heightMode == MeasureSpec.EXACTLY && widthMode != MeasureSpec.EXACTLY && heightSize > 0) {
                    val targetWidth = (heightSize.toFloat() * aspectRatioConfig.widthRatio / aspectRatioConfig.heightRatio).toInt()
                    val widthSpec = MeasureSpec.makeMeasureSpec(targetWidth, MeasureSpec.EXACTLY)
                    widthSpec to heightMeasureSpec
                } else {
                    widthMeasureSpec to heightMeasureSpec
                }
            }
        }
        return adjusted
    }
}

sealed class AspectRatioConfig(
    val widthRatio: Float,
    val heightRatio: Float,
) {
    class WidthFixed(widthRatio: Float, heightRatio: Float) : AspectRatioConfig(widthRatio, heightRatio)
    class HeightFixed(widthRatio: Float, heightRatio: Float) : AspectRatioConfig(widthRatio, heightRatio)
}

