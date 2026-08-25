// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.core

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.view.MotionEvent
import com.hive.script.base.ScriptCommand
import com.hive.script.extensions.contains
import com.hive.script.views.edit.xeditor.XCellModel
import com.hive.script.views.edit.xeditor.core.model.SCDrawEditItemModel
import com.hive.script.views.edit.xeditor.core.model.SCDrawEditRect
import com.hive.script.views.edit.xeditor.utils.XEditorHelper
import com.hive.utils.utils.DensityUtil

/**
 *
 * @author jiadou
 * @date 5/6/21
 */
abstract class SCAbsLayerItemView(context: Context) {
    var dp = DensityUtil.dip2px(1f)

    var isInTouchSelected = false

    protected var mEditModel = SCDrawEditItemModel()

    private var mInTouchMode = false

    var isInTouchMove = false

    var visibility = true

    var mItemRect = SCDrawEditRect()

    var mParentView: SCEditOperateView? = null

    var indexZ = 0

    var animCurrentDuration = 0f

    var animType: String? = "default"

    var animDuration = 0f
        set(value) {
            animCurrentDuration = value
            field = value
        }


    fun measureChild() {
        mParentView?.mVirtualEditLayerModel?.let {
            mEditModel.layerModel = it
        }
        onMeasure(mItemRect)
    }

    protected fun isNormalSize(): Boolean {
        return (mParentView?.mVirtualEditLayerModel?.mLayerScaleX ?: 1f) > 0.15
    }

    /**
     * 开始动画，duration为动画时长，当开始动画时，会回调用onDrawAnim方法
     */
    fun startAnim(duration: Float, type: String?) {
        animType = type
        animDuration = duration
        mParentView?.checkAndStartAnimation()
        requestInvalidate()
    }

    fun startAnim(duration: Float) {
        startAnim(duration, "default")
    }


    open fun getMainCell(): XCellModel? = null

    open fun onDataRefresh(cmd: ScriptCommand?) {}

    abstract fun onMeasure(rect: SCDrawEditRect)

    fun requestMeasure() {
        onMeasure(mItemRect)
    }

    fun draw(canvas: Canvas) {
        if (!visibility) {
            return
        } else {
            if (animCurrentDuration > 0) {
                onDrawAnim(canvas, 1f - animCurrentDuration / animDuration, animType)
            } else {
                onDraw(canvas)
            }
        }
    }

    /**
     * 绘制动画
     */
    open fun onDrawAnim(canvas: Canvas, animPercent: Float, type: String?) {
    }

    /**
     * 绘制视图
     */
    abstract fun onDraw(canvas: Canvas)

    abstract fun getRenderRect(): RectF?

    open fun onTouchEvent(event: MotionEvent): Boolean = false

    open fun onSelectedChanged(isSelected: Boolean) {}

    fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (!XEditorHelper.editMode) {
            return false
        }
        if (!visibility) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mInTouchMode = false
                if (isRectContains(event.x, event.y)) {
                    mInTouchMode = true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mInTouchMode = false
            }
        }
        return onTouchEvent(event)
    }

    open fun onClick(event: MotionEvent) {

    }

    open fun onLongClick(event: MotionEvent) {

    }

    open fun postEvent(eventData: Any?) {
        mParentView?.onItemEvent(this, eventData, null)
    }

    open fun postEvent(eventData: Any?, eventData2: Any?) {
        mParentView?.onItemEvent(this, eventData, eventData2)
    }

    /**
     * 是否包含点
     */
    open fun isRectContains(x: Float, y: Float): Boolean = mItemRect.contains(x, y)

    protected fun requestParentInvalidate() {
        mParentView?.invalidate()
    }

    protected fun requestInvalidate() {
        val r = mItemRect.toRect()
        mParentView?.postInvalidate(r.left, r.top, r.right, r.bottom)
    }

}