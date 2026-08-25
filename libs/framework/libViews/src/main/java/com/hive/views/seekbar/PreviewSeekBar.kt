// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import com.hive.utils.GlobalApp
import com.hive.utils.system.UIUtils
import com.hive.views.R
import kotlin.math.abs

/**
 *
 * @author jiadou
 * @date 2022/9/19
 */
class PreviewSeekBar(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs),
    IPreviewSeekBar {

    enum class State {
        Loading, Playing, Paused, Tracking
    }

    enum class Action {
        StartTracking, FinishTracking, DoTracking
    }

    private val drawerMap = mutableMapOf<State, AbsPreviewSeekBarDrawer>()

    private val segments = mutableListOf<PreviewSegmentData>()

    private val originalSegments = mutableListOf<PreviewSegmentData>()

    private var currentState = State.Playing
        set(value) {
            drawerMap.forEach {
                if (it.key == currentState && currentState != value) {
                    it.value.onHidden()
                } else if (it.key != currentState && it.key == value) {
                    it.value.onShow()
                }
            }
            field = value
            invalidate()
        }

    private var dp = UIUtils.dp2px(GlobalApp.getContext(),1)

    private var Monitor_interval = 200L

    private val Monitor_Beat_What = 1

    private var insetLeftPadding = 0f * dp

    private var insetRightPadding = 0f * dp

    private var viewWidth = 0

    private var viewHeight = 0

    private var progress = 0f

    private var onSeekBarListener: OnSeekBarStateListener? = null

    private var uiHandler: Handler? = null

    private var startTracking = false
        set(value) {
            field = value
            invalidate()
        }

    private var gestureDetector = GestureDetector(context!!, GestureHandler())

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var enableTouch = true

    private var globalTouchEnable = true

    private var previewSeekBarGlue: AbsPreviewSeekBarGlue? = null

    init {
        setStateDrawer(State.Loading, PreviewSeekBarLoadingDrawer())
        setStateDrawer(State.Playing, PreviewSeekBarPlayingDrawer())
        setStateDrawer(
            State.Paused,
            PreviewSeekBarPausedDrawer(R.drawable.preview_loading_min_icon)
        )
        setStateDrawer(
            State.Tracking,
            PreviewSeekBarTrackingDrawer(R.drawable.preview_loading_min_icon)
        )
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        updatePadding()
    }

    private fun updatePadding() {
        if (paddingLeft > 0)
            insetLeftPadding = paddingLeft.toFloat()
        if (paddingRight > 0)
            insetRightPadding = paddingRight.toFloat()
        drawerMap.forEach {
            val drawer = it.value
            drawer.originRect.set(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
            drawer.insetRect.set(drawer.originRect)
            drawer.insetRect.left = drawer.insetRect.left + insetLeftPadding
            drawer.insetRect.right = drawer.insetRect.right - insetRightPadding
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = measuredWidth
        viewHeight = measuredHeight
        updatePadding()

        invalidate()

    }


    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val drawer = getCurrentDrawer()
            ?: throw RuntimeException("You must implement a $currentState AbsPreviewSeekBarDrawer!!")
        canvas?.run {
            drawer.onDraw(canvas, progress, segments)
        }
    }

    private fun getCurrentDrawer() = drawerMap[currentState]

    override fun setProgress(progress: Float) {
        this.progress = progress
        invalidate()
        post { onSeekBarListener?.onProgressChanged(progress, segments, originalSegments) }
    }

    override fun setSegments(segments: List<PreviewSegmentData>) {
        this.segments.clear()
        this.segments.addAll(onSeekBarListener?.onPostProcessSegments(segments) ?: segments)
        invalidate()
    }

    override fun getSegments() = this.segments

    override fun setOriginalSegments(segments: List<PreviewSegmentData>) {
        this.originalSegments.clear()
        segments.forEach { originalSegments.add(it.copy()) }
        originalSegments.sortBy { it.inPoint }
    }

    override fun getOriginalSegments(): List<PreviewSegmentData> = originalSegments

    override fun setState(state: State) {
        currentState = state
        invalidate()
    }

    override fun getCurrentState(): State = currentState

    override fun setTouchEnable(enable: Boolean) {
        enableTouch = enable
    }

    override fun setGlobalTouchEnable(enable: Boolean) {
        globalTouchEnable = enable
    }

    override fun setTimeInterval(interval: Long) {
        Monitor_interval = interval
    }

    override fun setStateDrawer(style: State, drawer: AbsPreviewSeekBarDrawer) {
        drawerMap[style] = drawer
        drawer.hostView = this
        drawer.seekBar = this
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!enableTouch) return false
        if (startTracking && (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL)) {
            startBeating()
            startTracking = false
            onSeekBarListener?.onTouchStatusChanged(
                Action.FinishTracking,
                progress
            )
            previewSeekBarGlue?.onFinishTracking(progress)
        }

        return gestureDetector.onTouchEvent(event)
    }


    private fun startBeating() {
        uiHandler?.sendEmptyMessageDelayed(Monitor_Beat_What, Monitor_interval)
    }

    private fun stopBeating() {
        uiHandler?.removeMessages(Monitor_Beat_What)
    }

    override fun setOnStatusChangedListener(onProgressChanged: OnSeekBarStateListener?) {
        this.onSeekBarListener = onProgressChanged
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Looper.myLooper()?.run {
            uiHandler = object : Handler(this) {
                override fun handleMessage(msg: Message) {
                    startBeating()
                    onSeekBarListener?.run {
                        setProgress(onRetrievePlayerProgress())
                    }
                }
            }
        }
        setProgress(0f)
        startBeating()
        drawerMap.forEach {
            it.value.onAttached()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        setProgress(0f)
        stopBeating()
        drawerMap.forEach {
            it.value.onDetached()
        }
    }

    override fun release() {
        setProgress(0f)
        stopBeating()
        uiHandler = null
        previewSeekBarGlue?.release()
    }

    override fun setPreviewSeekBarGlue(glue: AbsPreviewSeekBarGlue?) {
        previewSeekBarGlue?.release()
        previewSeekBarGlue = glue
    }

    override fun getPreviewSeekBarGlue() = previewSeekBarGlue

    inner class GestureHandler : GestureDetector.OnGestureListener {


        override fun onShowPress(e: MotionEvent) {

        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return false
        }

        override fun onDown(e: MotionEvent): Boolean {
            if (!globalTouchEnable) {
                val drawer = getCurrentDrawer()
                val curPoint = (drawer?.insetRect?.width() ?: 0f) * progress
                val centerPoint = (drawer?.insetRect?.height() ?: 0f)
                val rect = RectF(
                    curPoint - 40 * dp,
                    centerPoint / 2 - 20 * dp,
                    curPoint + 40 * dp,
                    centerPoint / 2 + 20 * dp
                )
                return rect.contains(e!!.x, e.y)
            }
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
            if (!startTracking && abs(distanceX) > touchSlop) {
                if (onSeekBarListener?.shouldInterceptTracking() == true) {
                    return false
                }
                startTracking = true
                setState(State.Tracking)
                previewSeekBarGlue?.onStartTracking(progress)
                onSeekBarListener?.onTouchStatusChanged(
                    Action.StartTracking,
                    progress
                )
            }
            if (!startTracking) return false
            parent?.requestDisallowInterceptTouchEvent(true)
            stopBeating()
            val drawer = getCurrentDrawer()
            val progress =
                if (drawer == null) 0f else (drawer.insetRect.width() * progress - distanceX) / drawer.insetRect.width()
            this@PreviewSeekBar.progress = if (progress < 0) {
                0f
            } else if (progress > 1) {
                1f
            } else {
                progress
            }
            previewSeekBarGlue?.onDoTracking(progress)
            setProgress(this@PreviewSeekBar.progress)
            onSeekBarListener?.onTouchStatusChanged(
                Action.DoTracking,
                this@PreviewSeekBar.progress
            )
            return true
        }

        override fun onLongPress(e: MotionEvent) {
        }
    }


}