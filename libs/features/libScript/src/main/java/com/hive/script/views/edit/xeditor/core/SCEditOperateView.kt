// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.core

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import com.blankj.utilcode.util.ThreadUtils
import com.hive.script.extensions.copyTo
import com.hive.script.views.edit.xeditor.core.gesture.SCRotateGestureDetector
import com.hive.script.views.edit.xeditor.core.gesture.SCScaleGestureDetector
import com.hive.script.views.edit.xeditor.core.model.SCDrawEditLayerModel
import com.hive.script.views.edit.xeditor.core.model.SCDrawEditRect
import com.hive.script.views.edit.xeditor.utils.XEditorDrawHelper
import com.hive.script.views.edit.xeditor.utils.XEditorHelper
import com.hive.script.views.edit.xeditor.utils.XEditorRenderView
import com.hive.utils.GlobalApp
import java.util.Collections.synchronizedList

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 5/6/21
 */
open class SCEditOperateView(context: Context?, attrs: AttributeSet?) :
    XEditorRenderView(context, attrs) {

    private var valueAnimator: ValueAnimator? = null

    var mVirtualEditLayerModel = SCDrawEditLayerModel()

    var mOnTouchDrawEventListener: OnTouchDrawEventListener? = null

    var isSnapShotMode = false

    protected val mLayerRect: SCDrawEditRect = SCDrawEditRect()

    private var inScaleMode: Boolean = false

    private var mVirtualEditTouchEventHelper = SCEditTouchEventHelper(context!!)

    private var mTargetChildView: SCAbsLayerItemView? = null

    private var mChildViews = synchronizedList(mutableListOf<SCAbsLayerItemView>())

    private var mTouchPointRecord = synchronizedList(arrayListOf<PointF>())

    private var mClipChild = false

    private var mEnableLayerTranslate = true

    private var mEnableLayerScale = true

    private var mEnableLayerRotate = false

    private var layerCenter1: PointF = PointF()

    private var layerCenter2: PointF = PointF()

    private val targetAnimViews = synchronizedList(mutableListOf<SCAbsLayerItemView>())

    private val renderFrameRectF = SCDrawEditRect()

    private val debugPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 2f * GlobalApp.DP
    }


    init {
        mVirtualEditTouchEventHelper.registerEventListener(object : SCEditTouchEventListener() {

            override fun onLongPress(e: MotionEvent) {
                val ne = getTransformEvent(e)
                for (i in mChildViews.size - 1 downTo 0) {
                    if (mChildViews[i].isRectContains(ne.x, ne.y)) {
                        mChildViews[i].onLongClick(ne)
                        return
                    }
                }
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (!XEditorHelper.editMode) {
                    return false
                }
                val ne = getTransformEvent(e)
                for (i in mChildViews.size - 1 downTo 0) {

                    if (mChildViews[i].isRectContains(ne.x, ne.y)) {
                        mChildViews[i].onClick(ne)
                        return true
                    }
                }
                return false
            }

            override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
                return false
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (mEnableLayerTranslate && !isInTouchMove) {
                    translateLayer(distanceX, distanceY)
                }
                return true
            }

            override fun onScaleBegin(detector: SCScaleGestureDetector?): Boolean {
                if (!mEnableLayerScale) return false
                inScaleMode = true
                return true
            }

            override fun onScale(detector: SCScaleGestureDetector?): Boolean {
                detector?.run {
                    val center = getTouchCenterInScreen()
                    center?.let {
                        scaleLayer(scaleFactor, scaleFactor, center)
                    }
                }
                return true
            }

            override fun onScaleEnd(detector: SCScaleGestureDetector?) {
                super.onScaleEnd(detector)
                inScaleMode = false
            }

            override fun onRotateBegin(detector: SCRotateGestureDetector?): Boolean {
                return mEnableLayerRotate
            }

            override fun onRotate(detector: SCRotateGestureDetector?): Boolean {
                detector?.run {
                    val center = getTouchCenterInScreen()
                    center?.let {
                        rotateLayer(rotationDegreesDelta, center)
                    }
                }

                return true
            }
        })
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mVirtualEditLayerModel.mOriginRect.lt = PointF(0f, 0f)
        mVirtualEditLayerModel.mOriginRect.rt = PointF(w.toFloat(), 0f)
        mVirtualEditLayerModel.mOriginRect.rb = PointF(w.toFloat(), h.toFloat())
        mVirtualEditLayerModel.mOriginRect.lb = PointF(0f, h.toFloat())
        invalidate()
    }


    fun getTouchCenterInScreen(): PointF? {
        var centerPointF: PointF? = null
        if (mTouchPointRecord.size > 1) {
            centerPointF = PointF()
            centerPointF.x = (mTouchPointRecord[0].x + mTouchPointRecord[1].x) / 2
            centerPointF.y = (mTouchPointRecord[0].y + mTouchPointRecord[1].y) / 2
        }

        return centerPointF
    }

    open fun onTouchMove(transEvent: MotionEvent, originEvent: MotionEvent) {
    }

    fun getChildCount() = mChildViews.size

    fun getChildList() = mChildViews

    fun addChildView(itemView: SCAbsLayerItemView) {
        itemView.mParentView = this
        itemView.measureChild()
        mChildViews.add(itemView)
        notifyChildChanged()
    }

    fun addChildView(index: Int, itemView: SCAbsLayerItemView) {
        itemView.mParentView = this
        itemView.measureChild()
        mChildViews.add(index, itemView)
        notifyChildChanged()
    }

    fun removeAllView() {
        mChildViews.clear()
        notifyChildChanged()
    }

    fun removeChildView(itemView: SCAbsLayerItemView) {
        mChildViews.remove(itemView)
        notifyChildChanged()
    }

    fun removeChildView(index: Int) {
        mChildViews.removeAt(index)
        notifyChildChanged()
    }

    private fun notifyChildChanged() {
        postInvalidate()
    }

    override fun onRender(canvas: Canvas?) {
        try {
            checkAndStartAnimation()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        drawView(canvas)
    }

    private val transformEditRect = SCDrawEditRect()

    @Synchronized
    fun drawView(canvas: Canvas?) {
        canvas?.run {
            save()
            canvas.save()
            val matrix = mVirtualEditLayerModel.getTransformMatrix()
            canvas.setMatrix(matrix)
            renderFrameRectF.copyTo(transformEditRect)
            mVirtualEditLayerModel.inverseTransform(transformEditRect)
            onDrawBackground(canvas, transformEditRect)
//            if (BuildConfig.DEBUG) {
//                canvas.drawRect(transformEditRect.toRectF(), debugPaint)
//            }
            canvas.restore()
            onItemDrawBefore(canvas)
            try {
                for (i in mChildViews.indices) {
                    if (isItemVisibleToUser(
                            mChildViews[i],
                            transformEditRect
                        ) && mChildViews[i].visibility
                    ) {
                        save()
                        canvas.setMatrix(matrix)
                        mChildViews[i].draw(canvas)
                        restore()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onItemDrawAfter(canvas)
            restore()
        }
    }

    private fun isItemVisibleToUser(
        itemView: SCAbsLayerItemView,
        transformEditRect: SCDrawEditRect
    ): Boolean {
        val rectF = itemView.getRenderRect() ?: return true
        val sc = transformEditRect.toRectF()
        return sc.contains(rectF) || sc.intersect(rectF) || isSnapShotMode
    }

    open fun onItemDrawBefore(canvas: Canvas) {

    }

    open fun onItemDrawAfter(canvas: Canvas) {

    }


    private fun findAnimViews(): List<SCAbsLayerItemView> {
        targetAnimViews.clear()
        mChildViews.forEach {
            if (it.animCurrentDuration > 0) {
                targetAnimViews.add(it)
            }
        }
        return targetAnimViews
    }

    @Synchronized
    fun checkAndStartAnimation() {
        val animViews = findAnimViews()
        if (animViews.isNotEmpty() && (valueAnimator?.isRunning == true).not()) {
            startItemsAnimation(animViews)
            postInvalidate()
        }
    }

    private fun startItemsAnimation(animViews: List<SCAbsLayerItemView>) {
        val maxDuration = animViews.maxOf { it.animDuration }
        if (maxDuration <= 0) return
        valueAnimator = ValueAnimator.ofFloat(0f, maxDuration)
        valueAnimator?.setInterpolator { input -> input }
        valueAnimator?.duration = maxDuration.toLong()
        valueAnimator?.addUpdateListener { animTime ->
            val list = findAnimViews()
            list.forEach {
                if (it.animDuration > 0) {
                    it.animCurrentDuration = it.animDuration - animTime.animatedValue as Float
                    if (it.animCurrentDuration < 0f)
                        it.animCurrentDuration = 0f
                    postInvalidate()
                }
            }
            if (list.isEmpty()) {
                valueAnimator?.cancel()
            }
        }
        valueAnimator?.start()

    }


    @Synchronized
    fun saveLayerTransform() {
        mVirtualEditLayerModel.save()
    }

    @Synchronized
    fun restoreLayerTransform() {
        mVirtualEditLayerModel.restore()
    }

    @Synchronized
    fun requestReLayout() {
        mChildViews.sortBy { it.indexZ }
        ensureUiThread {
            invalidate()
        }
    }

    private fun ensureUiThread(block: () -> Unit) {
        if (ThreadUtils.isMainThread()) {
            block()
        } else {
            post {
                block()
            }
        }
    }

    open fun onItemEvent(itemView: SCAbsLayerItemView, eventData: Any?, eventData2: Any?) {

    }

    open fun onDrawBackground(canvas: Canvas, rect: SCDrawEditRect) {
        XEditorDrawHelper.drawDotGridBackground(
            canvas,
            rect.toRectF(),
            mVirtualEditLayerModel.mLayerScaleX
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        try {
            if (isInTouchMove) {
                onTouchMove(getTransformEvent(event!!), event)
                return true
            } else {
                event?.run {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            if (mChildViews.size > 0) {
                                for (i in mChildViews.size - 1 downTo 0) {
                                    if (mChildViews[i].dispatchTouchEvent(getTransformEvent(event))) {
                                        mTargetChildView = mChildViews[i]
                                        selectTargetChildView()
                                        return true
                                    }
                                }
                            }
                            mTargetChildView = null
                            selectTargetChildView()
                        }

                        else -> {
                            mTargetChildView?.dispatchTouchEvent(getTransformEvent(event))
                        }
                    }
                    if (mTargetChildView == null) {
                        onTouchEditPanel(event)
                    }
                }
                return true
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return true
        }
    }

    private fun getTransformEvent(event: MotionEvent): MotionEvent {
        val eventCopy = MotionEvent.obtain(event)
        val p = mVirtualEditLayerModel.inverseTransform(PointF(event.x, event.y))
        eventCopy.setLocation(
            p.x,
            p.y
        )
        return eventCopy
    }

    /**
     * 选中childView
     */
    private fun selectTargetChildView() {
        mChildViews.forEach {
            it.onSelectedChanged(it == mTargetChildView)
        }
        invalidate()
    }


    /**
     * item不处理touch event，会交给这里
     */
    private fun onTouchEditPanel(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mTouchPointRecord.clear()
                mTouchPointRecord.add(PointF(event.x, event.y))
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.getPointerId(event.pointerCount - 1)
                mTouchPointRecord.add(PointF(event.getX(index), event.getY(index)))
            }
        }
        mVirtualEditTouchEventHelper.onTouchEvent(event)
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        renderFrameRectF.set(RectF(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat()))
    }

    /**
     * 移动图层
     */
    fun translateLayer(dx: Float, dy: Float) {
        mVirtualEditLayerModel.mLayerTransX =
            mVirtualEditLayerModel.mLayerTransX - dx / mVirtualEditLayerModel.mLayerScaleX
        mVirtualEditLayerModel.mLayerTransY =
            mVirtualEditLayerModel.mLayerTransY - dy / mVirtualEditLayerModel.mLayerScaleY

        invalidate()
        mOnTouchDrawEventListener?.onTranslateEvent(
            mVirtualEditLayerModel.mLayerTransX,
            mVirtualEditLayerModel.mLayerTransY
        )
    }

    /**
     * 旋转图层
     */
    private fun rotateLayer(rotationDegreesDelta: Float, touchCenterInScreen: PointF) {
        mVirtualEditLayerModel.mLayerAngle -= rotationDegreesDelta
        invalidate()
    }

    /**
     * 放大图层
     * 放大图层时，以双指中心点来放大，因为图层可能经过放大/缩放/位移变换，因此求改变换的逆矩阵，以此来计算手指的实际坐标。
     */
    private fun scaleLayer(scaleX: Float, scaleY: Float, touchCenterInScreen: PointF) {

        val inverseCenter = mVirtualEditLayerModel.inverseTransform(touchCenterInScreen)

        layerCenter1 = touchCenterInScreen

        mVirtualEditLayerModel.mLayerScaleX = mVirtualEditLayerModel.mLayerScaleX * scaleX
        mVirtualEditLayerModel.mLayerScaleY = mVirtualEditLayerModel.mLayerScaleY * scaleY

        layerCenter2 = mVirtualEditLayerModel.getTransformPosition(inverseCenter)

        mVirtualEditLayerModel.mLayerTransX -= (layerCenter2.x - layerCenter1.x) / mVirtualEditLayerModel.mLayerScaleX
        mVirtualEditLayerModel.mLayerTransY -= (layerCenter2.y - layerCenter1.y) / mVirtualEditLayerModel.mLayerScaleY

        invalidate()

        mOnTouchDrawEventListener?.onTranslateEvent(
            mVirtualEditLayerModel.mLayerTransX,
            mVirtualEditLayerModel.mLayerTransY
        )

        mOnTouchDrawEventListener?.onScaleEvent(
            mVirtualEditLayerModel.mLayerScaleX,
            mVirtualEditLayerModel.mLayerScaleX
        )
    }

    interface OnTouchDrawEventListener {

        fun onTranslateEvent(x: Float, y: Float)

        fun onScaleEvent(scaleX: Float, scaleY: Float)
    }

    companion object {
        var isInTouchMove = false
    }
}