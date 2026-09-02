// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.accessibilityservice.AccessibilityService
import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.hive.anim.AnimUtils
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptConst
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.utils.GlobalApp
import com.hive.utils.extends.isLandscape
import com.hive.utils.system.CommonUtils
import com.hive.utils.utils.ViewUtils
import com.hive.views.view_manager.HiveViewManagerOfAccessibility
import com.hive.views.view_manager.HiveViewManagerOfActivity
import com.hive.views.view_manager.HiveViewManagerOfFloat
import com.hive.views.view_manager.IHiveViewManager
import android.view.OrientationEventListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Stack

/**
 *
 * @author jiadou
 * @date 6/16/21
 */
abstract class BaseScriptDialog(context: Context?) : BaseLayout(context) {

    @ScriptConst.FromSource
    protected var fromSource: Int = ScriptConst.From.FROM_SCRIPT_UNKNOWN

    private var windowView: View? = null

    private var mViewManager: IHiveViewManager? = null

    private var animRunning = false

    private var onDismissListener: OnDismissListener? = null

    private var animation: ValueAnimator? = null

    private var dismissCleanupDone = false

    private var dismissInProgress = false

    /** Invalidates callbacks belonging to an interrupted show/dismiss transition. */
    private var transitionGeneration = 0L

    private var currentContext: Context? = null

    open val Anim_Duration = ScriptConst.Anim_Duration

    private var isHidden = false

    private var layoutRoot: FrameLayout? = null

    /** 应用导航栏 inset 前，内容区原始 paddingBottom */
    private var baseContentPaddingBottom: Int? = null

    // 用于监听物理设备方向变化（悬浮窗应用需要）
    private var orientationEventListener: OrientationEventListener? = null
    private var lastLandscapeState: Boolean = GlobalApp.isLandscape()

    open fun getWindowContext(): Context = ScriptProvider.getViewContext()

    override fun initView(view: View?) {
        currentContext = getWindowContext()
        context.setTheme(com.hive.views.R.style.AppBaseTheme)
        windowView = LayoutInflater.from(context).inflate(getWindowLayoutId(), null)
        windowView?.isClickable = true
        val margin = getMarginParams()
        layoutRoot = findViewById(R.id.layoutRoot)
        layoutRoot?.addView(
            windowView,
            FrameLayout.LayoutParams(getWidthByOrientation(), getHeightByOrientation()).apply {
                this.gravity = Gravity.CENTER_HORIZONTAL
                this.setMargins(margin[0], margin[1], margin[2], margin[3])
            })
        getBgView()?.setBackgroundColor(getBgColor())
        if (isTouchOutsideDismissed()) {
            setOnClickListener {
                onTouchDismiss()
                dismiss()
            }
        }
        initWindow()
        applyNavigationBarInsets()
        initOrientationListener()
        post {
            initWindowContentTouch()
        }
    }

    /**
     * 弹层挂在 DecorView 上，会穿过系统导航栏。
     * - 底部 sheet / 全高内容：给 tag=ScriptDialog 补 padding，背景延伸进导航区
     * - 居中卡片：给外层补 padding，整体上移，避免卡片内部留白
     */
    private fun applyNavigationBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
            val navBottom = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val scriptDialog = windowView?.findViewWithTag<View>("ScriptDialog")
            val target = if (scriptDialog != null && isBottomAnchored(scriptDialog)) {
                scriptDialog
            } else {
                layoutRoot ?: this
            }
            val base = baseContentPaddingBottom ?: target.paddingBottom.also {
                baseContentPaddingBottom = it
            }
            target.setPadding(
                target.paddingLeft,
                target.paddingTop,
                target.paddingRight,
                base + navBottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(this)
    }

    private fun isBottomAnchored(view: View): Boolean {
        val lp = view.layoutParams ?: return false
        if (lp.height == LayoutParams.MATCH_PARENT || lp.height == FrameLayout.LayoutParams.MATCH_PARENT) {
            return true
        }
        if (lp is android.widget.RelativeLayout.LayoutParams) {
            return lp.rules[android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM] ==
                android.widget.RelativeLayout.TRUE
        }
        if (lp is FrameLayout.LayoutParams) {
            return lp.gravity and Gravity.BOTTOM == Gravity.BOTTOM
        }
        return false
    }

    private var isTouchInBar = false

    private var lastY = 0f

    private var startY = 0f

