// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.extensions

import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.view.View
import com.hive.script.R
import com.hive.script.base.core.LegacyScriptCipher
import com.hive.script.views.beans.PointVectorInt
import com.hive.utils.GlobalApp
import com.hive.utils.encrypt.Md5Utils
import com.hive.utils.utils.StringUtils

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2021/10/24
 */
val arrBmp = BitmapFactory.decodeResource(GlobalApp.getResources(), R.drawable.sc_ic_arr)

val matrix = Matrix()

var targetRect = Rect()

fun List<Point>.copyPoints(): List<Point> {
    val list = mutableListOf<Point>()
    this.forEach {
        list.add(Point(it.x, it.y))
    }
    return list
}


/**
 * 将point缩放到rect内
 */
fun List<Point>.scalePointInRect(rect: Rect): List<Point> {
    val padding = 12 * GlobalApp.DP
    val maxX = maxByOrNull { it.x }?.x ?: 0
    val minX = minByOrNull { it.x }?.x ?: 0
    val maxY = maxByOrNull { it.y }?.y ?: 0
    val minY = minByOrNull { it.y }?.y ?: 0

    targetRect.set(minX - padding, minY - padding, maxX + padding, maxY + padding)

    val scale =
        if (targetRect.width() / targetRect.height().toFloat() > rect.width() / rect.height()
                .toFloat()
        ) {
            rect.width().toFloat() / targetRect.width()
        } else {
            rect.height().toFloat() / targetRect.height()
        }
    val floats = Array(size * 2) { 0f }.toFloatArray()
    var i = 0
    forEach {
        floats[i] = it.x.toFloat()
        i++
        floats[i] = it.y.toFloat()
        i++
    }
    matrix.reset()
    matrix.postScale(scale, scale, targetRect.centerX().toFloat(), targetRect.centerY().toFloat())
    matrix.postTranslate(
        (rect.centerX() - targetRect.centerX()).toFloat(),
        (rect.centerY() - targetRect.centerY()).toFloat()
    )
    matrix.mapPoints(floats)
    floats.forEachIndexed { index, fl ->
        val i = index / 2
        if (index % 2 == 0) {
            this[i].x = fl.toInt()
        } else {
            this[i].y = fl.toInt()
        }
    }
    return this
}

fun List<PointVectorInt>.scaleVectorInRect(targetRect: Rect): List<PointVectorInt> {
    val list = this.mapToList {
        mutableListOf(
            Point(it.fromX, it.fromY), Point(
                it.toX,
                it.toY
            )
        )
    }
    val points = list.scalePointInRect(targetRect)
    val listPoints = mutableListOf<PointVectorInt>()
    for (i in points.indices step 2) {
        listPoints.add(
            PointVectorInt(
                points[i].x,
                points[i].y,
                points[i + 1].x,
                points[i + 1].y
            )
        )
    }
    return listPoints
}

fun <R, T> List<T>.mapToList(map: ((it: T) -> List<R>)): List<R> {
    val list = mutableListOf<R>()
    this.forEach {
        list.addAll(map.invoke(it))
    }
    return list
}

fun String.encrypt(): String {
    return LegacyScriptCipher.encrypt(StringUtils.encoding(this))
}

fun String.decrypt(): String {
    return StringUtils.decoding(LegacyScriptCipher.decrypt(this))
}

fun String.encrypt(key: String?): String {
    return if (key != null) {
        val key16 = Md5Utils.string2md5(Md5Utils.string2md5(key)).substring(0, 16)
        LegacyScriptCipher.encrypt(StringUtils.encoding(this), key16)
    } else {
        this.encrypt()
    }
}

fun String.decrypt(key: String?): String {
    return if (key != null) {
        val key16 = Md5Utils.string2md5(Md5Utils.string2md5(key)).substring(0, 16)
        StringUtils.decoding(LegacyScriptCipher.decrypt(this, key16))
    } else {
        this.decrypt()
    }
}

fun View.enable(enable: Boolean) {
    this.isEnabled = enable
    this.alpha = if (enable) 1f else 0.4f
}

fun View.enableAlpha(enable: Boolean) {
    this.alpha = if (enable) 1f else 0.4f
}
