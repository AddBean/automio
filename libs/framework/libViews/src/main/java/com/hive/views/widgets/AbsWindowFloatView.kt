// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.hive.utils.system.CommonUtils
import com.hive.utils.system.SystemProperty
import com.hive.utils.utils.ScreenUtils
import kotlin.math.abs

abstract class AbsWindowFloatView(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {

    protected var mViewWidth: Int = 0
    protected var mViewHeight: Int = 0
    protected var DP: Int = CommonUtils.dipToPx(context, 1)
    private var mParentHeight: Int = 0
    private var mParentWidth: Int = 0
    /** 最小 Y 坐标，避免浮窗移动到状态栏上方，默认取状态栏高度 */
    private var mMinY: Int = 0
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
    private var downRawX: Float = 0f
    private var downRawY: Float = 0f
    open protected var PADING_DISTANT: Int = 8
    protected var PADDING_TOP: Int = 0;
    protected open var PADDING_BOTTOM: Int = 48 * DP
    protected open var ANIM_DURATION: Long = 300//动画时间
    protected open var BACK_DISTANCE: Int = 90//离开手指多远回到原点，单位DP

    private var mWindowManager: WindowManager? = null

    private var mLayoutParams = WindowManager.LayoutParams().also { lp ->
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            lp.type = WindowManager.LayoutParams.TYPE_TOAST
        }
        lp.flags =
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        lp.format = PixelFormat.RGBA_8888
        lp.gravity = Gravity.TOP or Gravity.START
    }

    var mTransX: Float = 0f
        set(value) {
            field = value
            mLayoutParams.x = value.toInt()
            if (parent != null) mWindowManager?.updateViewLayout(this, mLayoutParams)
        }

    var mTransY: Float = 0f
        set(value) {
            field = value
            mLayoutParams.y = value.toInt()
            if (parent != null) mWindowManager?.updateViewLayout(this, mLayoutParams)
        }


    init {
        mGestureDetector = GestureDetector(getContext(), GestureHandler())
        mAnimator = ValueAnimator.ofFloat(0f, 1f)
        mAnimator.duration = ANIM_DURATION
        mAnimator.interpolator = DecelerateInterpolator()
    }


    private var shouldDispatchTouchEvent = false


    fun requestNoInterceptTouchEvent() {
        shouldDispatchTouchEvent = true
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        mGestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                shouldDispatchTouchEvent = false
                requestDisallowInterceptTouchEvent(true)
                super.dispatchTouchEvent(event)
                nx = event.rawX.toInt() - mViewWidth / 2
                ny = event.rawY.toInt() - mViewHeight / 2
                mDownX = mTransX
                mDownY = mTransY
                downRawX = event.rawX
                downRawY = event.rawY
            }

            MotionEvent.ACTION_MOVE -> {
                super.dispatchTouchEvent(event)
                //如果滑动距离大于touchSlop，则不拦截事件，shouldDispatchTouchEvent=false
//                val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
//                if (abs(event.rawX - downRawX) > touchSlop || abs(event.rawY - downRawY) > touchSlop) {
//                    shouldDispatchTouchEvent = false
//                }
                if (!shouldDispatchTouchEvent) {
                    val nowX = event.rawX.toInt() - mViewWidth / 2
                    val nowY = event.rawY.toInt() - mViewHeight / 2
                    val movedX = nowX - nx
                    val movedY = nowY - ny
                    nx = nowX
                    ny = nowY
                    mTransX += getMoveX(movedX)
                    mTransY += getMoveY(movedY)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!shouldDispatchTouchEvent) {
                    mUpX = mTransX
                    mUpY = mTransY
                    backToEdge()
                    val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toDouble()
                    if (abs(mUpX - mDownX) < touchSlop && abs(mUpY - mDownY) < touchSlop) {
                        super.dispatchTouchEvent(event)
                    }
                } else {
                    super.dispatchTouchEvent(event)
                }
                shouldDispatchTouchEvent = false
            }
        }
        return true
    }

    open fun getMoveX(movedX: Int): Int {
        if (mTransX + movedX < 0) {
            return -mTransX.toInt()
        }
        if (mTransX + movedX + mViewWidth > mParentWidth) {
            return mParentWidth - mViewWidth - mTransX.toInt()
        }
        return movedX
    }

    open fun getMoveY(movedY: Int): Int {
        val minY = if (mMinY > 0) mMinY else PADDING_TOP
        if (mTransY + movedY < minY) {
            return minY - mTransY.toInt()
        }
        if (mTransY + movedY + mViewHeight > mParentHeight) {
            return mParentHeight - mViewHeight - mTransY.toInt()
        }
        return movedY
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val pw = ScreenUtils.getScreenWidth()
        val ph = ScreenUtils.getScreenHeight() - PADDING_BOTTOM
        mParentWidth = pw
        mParentHeight = ph
        if (mMinY <= 0) {
            mMinY = SystemProperty.getStatusBarHeight(context).coerceAtLeast(PADDING_TOP)
        }
        mViewHeight = measuredHeight
        mViewWidth = measuredWidth
        //只会调用一次
        if (ph > 0 && mViewHeight == 0 && measuredHeight > 0) {
            val startLoc = getStartPosition(pw, ph)
            if (startLoc != null) {
                mTransX = startLoc.x.toFloat()
                val minY = if (mMinY > 0) mMinY else PADDING_TOP
                mTransY = startLoc.y.toFloat().coerceAtLeast(minY.toFloat())
            }
        }
    }

    abstract fun getStartPosition(pw: Int, ph: Int): Point?

    open fun backToEdge(toLeft: Boolean) {
        if (mAnimator.isRunning) {
            mAnimator.cancel()
        }
        mUpX = mTransX
        mUpY = mTransY
        val minY = if (mMinY > 0) mMinY else PADDING_TOP
        mUpY = mUpY.coerceAtLeast(minY.toFloat())

        mAnimTargetX = if (!toLeft) {
            (mParentWidth - width - PADING_DISTANT * DP).toFloat()
        } else {
            0f + PADING_DISTANT * DP
        }
        mAnimTargetY = mUpY

        mAnimator.addUpdateListener {
            val value = it.animatedValue as Float
            mTransX = mUpX + (mAnimTargetX - mUpX) * value
            mTransY = mUpY + (mAnimTargetY - mUpY) * value
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

    /**
     * 回到边界
     */
    open fun backToEdge() {
        backToEdge(mUpX < (mParentWidth / 2 - width / 2))
    }

    open fun onEdgeAnimationEnd() {

    }

    /**
     * 设置动画目标位置
     */
    protected fun setTargetPosition() {
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
            e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
        ): Boolean {
            return true
        }

        override fun onScroll(
            e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float
        ): Boolean {
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            onViewLongPress()
        }
    }

    open fun getWindowContext(): Context = context

    fun addToWindow() {
        try {
            mWindowManager =
                getWindowContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (parent == null) {
                mWindowManager?.addView(this, mLayoutParams)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeToWindow() {
        if (parent != null) {
            mWindowManager?.removeView(this)
        }
    }
}