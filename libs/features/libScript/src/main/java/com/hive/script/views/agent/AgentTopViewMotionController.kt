// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.agent

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import androidx.annotation.ColorInt
import com.hive.script.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Owns all structural motion for [ScriptAgentTopView]. Surface and WindowManager share the
 * same dimensions on every frame so content never sits in a larger window's top-left corner.
 */
internal class AgentTopViewMotionController(
    private val host: View,
    private val surface: View,
    private val collapsed: View,
    private val expanded: View,
    private val updateWindowSize: (width: Int, height: Int) -> Unit
) {
    private data class Size(val width: Int, val height: Int)
    private enum class Transition { IDLE, ENTER, EXIT, MORPH, DISPOSED }

    private val interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
    private val density = host.resources.displayMetrics.density
    private val surfaceBackground = surface.background.mutate() as? GradientDrawable
    @ColorInt
    private val defaultSurfaceColor: Int =
        host.resources.getColor(com.hive.i8n.R.color.black, host.context.theme)

    private var activeAnimator: ValueAnimator? = null
    private var disposed = false
    private var captureHideCount = 0
    private var transition = Transition.IDLE
    private var pendingSizeRefresh = false
    private var cachedExpandedSize: Size? = null

    var isTargetCollapsed: Boolean = false
        private set

    val isDismissing: Boolean
        get() = transition == Transition.EXIT

    /** 更新浮层填充色（暂停提示色等）；传 null 恢复默认黑底。 */
    fun setSurfaceFillColor(@ColorInt color: Int?) {
        if (disposed) return
        surfaceBackground?.setColor(color ?: defaultSurfaceColor)
    }

    fun initialize(collapsedState: Boolean) {
        if (disposed) return
        isTargetCollapsed = collapsedState
        transition = Transition.IDLE
        pendingSizeRefresh = false
        cachedExpandedSize = null
        captureHideCount = 0
        val sizes = measureTargets()
        if (collapsedState) {
            normalizeCollapsed()
            applyStableSize(sizes.first)
        } else {
            normalizeExpanded()
            applyStableSize(sizes.second)
        }
    }

    /** Re-measures when width may change (orientation); expanded height stays fixed in layout. */
    fun refreshCurrentSize() {
        if (disposed || transition == Transition.EXIT) return
        if (activeAnimator != null || transition == Transition.MORPH || transition == Transition.ENTER) {
            pendingSizeRefresh = true
            return
        }
        pendingSizeRefresh = false
        val sizes = measureTargets()
        applyStableSize(if (isTargetCollapsed) sizes.first else sizes.second)
    }

    fun enter(onStart: () -> Unit, onEnd: () -> Unit) {
        if (disposed) return
        cancelActive()
        transition = Transition.ENTER
        onStart()

        val initialAppearance = host.alpha == 1f &&
            host.translationY == 0f && host.scaleX == 1f && host.scaleY == 1f
        if (initialAppearance) {
            host.alpha = 0f
            host.translationY = -16f * density
            host.scaleX = 0.96f
            host.scaleY = 0.96f
        }
        val startAlpha = host.alpha
        val startTranslation = host.translationY
        val startScaleX = host.scaleX
        val startScaleY = host.scaleY
        animate(220L, {
            transition = Transition.IDLE
            runPendingSizeRefresh()
            onEnd()
        }) { fraction ->
            host.alpha = lerp(startAlpha, 1f, fraction)
            host.translationY = lerp(startTranslation, 0f, fraction)
            host.scaleX = lerp(startScaleX, 1f, fraction)
            host.scaleY = lerp(startScaleY, 1f, fraction)
        }
    }

    fun exit(onEnd: () -> Unit) {
        if (disposed) return
        cancelActive(recoverEnter = false)
        transition = Transition.EXIT
        pendingSizeRefresh = false
        captureHideCount = 0
        val startAlpha = host.alpha
        val startTranslation = host.translationY
        val startScaleX = host.scaleX
        val startScaleY = host.scaleY
        animate(180L, {
            transition = Transition.IDLE
            onEnd()
        }) { fraction ->
            host.alpha = lerp(startAlpha, 0f, fraction)
            host.translationY = lerp(startTranslation, -12f * density, fraction)
            host.scaleX = lerp(startScaleX, 0.98f, fraction)
            host.scaleY = lerp(startScaleY, 0.98f, fraction)
        }
    }

    fun expand(onEnd: () -> Unit = {}) {
        if (disposed || transition == Transition.EXIT) return
        isTargetCollapsed = false
        val expandedSize = measureTargets().second
        morph(
            target = expandedSize,
            duration = 280L,
            collapsing = false,
            targetCollapsedAlpha = 0f,
            targetExpandedAlpha = 1f,
            targetExpandedTranslation = 0f,
            targetRadius = 18f * density
        ) {
            transition = Transition.IDLE
            normalizeExpanded()
            applyStableSize(expandedSize)
            runPendingSizeRefresh()
            onEnd()
        }
    }

    fun collapse(onEnd: () -> Unit = {}) {
        if (disposed || transition == Transition.EXIT) return
        isTargetCollapsed = true
        val collapsedSize = measureTargets().first
        morph(
            target = collapsedSize,
            duration = 220L,
            collapsing = true,
            targetCollapsedAlpha = 1f,
            targetExpandedAlpha = 0f,
            targetExpandedTranslation = 8f * density,
            targetRadius = 24f * density
        ) {
            transition = Transition.IDLE
            normalizeCollapsed()
            applyStableSize(collapsedSize)
            runPendingSizeRefresh()
            onEnd()
        }
    }

    fun snapHiddenForCapture() {
        if (disposed) return
        if (transition == Transition.EXIT) {
            host.visibility = View.INVISIBLE
            return
        }
        if (captureHideCount == 0) {
            cancelAndSnapToCurrentTarget()
            host.visibility = View.INVISIBLE
        }
        captureHideCount++
    }

    fun snapVisibleAfterCapture() {
        if (disposed || transition == Transition.EXIT) return
        if (captureHideCount <= 0) {
            host.visibility = View.VISIBLE
            return
        }
        captureHideCount--
        if (captureHideCount == 0) {
            host.visibility = View.VISIBLE
            normalizeHostTransform()
        }
    }

    /** 强制恢复到完全可见外观（用于 show 兜底 / 动画中断恢复）。 */
    fun ensureFullyVisible() {
        if (disposed || transition == Transition.EXIT) return
        if (captureHideCount > 0) return
        cancelAndSnapToCurrentTarget()
        host.visibility = View.VISIBLE
        normalizeHostTransform()
    }

    fun cancelAndSnapToCurrentTarget() {
        if (disposed || transition == Transition.EXIT) return
        cancelActive(recoverEnter = true)
        transition = Transition.IDLE
        pendingSizeRefresh = false
        val sizes = measureTargets()
        if (isTargetCollapsed) {
            normalizeCollapsed()
            applyStableSize(sizes.first)
        } else {
            normalizeExpanded()
            applyStableSize(sizes.second)
        }
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        transition = Transition.DISPOSED
        pendingSizeRefresh = false
        captureHideCount = 0
        cachedExpandedSize = null
        cancelActive(recoverEnter = false)
    }

    private fun morph(
        target: Size,
        duration: Long,
        collapsing: Boolean,
        targetCollapsedAlpha: Float,
        targetExpandedAlpha: Float,
        targetExpandedTranslation: Float,
        targetRadius: Float,
        onEnd: () -> Unit
    ) {
        val startWidth = surface.layoutParams.width.takeIf { it > 0 } ?: surface.width
        val startHeight = surface.layoutParams.height.takeIf { it > 0 } ?: surface.height
        val startCollapsedAlpha = collapsed.alpha
        val startExpandedAlpha = expanded.alpha
        val startExpandedTranslation = expanded.translationY
        val startRadius = surfaceBackground?.cornerRadius
            ?.takeIf { it > 0f }
            ?: if (isTargetCollapsed) 18f * density else 24f * density

        collapsed.visibility = View.VISIBLE
        expanded.visibility = View.VISIBLE
        cancelActive(recoverEnter = true)
        transition = Transition.MORPH

        val sizes = measureTargets()
        val widthRange = abs(sizes.second.width - sizes.first.width)
        val heightRange = abs(sizes.second.height - sizes.first.height)
        val widthDistance = abs(target.width - startWidth).toFloat() / max(1, widthRange)
        val heightDistance = abs(target.height - startHeight).toFloat() / max(1, heightRange)
        val alphaDistance = max(
            abs(targetCollapsedAlpha - startCollapsedAlpha),
            abs(targetExpandedAlpha - startExpandedAlpha)
        )
        val remaining = max(0.15f, min(1f, max(max(widthDistance, heightDistance), alphaDistance)))

        val startSize = Size(startWidth, startHeight)
        animate((duration * remaining).toLong(), onEnd) { fraction ->
            applyFrameSize(
                Size(
                    lerp(startSize.width, target.width, fraction),
                    lerp(startSize.height, target.height, fraction)
                )
            )
            val collapsedProgress = if (collapsing) {
                ((fraction - 0.35f) / 0.65f).coerceIn(0f, 1f)
            } else {
                (fraction / 0.55f).coerceIn(0f, 1f)
            }
            val expandedProgress = if (collapsing) {
                (fraction / 0.60f).coerceIn(0f, 1f)
            } else {
                ((fraction - 0.20f) / 0.80f).coerceIn(0f, 1f)
            }
            collapsed.alpha =
                lerp(startCollapsedAlpha, targetCollapsedAlpha, collapsedProgress)
            expanded.alpha = lerp(startExpandedAlpha, targetExpandedAlpha, expandedProgress)
            expanded.translationY =
                lerp(startExpandedTranslation, targetExpandedTranslation, fraction)
            surfaceBackground?.cornerRadius = lerp(startRadius, targetRadius, fraction)
        }
    }

    private fun animate(duration: Long, onEnd: () -> Unit, frame: (Float) -> Unit) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        activeAnimator = animator
        animator.duration = duration
        animator.interpolator = interpolator
        animator.addUpdateListener { frame(it.animatedValue as Float) }
        animator.addListener(object : AnimatorListenerAdapter() {
            private var cancelled = false

            override fun onAnimationCancel(animation: Animator) {
                cancelled = true
            }

            override fun onAnimationEnd(animation: Animator) {
                if (activeAnimator === animator) activeAnimator = null
                if (!cancelled && !disposed) onEnd()
            }
        })
        animator.start()
    }

    private fun cancelActive(recoverEnter: Boolean = true) {
        val wasEnter = transition == Transition.ENTER
        val animator = activeAnimator ?: return
        activeAnimator = null
        animator.cancel()
        // enter 被中断时必须回到完全可见，否则 host.alpha 会卡在中间值导致「整场不显示」
        if (recoverEnter && wasEnter && !disposed) {
            normalizeHostTransform()
            if (captureHideCount == 0) {
                host.visibility = View.VISIBLE
            }
        }
    }

    private fun normalizeHostTransform() {
        host.alpha = 1f
        host.translationY = 0f
        host.scaleX = 1f
        host.scaleY = 1f
    }

    private fun runPendingSizeRefresh() {
        if (!pendingSizeRefresh || disposed || transition != Transition.IDLE) return
        pendingSizeRefresh = false
        refreshCurrentSize()
    }

    private fun measureTargets(): Pair<Size, Size> {
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        collapsed.measure(unspecified, unspecified)
        val collapsedSize = Size(collapsed.measuredWidth, collapsed.measuredHeight)

        val expandedSize = cachedExpandedSize?.takeIf { cached ->
            val screenWidth = host.resources.displayMetrics.widthPixels
            val maxWidth = host.resources.getDimensionPixelSize(R.dimen.script_agent_top_view_max_width)
            val expandedWidth = min(maxWidth, screenWidth - (40f * density).toInt())
                .coerceAtLeast(collapsedSize.width)
            cached.width == expandedWidth
        } ?: measureExpandedSize(collapsedSize.width).also { cachedExpandedSize = it }

        return collapsedSize to expandedSize
    }

    private fun measureExpandedSize(minWidth: Int): Size {
        val screenWidth = host.resources.displayMetrics.widthPixels
        val maxWidth = host.resources.getDimensionPixelSize(R.dimen.script_agent_top_view_max_width)
        val expandedWidth = min(maxWidth, screenWidth - (40f * density).toInt())
            .coerceAtLeast(minWidth)

        val savedExpandedVisibility = expanded.visibility
        val savedCollapsedVisibility = collapsed.visibility
        expanded.visibility = View.VISIBLE
        collapsed.visibility = View.INVISIBLE
        expanded.layoutParams = expanded.layoutParams.apply {
            width = expandedWidth
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        val widthSpec = View.MeasureSpec.makeMeasureSpec(expandedWidth, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        expanded.measure(widthSpec, heightSpec)
        expanded.visibility = savedExpandedVisibility
        collapsed.visibility = savedCollapsedVisibility

        return Size(expandedWidth, expanded.measuredHeight.coerceAtLeast(collapsed.measuredHeight))
    }

    private fun applyStableSize(size: Size) {
        applyFrameSize(size)
    }

    /** Surface and WM window always share the same bounds (including morph frames). */
    private fun applyFrameSize(size: Size) {
        surface.layoutParams = surface.layoutParams.apply {
            width = size.width
            height = size.height
        }
        surface.requestLayout()
        updateWindowSize(size.width, size.height)
    }

    private fun normalizeExpanded() {
        normalizeHostTransform()
        expanded.visibility = View.VISIBLE
        expanded.alpha = 1f
        expanded.translationY = 0f
        collapsed.alpha = 0f
        collapsed.translationY = 0f
        collapsed.visibility = View.INVISIBLE
        surfaceBackground?.cornerRadius = 18f * density
    }

    private fun normalizeCollapsed() {
        normalizeHostTransform()
        collapsed.visibility = View.VISIBLE
        collapsed.alpha = 1f
        collapsed.translationY = 0f
        expanded.alpha = 0f
        expanded.translationY = 0f
        expanded.visibility = View.INVISIBLE
        surfaceBackground?.cornerRadius = 24f * density
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float =
        start + (end - start) * fraction

    private fun lerp(start: Int, end: Int, fraction: Float): Int =
        lerp(start.toFloat(), end.toFloat(), fraction).toInt()
}
