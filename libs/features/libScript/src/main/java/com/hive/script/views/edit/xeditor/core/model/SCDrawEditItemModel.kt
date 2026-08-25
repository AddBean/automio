// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.core.model

import android.graphics.Matrix
import com.hive.script.extensions.copyTo
import com.hive.script.extensions.findCenter
import com.hive.script.extensions.mapEditRect

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 5/6/21
 */
class SCDrawEditItemModel {

    var layerModel: SCDrawEditLayerModel = SCDrawEditLayerModel()

    var mTransX: Float = 0f

    var mTransY: Float = 0f

    var mScaleX: Float = 1f

    var mScaleY: Float = 1f

    var mAngle: Float = 0f

    var mOriginRect = SCDrawEditRect()

    var matrix = Matrix()


    fun transform(srcRect: SCDrawEditRect) {

        mOriginRect.copyTo(srcRect)

        //平移
        matrix.reset()
        matrix.postTranslate(layerModel.mLayerTransX, layerModel.mLayerTransY) //图层平移
        matrix.postTranslate(mTransX, mTransY) //item平移
        matrix.mapEditRect(srcRect)

        //图层缩放
        matrix.reset()
        matrix.setScale(layerModel.mLayerScaleX, layerModel.mLayerScaleY)
        matrix.mapEditRect(srcRect)


        //item自身缩放
        matrix.reset()
        var cp = srcRect.findCenter()
        matrix.setScale(mScaleX, mScaleY, cp.x, cp.y)
        matrix.mapEditRect(srcRect)

        //item自身旋转
        matrix.reset()
        cp = srcRect.findCenter()
        matrix.setRotate(mAngle, cp.x, cp.y)
        matrix.mapEditRect(srcRect)

        //画布旋转
        matrix.reset()
        val r = SCDrawEditRect()
        layerModel.transform(r)
        val p = r.findCenter()
        matrix.postRotate(layerModel.mLayerAngle, p.x, p.y)
        matrix.mapEditRect(srcRect)

    }


}