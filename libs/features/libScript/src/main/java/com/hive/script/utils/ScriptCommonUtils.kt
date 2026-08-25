// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import com.hive.utils.GlobalApp
import dalvik.system.DexFile
import dalvik.system.PathClassLoader
import kotlin.random.Random
import kotlin.reflect.KClass

object ScriptCommonUtils {

    fun getShortUUID(): String {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8)
    }

    fun <T : Annotation> scanClass(
        ctx: Context,
        packageName: String,
        annotation: KClass<T>
    ): MutableList<Class<*>> {
        val classes: MutableList<Class<*>> = ArrayList()
        try {
            val classLoader = Thread
                .currentThread().contextClassLoader as PathClassLoader
            val dex = DexFile(ctx.packageResourcePath)
            val entries = dex.entries()
            while (entries.hasMoreElements()) {
                val entryName = entries.nextElement()
                if (entryName.contains(packageName)) {
                    val entryClass = Class.forName(
                        entryName,
                        true,
                        classLoader
                    )
                    val annotationEntity = entryClass.getAnnotation(annotation.java)
                    if (annotationEntity != null) {
                        classes.add(entryClass)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return classes
    }

    fun <T : Annotation> scanClass(ctx: Context, annotation: KClass<T>): MutableList<Class<*>> {
        val entityPackage = annotation.java.`package`?.name
        val classes: MutableList<Class<*>> = ArrayList()
        try {
            val classLoader = Thread
                .currentThread().contextClassLoader as PathClassLoader
            val dex = DexFile(ctx.packageResourcePath)
            val entries = dex.entries()
            while (entries.hasMoreElements()) {
                val entryName = entries.nextElement()
                if (entryName.contains(entityPackage!!)) {
                    val entryClass = Class.forName(
                        entryName,
                        true,
                        classLoader
                    )
                    val annotationEntity = entryClass.getAnnotation(annotation.java)
                    if (annotationEntity != null) {
                        classes.add(entryClass)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return classes
    }


    fun getRandomValue(value: Int, random: Int): Int {
        if (random == 0) {
            return value
        }
        val randomPx = (GlobalApp.DP * random / 2f).toInt()
        val r = Random.nextInt(randomPx) - (randomPx / 2)
        return value + r
    }

    fun getRandomDuration(startDuration: Long, endDuration: Long): Long {
        return if (startDuration == endDuration) {
            startDuration
        } else {
            (startDuration..endDuration).random()
        }
    }

    fun isFloatRectOrigin(rectF: RectF): Boolean {
        return rectF.left == 0f && rectF.top == 0f && rectF.right == 1f && rectF.bottom == 1f
    }

    fun covertToScreenRect(rectF: RectF): Rect {
        val w = ScriptCoordinateAdapter.getScreenWidthByOrientation()
        val h = ScriptCoordinateAdapter.getScreenHeightByOrientation()

        return Rect(
            (rectF.left * w).toInt(),
            (rectF.top * h).toInt(),
            (rectF.right * w).toInt(),
            (rectF.bottom * h).toInt()
        )
    }

    fun covertToRect(rectF: RectF): Rect {
        return covertToRect(
            rectF,
            ScriptCoordinateAdapter.getScreenWidthByOrientation(),
            ScriptCoordinateAdapter.getScreenHeightByOrientation()
        )
    }


    fun covertToRect(rectF: RectF, w: Int, h: Int): Rect {
        return Rect(
            (rectF.left * w).toInt(),
            (rectF.top * h).toInt(),
            (rectF.right * w).toInt(),
            (rectF.bottom * h).toInt()
        )
    }


    fun convertToNormalization(rectF: RectF): RectF {
        return convertToNormalization(
            rectF,
            ScriptCoordinateAdapter.getScreenWidthByOrientation(),
            ScriptCoordinateAdapter.getScreenHeightByOrientation()
        )
    }

    fun convertToNormalization(rectF: RectF, width: Int, height: Int): RectF {
        val rect = RectF(
            rectF.left / width,
            rectF.top / height,
            rectF.right / width,
            rectF.bottom / height
        )
        //检查归一化数据是否合法
        if (rect.left < 0) {
            rect.left = 0f
        }
        if (rect.left > 1f) {
            rect.left = 1f
        }
        if (rect.top < 0) {
            rect.top = 0f
        }
        if (rect.top > 1f) {
            rect.top = 1f
        }
        if (rect.right < 0) {
            rect.right = 0f
        }
        if (rect.right > 1f) {
            rect.right = 1f
        }
        if (rect.bottom < 0) {
            rect.bottom = 0f
        }
        if (rect.bottom > 1f) {
            rect.bottom = 1f
        }
        return rect
    }

    fun convertToLocation(rectF: RectF, width: Int, height: Int): RectF {
        val rect = RectF(
            rectF.left * width,
            rectF.top * height,
            rectF.right * width,
            rectF.bottom * height
        )
        return rect
    }

    /**
     * clickType:0:自上而下点击 1:自下而上点击 2:自左而右点击 3:自右而左点击 4:随机点击(不重复)
     */
    fun forEachRect(
        mRectF: RectF,
        clickType: Int,
        hrz: Int,
        ver: Int,
        action: (Float, Float) -> Unit
    ) {
        when (clickType) {
            0 -> {
                forEachUpDown(mRectF, hrz, ver) { x, y ->
                    action.invoke(x, y)
                }
            }

            1 -> {
                forEachDownUp(mRectF, hrz, ver) { x, y ->
                    action.invoke(x, y)
                }
            }

            2 -> {
                forEachLeftRight(mRectF, hrz, ver) { x, y ->
                    action.invoke(x, y)
                }
            }

            3 -> {
                forEachRightLeft(mRectF, hrz, ver) { x, y ->
                    action.invoke(x, y)
                }
            }

            4 -> {
                forEachRandom(mRectF, hrz, ver) { x, y ->
                    action.invoke(x, y)
                }
            }

            5 -> {
                forEachOblique(mRectF, hrz, ver) { x, y ->
                    action.invoke(x, y)
                }
            }
        }
    }

    private fun forEachUpDown(
        mRectF: RectF,
        hrz: Int,
        ver: Int,
        action: (Float, Float) -> Unit
    ) {
        val xStep = mRectF.width() / hrz
        val yStep = mRectF.height() / ver
        val xStart = mRectF.left + xStep / 2
        val yStart = mRectF.top + yStep / 2
        for (i in 0 until hrz) {
            for (j in 0 until ver) {
                action.invoke(
                    xStart + i * xStep, yStart + j * yStep
                )
            }
        }
    }

    private fun forEachDownUp(
        mRectF: RectF,
        hrz: Int,
        ver: Int,
        action: (Float, Float) -> Unit
    ) {
        val xStep = mRectF.width() / hrz
        val yStep = mRectF.height() / ver
        val xStart = mRectF.left + xStep / 2
        val yStart = mRectF.top + yStep / 2
        for (i in 0 until hrz) {
            for (j in 0 until ver) {
                action.invoke(
                    xStart + i * xStep, yStart + (ver - j - 1) * yStep
                )
            }
        }
    }


    private fun forEachLeftRight(
        mRectF: RectF,
        hrz: Int,
        ver: Int,
        action: (Float, Float) -> Unit
    ) {
        val xStep = mRectF.width() / hrz
        val yStep = mRectF.height() / ver
        val xStart = mRectF.left + xStep / 2
        val yStart = mRectF.top + yStep / 2
        for (j in 0 until ver) {
            for (i in 0 until hrz) {
                action.invoke(
                    xStart + i * xStep, yStart + j * yStep
                )
            }
        }
    }

    private fun forEachRightLeft(
        mRectF: RectF,
        hrz: Int,
        ver: Int,
        action: (Float, Float) -> Unit
    ) {
        val xStep = mRectF.width() / hrz
        val yStep = mRectF.height() / ver
        val xStart = mRectF.left + xStep / 2
        val yStart = mRectF.top + yStep / 2
        for (j in 0 until ver) {
            for (i in 0 until hrz) {

                action.invoke(
                    xStart + (hrz - i - 1) * xStep, yStart + j * yStep
                )
            }
        }
    }


    private fun forEachRandom(
        mRectF: RectF,
        hrz: Int,
        ver: Int,
        action: (Float, Float) -> Unit
    ) {
        val xStep = mRectF.width() / hrz
        val yStep = mRectF.height() / ver
        val xStart = mRectF.left + xStep / 2
        val yStart = mRectF.top + yStep / 2
        val list = ArrayList<Pair<Int, Int>>()
        for (i in 0 until hrz) {
            for (j in 0 until ver) {
                list.add(Pair(i, j))
            }
        }
        list.shuffle()
        for (pair in list) {
            action.invoke(
                xStart + pair.first * xStep, yStart + pair.second * yStep
            )
        }
    }

    private fun forEachOblique(
        mRectF: RectF,
        hrz: Int,
        ver: Int,
        action: (Float, Float) -> Unit
    ) {
        val xStep = mRectF.width() / hrz
        val yStep = mRectF.height() / ver
        val xStart = mRectF.left + xStep / 2
        val yStart = mRectF.top + yStep / 2

        for (sum in 0 until hrz + ver - 1) {
            for (i in 0..sum) {
                val j = sum - i
                if (i < hrz && j < ver) {
                    val x = xStart + i * xStep
                    val y = yStart + j * yStep
                    action.invoke(x, y)
                }
            }
        }
    }
}
