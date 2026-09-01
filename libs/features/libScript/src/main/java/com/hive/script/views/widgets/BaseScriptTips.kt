// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration
import android.text.TextUtils
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.hive.extension.visibleOrGone
import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IEditorProvider
import com.hive.script.R
import com.hive.script.views.agent.ScriptAgentTopView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.isLandscape
import com.hive.utils.system.ClipboardUtil
import com.hive.views.widgets.CommonToast
import android.view.ViewGroup


open class BaseScriptTips(context: Context) : BaseScriptDialog(context) {

    private var btnAction: TextView? = null
    private var btnCancel: TextView? = null
    private var btnSubmit: TextView? = null
    private var layoutOpt: View? = null
    private var tvMsg: TextView? = null
    private var tvTitle: TextView? = null

    // 收起状态相关组件
    private var llCollapsedStatus: LinearLayout? = null
    private var ivCollapsedIcon: ImageView? = null
    private var tvCollapsedStatus: TextView? = null
    private var vCollapsedIndicator: View? = null
    private var cvExpandedContent: View? = null
    private var btnCollapse: ImageView? = null
    private var btnCollapseExpanded: ImageView? = null

    // 收起状态相关变量
    private var isCollapsed = false
    private var isCollapseEnabled = false
    private val collapseManager = CollapseManager()

