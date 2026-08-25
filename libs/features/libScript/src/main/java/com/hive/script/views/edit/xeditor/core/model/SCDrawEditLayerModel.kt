// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.core.model

import android.graphics.Matrix
import android.graphics.PointF
import com.hive.script.extensions.copyTo
import com.hive.script.extensions.findCenter
import com.hive.script.extensions.mapEditRect
import com.hive.script.extensions.mapPointF

/**
 *
 * @author jiadou
 * @date 5/6/21
 */
class SCDrawEditLayerModel {

    var mLayerAngle: Float = 0f

    var mLayerScaleX: Float = 1f

    var mLayerScaleY: Float = 1f

    var mLayerTransY: Float = 1f

    var mLayerTransX: Float = 0f

    var mOriginRect = SCDrawEditRect()

    var matrix = Matrix()

    var inverseMatrix = Matrix()

    fun getTransformMatrix(): Matrix {
        matrix.reset()
        val p = mOriginRect.findCenter()
        matrix.postRotate(mLayerAngle, p.x, p.y) //画板旋转
        matrix.postTranslate(mLayerTransX, mLayerTransY)  //画板平移
        matrix.postScale(mLayerScaleX, mLayerScaleY)      //画板缩放
        return matrix
    }

    fun transform(srcRect: SCDrawEditRect) {
        mOriginRect.copyTo(srcRect)
        getTransformMatrix().mapEditRect(srcRect)
    }

    /**
     * 根据view上的点，获取其在图层上的坐标
     */
    fun getTransformPosition(originPoint: PointF): PointF {
        val dstPoint = PointF()
        getTransformMatrix().mapPointF(dstPoint, originPoint)
        return dstPoint
    }

    /**
     * 逆变换
     */
    fun inverseTransform(originPoint: PointF): PointF {
        inverseMatrix.reset()
        val dstPoint = PointF()
        getTransformMatrix().invert(inverseMatrix) //求逆
        inverseMatrix.mapPointF(dstPoint, originPoint)
        return dstPoint
    }

    fun inverseTransform(rect: SCDrawEditRect): SCDrawEditRect {
        inverseMatrix.reset()
        getTransformMatrix().invert(inverseMatrix) //求逆
        inverseMatrix.mapEditRect(rect)
        return rect
    }


    fun save() {
        layerModelSave.mLayerAngle = mLayerAngle
        layerModelSave.mLayerScaleX = mLayerScaleX
        layerModelSave.mLayerScaleY = mLayerScaleY
        layerModelSave.mLayerTransY = mLayerTransY
        layerModelSave.mLayerTransX = mLayerTransX
        layerModelSave.mOriginRect = mOriginRect
        layerModelSave.matrix = matrix
        layerModelSave.inverseMatrix = inverseMatrix

        mLayerAngle = 0f

        mLayerScaleX = 1f

        mLayerScaleY = 1f

        mLayerTransY = 1f

        mLayerTransX = 0f

        mOriginRect = SCDrawEditRect()

        matrix = Matrix()

        inverseMatrix = Matrix()

    }

    fun restore() {
        mLayerAngle = layerModelSave.mLayerAngle
        mLayerScaleX = layerModelSave.mLayerScaleX
        mLayerScaleY = layerModelSave.mLayerScaleY
        mLayerTransY = layerModelSave.mLayerTransY
        mLayerTransX = layerModelSave.mLayerTransX
        mOriginRect = layerModelSave.mOriginRect
        matrix = layerModelSave.matrix
        inverseMatrix = layerModelSave.inverseMatrix
    }

    companion object {
        private var layerModelSave = SCDrawEditLayerModel()
    }

}