// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.extensions

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Region
import com.hive.script.views.edit.xeditor.core.model.SCDrawEditRect

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 5/7/21
 */


fun Matrix.mapEditRect(srcRect: SCDrawEditRect) {
    val dst = floatArrayOf(0f, 0f)

    var src = floatArrayOf(srcRect.lt.x, srcRect.lt.y)
    this.mapPoints(dst, src)
    srcRect.lt.x = dst[0]
    srcRect.lt.y = dst[1]

    src = floatArrayOf(srcRect.rt.x, srcRect.rt.y)
    this.mapPoints(dst, src)
    srcRect.rt.x = dst[0]
    srcRect.rt.y = dst[1]


    src = floatArrayOf(srcRect.rb.x, srcRect.rb.y)
    this.mapPoints(dst, src)
    srcRect.rb.x = dst[0]
    srcRect.rb.y = dst[1]

    src = floatArrayOf(srcRect.lb.x, srcRect.lb.y)
    this.mapPoints(dst, src)
    srcRect.lb.x = dst[0]
    srcRect.lb.y = dst[1]
}

fun Matrix.mapPointF(dstPointF: PointF, srcPointF: PointF) {
    val dst = floatArrayOf(0f, 0f)

    val src = floatArrayOf(srcPointF.x, srcPointF.y)
    this.mapPoints(dst, src)
    dstPointF.x = dst[0]
    dstPointF.y = dst[1]
}


fun SCDrawEditRect.copyTo(rect: SCDrawEditRect) {
    rect.rt.x = rt.x
    rect.rt.y = rt.y

    rect.lt.x = lt.x
    rect.lt.y = lt.y

    rect.rb.x = rb.x
    rect.rb.y = rb.y

    rect.lb.x = lb.x
    rect.lb.y = lb.y
}

fun SCDrawEditRect.findCenter(): PointF {
    return PointF((lt.x + rb.x) / 2f, (lt.y + rb.y) / 2f)
}

//fun SCDrawEditRect.contains(x: Float, y: Float): Boolean {
//    val region = path2Region(toPath(null))
//    return region.contains(x.toInt(), y.toInt())
//}
fun SCDrawEditRect.contains(x: Float, y: Float): Boolean {
    return toRect().contains(x.toInt(), y.toInt())
}

fun path2Region(path: Path): Region {
    val rectF = RectF()
    path.computeBounds(rectF, true)
    val region = Region()
    region.setPath(
        path,
        Region(rectF.left.toInt(), rectF.top.toInt(), rectF.right.toInt(), rectF.bottom.toInt())
    )
    return region
}

fun SCDrawEditRect.toPath(path: Path?): Path {
    var p = path
    if (p == null) p = Path()
    p.reset()
    p.moveTo(lt.x, lt.y)
    p.lineTo(rt.x, rt.y)
    p.lineTo(rb.x, rb.y)
    p.lineTo(lb.x, lb.y)
    p.close()
    return p
}

fun SCDrawEditRect.toSplitLines(p: MutableList<SplitLine>, numberColumn: Int, numberRow: Int) {

    val width = (rt.x - lt.x) / numberColumn
    val height = (lb.y - lt.y) / numberRow
    for (i in 0 until numberColumn) {
        val start = PointF(lt.x + width * i, lt.y)
        val end = PointF(lt.x + width * i, lb.y)
        p.add(SplitLine(start, end))
    }
    for (i in 0 until numberRow) {
        val start = PointF(lt.x, lt.y + height * i)
        val end = PointF(rt.x, lt.y + height * i)
        p.add(SplitLine(start, end))
    }
}


data class SplitLine(var start:PointF,var end:PointF)




