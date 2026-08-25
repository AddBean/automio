// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.extends

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.TypedValue
import com.hive.utils.GlobalApp
import com.hive.utils.utils.StringUtils
import java.util.Locale

fun getResourcesDensity(): Float {
    return GlobalApp.getContext().run {
        this.resources.displayMetrics.density
    }
}

fun getResourcesDisplayMetrics(): DisplayMetrics {
    return GlobalApp.getContext().run {
        this.resources.displayMetrics
    } ?: Resources.getSystem().displayMetrics
}

fun Int.dp(): Int {
    return GlobalApp.DP * this
}

fun Int.dpf(): Float {
    return (GlobalApp.DP * this).toFloat()
}

fun Int.string(vararg ps: Any?): String {
    return GlobalApp.getString(this, *ps)
}

fun Int.stringArray(): Array<String> {
    return GlobalApp.getStringArray(this)
}

fun Int.color(): Int {
    return GlobalApp.getColor(this)
}

fun Int.colorAlpha(alpha: Float): Int {
    //color int值改成带透明度的颜色
    return this and 0x00ffffff or ((alpha * 255.0f + 0.5f).toInt() shl 24)
}

fun Int.toDrawable(): Drawable? {
    return GlobalApp.getDrawable(this)
}

fun Int.toDimension(): Float {
    return GlobalApp.getDimension(this)
}

fun Int.toDimensionInt(): Int {
    return GlobalApp.getDimension(this).toInt()
}

fun Int.toBitmap(): Bitmap {
    return BitmapFactory.decodeResource(GlobalApp.getResources(), this)
}


fun Double.dpi(): Int {
    return (GlobalApp.DP * this).toInt()
}

fun String.encode(): String {
    return StringUtils.encoding(this)
}

fun String.decode(): String {
    return StringUtils.decoding(this)
}


val Float.dp: Float
    get() = (GlobalApp.DP * this)

fun Float.dp(): Float {
    return GlobalApp.DP * this
}

fun Double.dp(): Double {
    return GlobalApp.DP * this
}

fun Float.dpi(): Int {
    return (GlobalApp.DP * this).toInt()
}

val Int.dp: Int
    get() = (this * getResourcesDensity()).toInt()


val Int.dpf: Float
    get() = (this * getResourcesDensity())

val Float.dpf: Float
    get() = (this * getResourcesDensity())

val Double.dpf: Float
    get() = (this * getResourcesDensity()).toFloat()

val Double.dp: Int
    get() = (this * getResourcesDensity()).toInt()

val Double.dpd: Double
    get() = this * getResourcesDensity()

/**
 * 将 SP 值转换为 PX 值
 */
val Int.sp: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        getResourcesDisplayMetrics(),
    ).toInt()

val Float.sp: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this,
        getResourcesDisplayMetrics(),
    )

fun Int.toast(vararg ps: Any?) {
    toastShort(*ps)
}


fun Int.toastShort(vararg ps: Any?) {
    this.string(*ps).toastShort()
}

fun Int.toastLong(vararg ps: Any?) {
    this.string(*ps).toastLong()
}

fun Int.toReadableCount(locale: Locale = Locale.getDefault()): String {
    return StringUtils.formatReadableCount(locale, this)
}

fun Long.toReadableCount(locale: Locale = Locale.getDefault()): String {
    return StringUtils.formatReadableCount(locale, this)
}

fun Int?.toReadableCount(locale: Locale = Locale.getDefault()): String {
    return (this ?: 0).toReadableCount(locale)
}

fun Long?.toReadableCount(locale: Locale = Locale.getDefault()): String {
    return (this ?: 0L).toReadableCount(locale)
}