    private var layoutParamsNotFull = WindowManager.LayoutParams().also { lp ->
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            lp.type = WindowManager.LayoutParams.TYPE_TOAST
        }
        lp.flags =
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        lp.format = PixelFormat.RGBA_8888
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
    }

    private fun applyTopWindowOffset() {
        layoutParamsNotFull.y = ScriptAgentTopView.getWindowTopY()
        findViewById<FrameLayout>(R.id.layoutRoot)?.fitsSystemWindows = false
        llCollapsedStatus?.let { bar ->
            (bar.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = 0
        }
    }

    override fun initWindow() {
        btnAction = findViewById(R.id.btnAction)
        btnCancel = findViewById(R.id.btnCancel)
        btnSubmit = findViewById(R.id.btnSubmit)
        layoutOpt = findViewById(R.id.layoutOpt)
        tvMsg = findViewById(R.id.tvMsg)
        tvMsg?.movementMethod = ScrollingMovementMethod()
        tvTitle = findViewById(R.id.tvTitle)

        // 收起状态组件
        llCollapsedStatus = findViewById(R.id.ll_collapsed_status)
        ivCollapsedIcon = findViewById(R.id.iv_collapsed_icon)
        tvCollapsedStatus = findViewById(R.id.tv_collapsed_status)
        vCollapsedIndicator = findViewById(R.id.v_collapsed_indicator)
        cvExpandedContent = findViewById(R.id.cv_expanded_content)
        btnCollapse = findViewById(R.id.btn_collapse)
        btnCollapseExpanded = findViewById(R.id.btn_collapse_expanded)

        setupCollapseListeners()

        // 默认隐藏收起按钮
        btnCollapse?.visibility = View.GONE
        btnCollapseExpanded?.visibility = View.GONE

        // 默认隐藏操作按钮
        btnAction?.visibility = View.GONE

        applyTopWindowOffset()
        post {
            applyTopWindowOffset()
            getViewManager()?.updateLayoutParams(layoutParamsNotFull)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        applyTopWindowOffset()
        getViewManager()?.updateLayoutParams(layoutParamsNotFull)
    }

    private fun setupCollapseListeners() {
        // 点击收起状态小条展开
        llCollapsedStatus?.setOnClickListener {
            collapseManager.expand()
        }

        // 点击展开内容区域重置自动收起定时器
        cvExpandedContent?.setOnClickListener {
            collapseManager.resetAutoCollapseTimer()
        }

        // 点击收起按钮（收起状态）
        btnCollapse?.setOnClickListener {
            animateButtonClick(btnCollapse) {
                collapseManager.toggleCollapse()
            }
        }

        // 点击收起按钮（展开状态）
        btnCollapseExpanded?.setOnClickListener {
            animateButtonClick(btnCollapseExpanded) {
                collapseManager.toggleCollapse()
            }
        }
    }

    private fun animateButtonClick(button: ImageView?, action: () -> Unit) {
        button?.let {
            // 简单的缩放动画
            val scaleDown = ObjectAnimator.ofFloat(it, "scaleX", 1f, 0.9f)
            val scaleDownY = ObjectAnimator.ofFloat(it, "scaleY", 1f, 0.9f)
            val scaleUp = ObjectAnimator.ofFloat(it, "scaleX", 0.9f, 1f)
            val scaleUpY = ObjectAnimator.ofFloat(it, "scaleY", 0.9f, 1f)

            val animatorSet = AnimatorSet()
            animatorSet.play(scaleDown).with(scaleDownY)
            animatorSet.play(scaleUp).with(scaleUpY).after(scaleDown)
            animatorSet.duration = 150
            animatorSet.interpolator = AccelerateDecelerateInterpolator()

            animatorSet.start()

            // 延迟执行动作
            postDelayed({
                action()
            }, 75)
        }
    }

    fun startDismissTimer(delay: Long): BaseScriptTips {
        postDelayed({
            dismiss()
        }, delay)
        return this
    }

    open fun setOptEnable(enable: Boolean): BaseScriptTips {
        layoutOpt?.visibleOrGone(enable)
        return this
    }

    open fun setSubmitText(text: String): BaseScriptTips {
        btnSubmit?.text = text
        return this
    }

    open fun setCancelText(text: String): BaseScriptTips {
        btnCancel?.text = text
        return this
    }

    open fun setTitleText(text: String): BaseScriptTips {
        tvTitle?.text = text
        tvCollapsedStatus?.text = text
        return this
    }

    open fun setMsgText(text: String): BaseScriptTips {
        val editorProvider = ComponentManager.getInstance().getProvider(IEditorProvider::class.java) as? IEditorProvider
        if (editorProvider != null && tvMsg != null) {
            editorProvider.renderMarkdown(tvMsg!!, text)
        } else {
            tvMsg?.text = text
        }
        return this
    }

    open fun setSubmitClickListener(listener: (dialog: BaseScriptTips) -> Unit): BaseScriptTips {
        btnSubmit?.setOnClickListener {
            listener.invoke(this)
        }
        return this
    }

    open fun setCancelClickListener(listener: (dialog: BaseScriptTips) -> Unit): BaseScriptTips {
        btnCancel?.setOnClickListener {
            listener.invoke(this)
        }
        return this
    }

    /**
     * 设置操作按钮为复制，点击后复制当前内容到剪贴板。
     * 默认不展示，调用此方法后显示复制按钮。
     */
    open fun setActionButton(
        btnText: String,
        onClick: (v: View, text: String?) -> Unit
    ): BaseScriptTips {
        btnAction?.visibility = View.VISIBLE
        btnAction?.text = btnText
        btnAction?.setOnClickListener {
            val content = tvMsg?.text?.toString()
            onClick.invoke(it, content)
        }
        return this
    }

    /**
     * 设置是否启用收起功能
     */
    open fun setCollapseEnabled(enabled: Boolean): BaseScriptTips {
        isCollapseEnabled = enabled
        if (enabled) {
            collapseManager.startAutoCollapseTimer()
            btnCollapse?.visibility = View.VISIBLE
            btnCollapseExpanded?.visibility = View.VISIBLE
        } else {
            collapseManager.cleanup()
            btnCollapse?.visibility = View.GONE
            btnCollapseExpanded?.visibility = View.GONE
        }
        return this
    }

    /**
     * 设置收起状态显示的文本
     */
    open fun setCollapsedText(text: String): BaseScriptTips {
        tvCollapsedStatus?.text = text
        return this
    }

    override fun getMarginParams() = arrayOf(0, 0, 0, 0)

    override fun getHeightByOrientation(): Int {
        return FrameLayout.LayoutParams.WRAP_CONTENT
    }

    override fun enableFadeAnimation() = false

    override fun enableUpDownAnimation() = true

    override fun getWindowLayoutId() = R.layout.base_script_tips

    override fun getLayoutId() = R.layout.base_script_tips_dialog

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        collapseManager.cleanup()
    }

    // 收起状态管理器
    private inner class CollapseManager {
        private var autoCollapseTimer: Handler? = null
        private val AUTO_COLLAPSE_DELAY = 8000L

        fun startAutoCollapseTimer() {
            autoCollapseTimer = Handler(Looper.getMainLooper())
            resetAutoCollapseTimer()
        }

        fun resetAutoCollapseTimer() {
            if (!isCollapseEnabled) return

            autoCollapseTimer?.removeCallbacksAndMessages(null)
            autoCollapseTimer?.postDelayed({
                if (!isCollapsed) {
                    collapse()
                }
            }, AUTO_COLLAPSE_DELAY)
        }

        fun toggleCollapse() {
            if (!isCollapseEnabled) return

            if (isCollapsed) {
                expand()
            } else {
                collapse()
            }
        }

        fun expand() {
            if (!isCollapsed || !isCollapseEnabled) return

            isCollapsed = false

            // 创建展开动画
            val expandAnimator = AnimatorSet()

            // 收起状态淡出
            val fadeOutCollapsed = ObjectAnimator.ofFloat(llCollapsedStatus, "alpha", 1f, 0f)
            fadeOutCollapsed.duration = 200

            // 展开内容淡入
            val fadeInExpanded = ObjectAnimator.ofFloat(cvExpandedContent, "alpha", 0f, 1f)
            fadeInExpanded.duration = 200

            // 收起按钮旋转动画和图标更新
            val rotateButton = ObjectAnimator.ofFloat(btnCollapse, "rotation", 180f, 0f)
            val rotateButtonExpanded =
                ObjectAnimator.ofFloat(btnCollapseExpanded, "rotation", 180f, 0f)
            rotateButton.duration = 200
            rotateButtonExpanded.duration = 200
            rotateButton.interpolator = AccelerateDecelerateInterpolator()
            rotateButtonExpanded.interpolator = AccelerateDecelerateInterpolator()

            // 更新按钮图标
            btnCollapse?.setImageResource(R.drawable.ic_collapse_arrow)
            btnCollapseExpanded?.setImageResource(R.drawable.ic_collapse_arrow)

            expandAnimator.play(fadeOutCollapsed).with(rotateButton).with(rotateButtonExpanded)
            expandAnimator.play(fadeInExpanded).after(fadeOutCollapsed)

            expandAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    cvExpandedContent?.visibility = View.VISIBLE
                    cvExpandedContent?.alpha = 0f
                }

                override fun onAnimationEnd(animation: Animator) {
                    llCollapsedStatus?.visibility = View.GONE
                    llCollapsedStatus?.alpha = 1f
                }
            })

            expandAnimator.start()
            resetAutoCollapseTimer()
        }

        private fun collapse() {
            if (isCollapsed || !isCollapseEnabled) return

            isCollapsed = true

            // 创建收起动画
            val collapseAnimator = AnimatorSet()

            // 展开内容淡出
            val fadeOutExpanded = ObjectAnimator.ofFloat(cvExpandedContent, "alpha", 1f, 0f)
            fadeOutExpanded.duration = 200

            // 收起状态淡入
            val fadeInCollapsed = ObjectAnimator.ofFloat(llCollapsedStatus, "alpha", 0f, 1f)
            fadeInCollapsed.duration = 200

            // 收起按钮旋转动画和图标更新
            val rotateButton = ObjectAnimator.ofFloat(btnCollapse, "rotation", 0f, 180f)
            val rotateButtonExpanded =
                ObjectAnimator.ofFloat(btnCollapseExpanded, "rotation", 0f, 180f)
            rotateButton.duration = 200
            rotateButtonExpanded.duration = 200
            rotateButton.interpolator = AccelerateDecelerateInterpolator()
            rotateButtonExpanded.interpolator = AccelerateDecelerateInterpolator()

            // 更新按钮图标
            btnCollapse?.setImageResource(R.drawable.ic_expand_arrow)
            btnCollapseExpanded?.setImageResource(R.drawable.ic_expand_arrow)

            collapseAnimator.play(fadeOutExpanded).with(rotateButton).with(rotateButtonExpanded)
            collapseAnimator.play(fadeInCollapsed).after(fadeOutExpanded)

            collapseAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    llCollapsedStatus?.visibility = View.VISIBLE
                    llCollapsedStatus?.alpha = 0f
                }

                override fun onAnimationEnd(animation: Animator) {
                    cvExpandedContent?.visibility = View.GONE
                    cvExpandedContent?.alpha = 1f
                }
            })

            collapseAnimator.start()
        }

        fun cleanup() {
            autoCollapseTimer?.removeCallbacksAndMessages(null)
            autoCollapseTimer = null
        }
    }
}

/*
使用示例：

// 创建提示对话框
val tips = BaseScriptTips(context)
    .setTitleText("提示标题")
    .setMsgText("这是提示信息内容")
    .setSubmitText("确定")
    .setCancelText("取消")
    .setCollapseEnabled(true)  // 启用收起功能
    .setCollapsedText("提示信息")  // 设置收起状态显示的文本
    .setSubmitClickListener { dialog ->
        // 处理确定按钮点击
        dialog.dismiss()
    }
    .setCancelClickListener { dialog ->
        // 处理取消按钮点击
        dialog.dismiss()
    }

// 显示对话框
tips.show()

// 启动自动消失定时器（可选）
tips.startDismissTimer(5000)
*/