    @SuppressLint("ClickableViewAccessibility")
    open fun initWindowContentTouch() {
        val rootView = findWindowRoot()
        rootView?.setOnClickListener { }
        rootView?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (checkIfTouchInTopBar(event)) {
                        isTouchInBar = true
                        lastY = event.rawY
                        startY = event.rawY
                        return@setOnTouchListener true
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isTouchInBar) {
                        followFinger(event)
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isTouchInBar) {
                        //下滑超过80dp，dismiss
                        if (event.rawY - startY > ScriptCoordinateAdapter.getScreenHeight() / 3f) {
                            onTouchDismiss()
                            dismiss()
                        } else {
                            startTranslationAnimation(windowView?.translationY ?: 0f, 0f, null)
                        }
                    }
                    isTouchInBar = false
                }
            }
            isTouchInBar
        }
    }

    open fun onTouchDismiss() {

    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        initWindowContentTouch()
    }

    private fun followFinger(event: MotionEvent) {
        val dy = event.rawY - lastY
        lastY = event.rawY
        if ((windowView?.translationY?.plus(dy) ?: 0f) < 0) return
        windowView?.translationY = windowView?.translationY?.plus(dy) ?: 0f
    }

    private fun checkIfTouchInTopBar(event: MotionEvent): Boolean {
        val x = event.rawX
        val y = event.rawY
        val rect = getTouchBarRect()
        return rect.contains(x.toInt(), y.toInt())
    }

    private fun getTouchBarRect(): Rect {
        val topBarHeight = 36 * GlobalApp.DP
        val topBarWidth = 100 * GlobalApp.DP
        val windowRoot = findWindowRoot()
        val windowTopInScreen = IntArray(2)
        windowRoot ?: return Rect(0, 0, 0, 0)
        windowRoot.getLocationOnScreen(windowTopInScreen)
        val windowTopX = windowTopInScreen[0] + (windowRoot.width ?: 0) / 2 - topBarWidth / 2
        val windowTopY = windowTopInScreen[1]
        return Rect(windowTopX, windowTopY, windowTopX + topBarWidth, windowTopY + topBarHeight)
    }

    private fun findWindowRoot(): View? {
        var windowRoot: View? = null
        ViewUtils.traverseViewTree(windowView) {
            if (it.tag == "ScriptDialog") {
                windowRoot = it
                return@traverseViewTree
            }
        }
        return windowRoot
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
//        canvas?.drawRect(getTouchBarRect(), Paint().apply {
//            color = 0xffff0000.toInt()
//            style = Paint.Style.FILL
//        })
    }

    abstract fun initWindow()

    abstract fun getWindowLayoutId(): Int

    private fun initOrientationListener() {
        orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                // orientation: 0=竖屏, 90=横屏右, 180=倒竖屏, 270=横屏左
                if (orientation == ORIENTATION_UNKNOWN) return

                val currentLandscape = GlobalApp.isLandscape()
                if (currentLandscape != lastLandscapeState) {
                    lastLandscapeState = currentLandscape
                    // 触发布局更新
                    post {
                        updateLayoutForOrientation()
                    }
                }
            }
        }
        orientationEventListener?.enable()
    }

    private fun updateLayoutForOrientation() {
        windowView?.layoutParams?.apply {
            val margin = getMarginParams()
            this.width = getWidthByOrientation()
            this.height = getHeightByOrientation()
            ViewUtils.setMargins(windowView, margin[0], margin[1], margin[2], margin[3])
            windowView?.layoutParams = this
        }
        mView.layoutParams?.apply {
            mView?.layoutParams = this
        }
        // 更新背景色
        getBgView()?.setBackgroundColor(getBgColor())
        getViewManager()?.updateViewLayout(this)
    }

    open fun enableFadeAnimation() = false

    open fun isTouchOutsideDismissed() = true

    open fun getWidthByOrientation(): Int {
        return if (context.isLandscape())
            400 * DP
        else FrameLayout.LayoutParams.MATCH_PARENT
    }

    open fun getHeightByOrientation(): Int {
        return FrameLayout.LayoutParams.MATCH_PARENT
    }

    open fun getMarginParams(): Array<Int> {
        return arrayOf(0, 0, 0, 0)
    }

    open fun getBgColor() =
        if (GlobalApp.getContext().isLandscape()) 0x00000000 else 0xa0000000.toInt()

    /**
     * 自上而下动画，默认自下而上
     */
    open fun enableUpDownAnimation() = false

    /**
     * Optional transition hooks for dialogs that need to coordinate their animation with
     * WindowManager sizing. Returning true means the subclass owns the transition and must
     * invoke the supplied callbacks.
     */
    protected open fun runCustomShowAnimation(
        onStart: () -> Unit,
        onEnd: () -> Unit
    ): Boolean = false

    protected open fun runCustomDismissAnimation(onEnd: () -> Unit): Boolean = false

    /** Only custom transitions that can resume from their current visual values opt in. */
    protected open fun canInterruptDismissWithShow(): Boolean = false

    open fun getBgView(): View? = findViewById(R.id.view_bg)

    open fun onDismiss() {
        orientationEventListener?.disable()
        orientationEventListener = null
        stackRemove(this)
    }

    open fun onShow() {
        stackPush(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        post {
            windowView?.layoutParams?.apply {
                val margin = getMarginParams()
                this.width = getWidthByOrientation()
                this.height = getHeightByOrientation()
                ViewUtils.setMargins(windowView, margin[0], margin[1], margin[2], margin[3])
                windowView?.layoutParams = this
            }
            mView.layoutParams?.apply {
                mView?.layoutParams = this
            }
            getViewManager()?.updateViewLayout(this)
        }
    }

    fun hidden(anim: Boolean = false) {
        parent ?: return
        if (!anim) {
            getViewManager()?.removeView(this)
            isHidden = true
        } else {
            startFadeAnimation(false, object : AnimUtils.AnimListener() {
                override fun onOver(v: View?) {
                    getViewManager()?.removeView(this@BaseScriptDialog)
                    isHidden = true
                }
            }, 200L)
        }
    }

    fun restore(anim: Boolean = false) {
        if (parent != null) return
        visibility = View.VISIBLE
        getViewManager()?.addView(this)
        isHidden = false
        if (anim) {
            startFadeAnimation(true, null, 200L)
        }
    }

    open fun show(): BaseScriptDialog {
        isHidden = false
        if (visibility == View.VISIBLE && this.parent != null &&
            !(dismissInProgress && canInterruptDismissWithShow())
        ) return this
        if (currentContext == null) return this
        dismissCleanupDone = false
        dismissInProgress = false
        val showGeneration = ++transitionGeneration

        if (parent == null) {
            visibility = View.INVISIBLE
            getViewManager()?.addView(this)
        }
        stackPush(this)
        post {
            if (showGeneration != transitionGeneration) return@post
            clearAllAnimation()
            if (runCustomShowAnimation(
                    onStart = { visibility = View.VISIBLE },
                    onEnd = {
                        this@BaseScriptDialog.post {
                            if (showGeneration == transitionGeneration) onShow()
                        }
                    }
                )
            ) {
                return@post
            }
            if (enableFadeAnimation()) {
                startFadeAnimation(true, object : AnimUtils.AnimListener() {
                    override fun onBegin(v: View?) {
                        visibility = View.VISIBLE
                    }

                    override fun onOver(v: View?) {
                        super.onOver(v)
                        this@BaseScriptDialog.post {
                            if (showGeneration == transitionGeneration) onShow()
                        }
                    }
                })
            } else {
                startTranslationAnimation(height.toFloat(), 0f, object : AnimUtils.AnimListener() {
                    override fun onBegin(v: View?) {
                        visibility = View.VISIBLE
                    }

                    override fun onOver(v: View?) {
                        super.onOver(v)
                        this@BaseScriptDialog.post {
                            if (showGeneration == transitionGeneration) onShow()
                        }
                    }
                })
            }
        }
        return this
    }

    open fun dismiss(
        onDismissFun: (() -> Unit)? = null
    ): BaseScriptDialog {
        return dismiss(onDismissFun, true)
    }

    open fun dismiss(
        onDismissFun: (() -> Unit)? = null,
        notifyListener: Boolean = true
    ): BaseScriptDialog {
        CommonUtils.closeKeyboard(this)
        stackRemove(this)
        dismissInProgress = true
        val dismissGeneration = ++transitionGeneration
        post {
            if (dismissGeneration != transitionGeneration) return@post
            clearAllAnimation()
            if (notifyListener) {
                onDismissListener?.onDismiss()
            }
            val finishDismiss = {
                if (!dismissCleanupDone && dismissGeneration == transitionGeneration) {
                    dismissCleanupDone = true
                    dismissInProgress = false
                    isHidden = true
                    visibility = View.GONE
                    removeSelf()
                    onDismiss()
                    onDismissFun?.invoke()
                }
            }
            if (runCustomDismissAnimation(finishDismiss)) {
                return@post
            }
            if (enableFadeAnimation()) {
                startFadeAnimation(false, object : AnimUtils.AnimListener() {
                    override fun onOver(v: View?) {
                        finishDismiss()
                    }
                })
            } else {
                startTranslationAnimation(
                    windowView?.translationY ?: 0f,
                    height.toFloat(),
                    object : AnimUtils.AnimListener() {
                        override fun onOver(v: View?) {
                            finishDismiss()
                        }
                    })
            }
        }
        return this
    }

    private fun removeSelf() {
        try {
            getViewManager()?.removeView(this@BaseScriptDialog)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun getLayoutId() = R.layout.base_script_dialog

    private fun clearAllAnimation() {
        animRunning = false
        animation?.cancel()
    }

    private fun startFadeAnimation(
        fadeIn: Boolean, listener: AnimUtils.AnimListener?, duration: Long = Anim_Duration
    ) {
        if (animRunning) return
        animation = ValueAnimator.ofFloat(0f, 1f)
        animation?.duration = duration
        animation?.interpolator = DecelerateInterpolator()
        animation?.addUpdateListener {
            val v = it.animatedValue as Float
            if (!fadeIn) {
                getBgView()?.alpha = 1 - v
                windowView?.alpha = 1 - v
            } else {
                getBgView()?.alpha = v
                windowView?.alpha = v
            }
        }
        animation?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                animRunning = false
                listener?.onOver(this@BaseScriptDialog)
            }

            override fun onAnimationCancel(animation: Animator) {
                animRunning = false
            }

            override fun onAnimationStart(animation: Animator) {
                animRunning = true
                listener?.onBegin(this@BaseScriptDialog)
            }

        })
        animation?.start()
    }

    private fun startTranslationAnimation(
        y1: Float, y2: Float, listener: AnimUtils.AnimListener?
    ) {
        if (animRunning) return
        var startY = y1
        var endY = y2
        if (enableUpDownAnimation()) {
            startY = -y1
            endY = -y2
        }
        animation = ValueAnimator.ofFloat(0f, 1f)
        animation?.duration = Anim_Duration
        animation?.interpolator = DecelerateInterpolator()
        animation?.addUpdateListener {
            val v = it.animatedValue as Float
            if (enableUpDownAnimation()) {
                getBgView()?.alpha = if (endY > startY) {
                    v
                } else {
                    1 - v
                }
            } else {
                getBgView()?.alpha = if (endY > startY) {
                    1 - v
                } else {
                    v
                }
            }
            windowView?.translationY = startY + (endY - startY) * v
        }
        animation?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                animRunning = false
                listener?.onOver(this@BaseScriptDialog)
            }

            override fun onAnimationCancel(animation: Animator) {
                animRunning = false
            }

            override fun onAnimationStart(animation: Animator) {
                animRunning = true
                listener?.onBegin(this@BaseScriptDialog)
            }

        })
        animation?.start()
    }

    fun setFromSource(@ScriptConst.FromSource fromSource: Int): BaseScriptDialog {
        this.fromSource = fromSource
        return this
    }

    fun setOnDismissListener(listener: OnDismissListener?): BaseScriptDialog {
        onDismissListener = listener
        return this
    }

    interface OnDismissListener {
        fun onDismiss()
    }

    protected fun getViewManager(): IHiveViewManager? {
        if (mViewManager != null) return mViewManager
        val viewWidth = getWidthByOrientation()
        val viewHeight = getHeightByOrientation()
        mViewManager = when (currentContext) {
            is AccessibilityService -> HiveViewManagerOfAccessibility(
                ScriptProvider.getAccessService()!!, viewWidth, viewHeight
            )

            is Activity -> HiveViewManagerOfActivity(currentContext as Activity, viewWidth, viewHeight)

            else -> HiveViewManagerOfFloat(currentContext!!, viewWidth, viewHeight)
        }
        return mViewManager
    }

    companion object {
        private val viewStackSave = Stack<StackItem>()

        private val viewStack = Stack<BaseScriptDialog>()

        fun getDialogStack(): Stack<BaseScriptDialog> {
            return viewStack
        }

        fun stackPush(dialog: BaseScriptDialog) {
            if (viewStack.contains(dialog)) {
                viewStack.remove(dialog)
            }
            viewStack.push(dialog)
        }

        fun stackPop(): BaseScriptDialog? {
            if (viewStack.isEmpty()) return null
            return viewStack.pop()
        }

        fun stackRemove(dialog: BaseScriptDialog) {
            if (viewStack.isEmpty()) return
            if (!viewStack.contains(dialog)) return
            viewStack.remove(dialog)
        }

        fun onBackPress(): Boolean {
            if (viewStack.isEmpty()) return false
            val item = stackPop()
            if (item != null) {
                item.dismiss()
                return true
            }
            return false
        }

        /**
         * 保存所有dialog的状态和隐藏状态
         */
        fun saveStateAndHidden() {
            viewStackSave.clear()
            viewStackSave.addAll(viewStack.reversed().map { StackItem(it, it.isHidden) })
            viewStackSave.forEach {
                it.dialog?.hidden()
            }
        }

        /**
         * 恢复所有临时隐藏的dialog，仅会恢复saveStateAndHidden之后仍展示的dialog
         */
        fun restoreState() {
            while (!viewStackSave.isEmpty()) {
                val it = viewStackSave.pop()
                if (!it.isHidden) {
                    it.dialog?.restore()
                }
            }
            viewStackSave.clear()
        }

        data class StackItem(
            var dialog: BaseScriptDialog? = null, var isHidden: Boolean = false
        )
    }
}
