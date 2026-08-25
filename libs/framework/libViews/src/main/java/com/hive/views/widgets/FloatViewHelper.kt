// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets

import android.animation.ValueAnimator
import android.graphics.Point
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.hive.utils.GlobalApp
import com.hive.utils.system.CommonUtils

/**
 *
 * @author jiadou
 * @date 5/28/21
 */
class FloatViewHelper : View.OnTouchListener {

    var mOnViewClickListener: OnViewClickListener? = null
    private lateinit var mTargetView: View
    private var mViewWidth: Int = 0
    private var mViewHeight: Int = 0
    private var DP: Int = CommonUtils.dipToPx(GlobalApp.getContext(), 1)
    private var mParentHeight: Int = 0
    private var mParentWidth: Int = 0
    private var mGestureDetector: GestureDetector
    private var x: Int = 0
    private var y: Int = 0
    private var mAnimator: ValueAnimator
    private var mAnimTargetX: Float = 0f
    private var mAnimTargetY: Float = 0f
    private var mUpX: Float = 0f
    private var mUpY: Float = 0f
    private var mDownX: Float = 0f
    private var mDownY: Float = 0f
    private var PADING_DISTANT: Int = 8
    private var PADDING_TOP: Int = 0;
    private var PADDING_BOTTOM: Int = 48 * DP

    companion object {
        val ANIM_DURATION: Long = 300//动画时间
        val BACK_DISTANCE: Int = 90//离开手指多远回到原点，单位DP
    }


    init {
        mGestureDetector = GestureDetector(GlobalApp.getContext(), GestureHandler())
        mAnimator = ValueAnimator.ofFloat(0f, 1f)
        mAnimator.duration = ANIM_DURATION
        mAnimator.interpolator = DecelerateInterpolator()
    }

    fun attachToView(view: View, startX: Int, startY: Int) {
        mTargetView = view
        var pw = (mTargetView.parent.parent as View).width
        var ph = (mTargetView.parent.parent as View).height - PADDING_BOTTOM
        mParentWidth = pw
        mParentHeight = ph
        //只会调用一次
        if (ph > 0 && mViewHeight == 0 && mTargetView.measuredHeight > 0) {
            mViewHeight = mTargetView.measuredHeight
            mViewWidth = mTargetView.measuredWidth
            var startLoc = Point(startX, startY)
            if (startLoc != null) {
                mTargetView.translationX = startLoc.x.toFloat()
                mTargetView.translationY = startLoc.y.toFloat()
            }
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        mGestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                x = event.rawX.toInt()
                y = event.rawY.toInt()
                mDownX = mTargetView.translationX
                mDownY = mTargetView.translationY
            }
            MotionEvent.ACTION_MOVE -> {
                val nowX = event.rawX.toInt()
                val nowY = event.rawY.toInt()
                val movedX = nowX - x
                val movedY = nowY - y
                x = nowX
                y = nowY
                mTargetView.translationX += getMoveX(movedX)
                mTargetView.translationY += getMoveY(movedY)
            }
            MotionEvent.ACTION_UP -> {
                mUpX = mTargetView.translationX
                mUpY = mTargetView.translationY
                setTargetPosition()
                backToEdge()
            }
        }
        return true
    }

    private fun getMoveX(movedX: Int): Int {
        if (mTargetView.translationX + movedX < 0) {
            return -mTargetView.translationX.toInt()
        }
        if (mTargetView.translationX + movedX + mTargetView.width > mParentWidth) {
            return mParentWidth - mTargetView.width - mTargetView.translationX.toInt()
        }
        return movedX
    }

    private fun getMoveY(movedY: Int): Int {

        if (mTargetView.translationY + movedY < 0) {
            return -mTargetView.translationY.toInt()
        }
        if (mTargetView.translationY + movedY + mTargetView.height > mParentHeight) {
            return mParentHeight - mTargetView.height - mTargetView.translationY.toInt()
        }
        return movedY
    }


    /**
     * 回到边界
     */
    private fun backToEdge() {
        if (mAnimator.isRunning) {
            mAnimator.cancel()
        }
        mAnimator.addUpdateListener {
            var value = it.animatedValue as Float
            mTargetView.translationX = mUpX + (mAnimTargetX - mUpX) * value
            mTargetView.translationY = mUpY + (mAnimTargetY - mUpY) * value
        }
        mAnimator.start()
    }

    /**
     * 设置动画目标位置
     */
    private fun setTargetPosition() {
        when (mUpX > (mParentWidth / 2 - mTargetView.width / 2)) {
            true -> mAnimTargetX = (mParentWidth - mTargetView.width - PADING_DISTANT * DP).toFloat()
            false -> mAnimTargetX = 0f + PADING_DISTANT * DP
        }
        mAnimTargetY = mUpY
    }

    private fun onViewClicked() {
        mOnViewClickListener?.onViewClicked(mTargetView)
    }

    private fun onViewLongPress() {
        mOnViewClickListener?.onViewLongClicked(mTargetView)
    }


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

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            onViewLongPress()
        }
    }


    interface OnViewClickListener {
        fun onViewClicked(v: View)
        fun onViewLongClicked(v: View)
    }

}