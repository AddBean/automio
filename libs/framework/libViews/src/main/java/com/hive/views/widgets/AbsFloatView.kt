// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.hive.utils.system.CommonUtils
import kotlin.math.abs

abstract class AbsFloatView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    protected var DP: Int = CommonUtils.dipToPx(context, 1)
    private var mViewWidth: Int = 0
    private var mViewHeight: Int = 0
    private var mParentHeight: Int = 0
    private var mParentWidth: Int = 0
    private var mGestureDetector: GestureDetector
    private var nx: Int = 0
    private var ny: Int = 0
    private var mAnimator: ValueAnimator
    private var mAnimTargetX: Float = 0f
    private var mAnimTargetY: Float = 0f
    private var mUpX: Float = 0f
    private var mUpY: Float = 0f
    private var mDownX: Float = 0f
    private var mDownY: Float = 0f
    protected open var PADING_DISTANT: Int = 8
    protected var PADDING_TOP: Int = 0;
    protected open var PADDING_BOTTOM: Int = 48 * DP
    protected open var ANIM_DURATION: Long = 300//动画时间
    protected open var BACK_DISTANCE: Int = 90//离开手指多远回到原点，单位DP

    init {
        mGestureDetector = GestureDetector(getContext(), GestureHandler())
        mAnimator = ValueAnimator.ofFloat(0f, 1f)
        mAnimator.duration = ANIM_DURATION
        mAnimator.interpolator = DecelerateInterpolator()
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        return true
    }

    private var hasMove = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        mGestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                super.dispatchTouchEvent(event)
                requestDisallowInterceptTouchEvent(true)
                nx = event.rawX.toInt()
                ny = event.rawY.toInt()
                mDownX = translationX
                mDownY = translationY
                hasMove = false
            }

            MotionEvent.ACTION_MOVE -> {
                val nowX = event.rawX.toInt()
                val nowY = event.rawY.toInt()
                val movedX = nowX - nx
                val movedY = nowY - ny
                if (abs(movedX) > 4 * DP) {
                    hasMove = true
                }
                nx = nowX
                ny = nowY
                translationX += getMoveX(movedX)
                translationY += getMoveY(movedY)
            }

            MotionEvent.ACTION_UP -> {
                if (!hasMove) {
                    super.dispatchTouchEvent(event)
                }
                mUpX = translationX
                mUpY = translationY
                backToEdge()
            }
        }
        return true
    }

    open fun getMoveX(movedX: Int): Int {
        if (translationX + movedX < 0) {
            return -translationX.toInt()
        }
        if (translationX + movedX + width > mParentWidth) {
            return mParentWidth - width - translationX.toInt()
        }
        return movedX
    }

    open fun getMoveY(movedY: Int): Int {

        if (translationY + movedY < 0) {
            return -translationY.toInt()
        }
        if (translationY + movedY + height > mParentHeight) {
            return mParentHeight - height - translationY.toInt()
        }
        return movedY
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val pw = (parent.parent as View).width
        val ph = (parent.parent as View).height - PADDING_BOTTOM
        mParentWidth = pw
        mParentHeight = ph
        //只会调用一次
        if (ph > 0 && mViewHeight == 0 && measuredHeight > 0) {
            mViewHeight = measuredHeight
            mViewWidth = measuredWidth
            val startLoc = getStartPosition(pw, ph)
            translationX = startLoc.x.toFloat()
            translationY = startLoc.y.toFloat()
        }
    }

    abstract fun getStartPosition(pw: Int, ph: Int): Point

    /**
     * 回到边界
     */
    protected fun backToEdge() {
        if (mAnimator.isRunning) {
            mAnimator.cancel()
        }
        mUpX = translationX
        mUpY = translationY
        setTargetPosition()
        mAnimator.addUpdateListener {
            val value = it.animatedValue as Float
            translationX = mUpX + (mAnimTargetX - mUpX) * value
            translationY = mUpY + (mAnimTargetY - mUpY) * value
        }
        mAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                onEdgeAnimationEnd()
            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }

        })
        mAnimator.start()
    }

    open fun onEdgeAnimationEnd() {

    }

    /**
     * 设置动画目标位置
     */
    protected fun setTargetPosition() {
        //回原点
//        var dx = (mDownX - mUpX).toDouble()
//        var dy = (mDownY - mUpY).toDouble()
//        var d = sqrt(dx * dx + dy * dy)
//        if (d < BACK_DISTANCE * DP) {
//            mAnimTargetX = mDownX
//            mAnimTargetY = mDownY
//            return
//        }
        mAnimTargetX = when (mUpX > (mParentWidth / 2 - width / 2)) {
            true -> (mParentWidth - width - PADING_DISTANT * DP).toFloat()
            false -> 0f + PADING_DISTANT * DP
        }
        mAnimTargetY = mUpY
    }

    open fun onViewClicked() {}

    fun onViewLongPress() {}


    inner class GestureHandler : GestureDetector.OnGestureListener {
        override fun onShowPress(e: MotionEvent) {

        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onViewClicked()
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            onViewLongPress()
        }
    }


}