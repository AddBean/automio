// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.anim

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.EditText
import androidx.core.view.ViewCompat
import java.lang.ref.WeakReference


object AnimUtils {
    fun scaleAnim(view: View?, from: Float, to: Float, mAnimListener: AnimListener?): Animation {
        val animation = ScaleAnimation(
            from,
            to,
            from,
            to,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        animation.duration = 200L
        animation.startNow()
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                mAnimListener?.onBegin(view)
            }

            override fun onAnimationEnd(animation: Animation) {
                mAnimListener?.onOver(view)
            }

            override fun onAnimationRepeat(animation: Animation) {
            }
        })
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(animation)
        view?.startAnimation(animationSet)
        animationSet.start()
        return animation
    }

    @JvmStatic
    fun scaleAnim(view: View?, mAnimListener: AnimListener?): Animation {
        val animation = ScaleAnimation(0.8f, 1.0f, 0.8f, 1.0f, 1, 0.5f, 1, 0.5f)
        animation.duration = 200L
        animation.startNow()
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                mAnimListener?.onBegin(view)
            }

            override fun onAnimationEnd(animation: Animation) {
                mAnimListener?.onOver(view)
            }

            override fun onAnimationRepeat(animation: Animation) {
            }
        })
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(animation)
        view?.startAnimation(animationSet)
        animationSet.start()
        return animation
    }

    @JvmStatic
    fun scaleAnim(view: View?): Boolean {
        view?:return false
        val animation = ScaleAnimation(0.94f, 1.0f, 0.94f, 1.0f, 1, 0.5f, 1, 0.5f)
        animation.duration = 120L
        animation.startNow()
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(animation)
        view.startAnimation(animationSet)
        animationSet.start()
        return false
    }

    fun scaleAnim(view: View?, targetScale: Float): Boolean {
        view?:return false
        val animation = ScaleAnimation(targetScale, 1.0f, targetScale, 1.0f, 1, 0.5f, 1, 0.5f)
        animation.duration = 120L
        animation.startNow()
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(animation)
        view.startAnimation(animationSet)
        animationSet.start()
        return false
    }

    @JvmStatic
    fun fadeOutAnim(view: View?, mAnimListener: AnimListener?): Boolean {
        view?:return false
        return fadeOutAnim(view, 200L, mAnimListener)
    }

    @JvmStatic
    fun fadeOutAnim(view: View?, duration: Long, mAnimListener: AnimListener?): Boolean {
        if (view == null) return false
        val animation = AlphaAnimation(1.0f, 0f)
        animation.duration = duration
        animation.startNow()
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                mAnimListener?.onBegin(view)
            }

            override fun onAnimationEnd(animation: Animation) {
                mAnimListener?.onOver(view)
            }

            override fun onAnimationRepeat(animation: Animation) {
            }
        })
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(animation)
        view.startAnimation(animationSet)
        animationSet.start()
        return false
    }

    fun fadeInAnim(view: View?, duration: Long, mAnimListener: AnimListener?): Boolean {
        if (view == null) return false
        val animation = AlphaAnimation(0f, 1f)
        animation.duration = duration
        animation.startNow()
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                mAnimListener?.onBegin(view)
            }

            override fun onAnimationEnd(animation: Animation) {
                mAnimListener?.onOver(view)
            }

            override fun onAnimationRepeat(animation: Animation) {
            }
        })
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(animation)
        view.startAnimation(animationSet)
        animationSet.start()
        return false
    }

    @JvmStatic
    fun fadeInAnim(view: View?, mAnimListener: AnimListener?): Boolean {
        return fadeInAnim(view, 200L, mAnimListener)
    }

    fun heightAnim(
        view: View?,
        startHeight: Int,
        targetHeight: Int,
        animListener: AnimListener?
    ): ValueAnimator? {
        if (startHeight == targetHeight) return null
        val ref = WeakReference(view)
        val valueAnimator = ValueAnimator.ofInt(startHeight, targetHeight)
        valueAnimator.setDuration(300)
        valueAnimator.addUpdateListener(AnimatorUpdateListener { animation ->
            if (ref?.get() == null) return@AnimatorUpdateListener
            val value = animation.animatedValue as Int
            val v = ref.get()
            val lp = v!!.layoutParams
            lp.height = value
            v.layoutParams = lp
            if (targetHeight == value) {
                animListener?.onOver(view)
            }
            if (startHeight == value) {
                animListener?.onBegin(view)
            }
            animListener?.onProgress(view, value)
        })
        valueAnimator.start()
        return valueAnimator
    }

    fun rotateAnim(v: View?) {
        v?.let { view ->
            view.pivotX = (view.width / 2).toFloat()
            view.pivotY = (view.height / 2).toFloat()
            val animation = RotateAnimation(0f, 360f, view.pivotX, view.pivotY)
            animation.duration = 300L
            animation.interpolator = LinearInterpolator()
            view.startAnimation(animation)
        }
    }

    fun startXTranslation(v: View?, startX: Int, endX: Int): TranslateAnimation {
        val animation = TranslateAnimation(startX.toFloat(), endX.toFloat(), 0f, 0f)
        animation.duration = 300L
        animation.fillAfter = true
        animation.interpolator = LinearInterpolator()
        v?.startAnimation(animation)
        return animation
    }

    fun startYTranslation(v: View?, startY: Int, endY: Int): TranslateAnimation {
        val animation = TranslateAnimation(0f, 0f, startY.toFloat(), endY.toFloat())
        animation.duration = 300L
        animation.fillAfter = true
        animation.interpolator = LinearInterpolator()
        v?.startAnimation(animation)
        return animation
    }

    fun startYTranslation(
        v: View?,
        startY: Int,
        endY: Int,
        animListener: AnimListener
    ): TranslateAnimation {
        val animation = TranslateAnimation(0f, 0f, startY.toFloat(), endY.toFloat())
        animation.duration = 300L
        animation.fillAfter = true
        animation.interpolator = LinearInterpolator()
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                animListener.onBegin(v)
            }

            override fun onAnimationEnd(animation: Animation) {
                animListener.onOver(v)
            }

            override fun onAnimationRepeat(animation: Animation) {
            }
        })
        v?.startAnimation(animation)
        return animation
    }

    fun startRotate(v: View?, interval: Long, loop: Boolean): RotateAnimation {
        return v?.let { view ->
            view.pivotX = (view.width / 2).toFloat()
            view.pivotY = (view.height / 2).toFloat()
            val animation = RotateAnimation(0f, 360f, view.pivotX, view.pivotY)
            animation.duration = interval
            animation.repeatCount = if (loop) Animation.INFINITE else 0
            animation.fillAfter = true
            animation.interpolator = LinearInterpolator()
            view.startAnimation(animation)
            animation
        } ?: RotateAnimation(0f, 360f, 0f, 0f)
    }

    fun stopRotate(v: View) {
        if (v.animation != null && v.animation is RotateAnimation) {
            val animation = v.animation as RotateAnimation
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                }

                override fun onAnimationEnd(animation: Animation) {
                }

                override fun onAnimationRepeat(animation: Animation) {
                    animation.cancel()
                    startRotate(v, animation.duration, false).interpolator =
                        DecelerateInterpolator()
                }
            })
        }
    }


    const val TYPING_SPEED_MS = 10L

    // 动画时长常量
    const val ANIMATION_DURATION_SHORT = 100L
    const val ANIMATION_DURATION_MEDIUM = 300L
    const val ANIMATION_DURATION_LONG = 400L

    // 动画缩放常量
    const val SCALE_PRESSED = 0.95f
    const val SCALE_CARD_PRESSED = 0.98f

    /**
     * 卡片按下动画
     */
    fun animatePress(view: View?, onComplete: () -> Unit) {
        view?.let {
            val currentElevation = ViewCompat.getElevation(it)
            val scaleDown = createScaleAnimatorSet(
                it, 1f, SCALE_CARD_PRESSED,
                currentElevation, currentElevation * 0.5f
            )
            val scaleUp = createScaleAnimatorSet(
                it, SCALE_CARD_PRESSED, 1f,
                currentElevation * 0.5f, currentElevation
            )

            scaleDown.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    scaleUp.start()
                    onComplete()
                }
            })

            scaleDown.start()
        } ?: onComplete()
    }

    /**
     * 缩放按下动画
     */
    fun animateScalePress(
        view: View?,
        scale: Float,
        onComplete: () -> Unit,
        interpolator: android.view.animation.Interpolator = AccelerateDecelerateInterpolator()
    ) {
        view?.let {
            it.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(ANIMATION_DURATION_SHORT)
                .setInterpolator(interpolator)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(ANIMATION_DURATION_SHORT)
                        .setInterpolator(interpolator)
                        .withEndAction { onComplete() }
                        .start()
                }
                .start()
        } ?: onComplete()
    }


    /**
     * 视图入场动画
     */
    fun animateViewEntrance(view: View, delay: Long = 0L) {
        view.alpha = 0f
        view.translationY = 50f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(ANIMATION_DURATION_LONG)
            .setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /**
     * 缩放弹跳动画
     */
    fun animateScaleBounce(
        view: View?,
        scale: Float,
        duration: Long = ANIMATION_DURATION_SHORT
    ) {
        view?.let { v ->
            v.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(duration)
                .withEndAction {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(duration)
                        .start()
                }
                .start()
        }
    }

    /**
     * 视图可见性动画
     */
    fun animateViewVisibility(view: View?, isVisible: Boolean) {
        view?.let {
            if (isVisible && it.visibility != View.VISIBLE) {
                it.visibility = View.VISIBLE
                it.alpha = 0f
                it.animate()
                    .alpha(1f)
                    .setDuration(ANIMATION_DURATION_MEDIUM)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            } else if (!isVisible && it.visibility == View.VISIBLE) {
                it.animate()
                    .alpha(0f)
                    .setDuration(ANIMATION_DURATION_MEDIUM)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction { it.visibility = View.GONE }
                    .start()
            }
        }
    }

    /**
     * 打字机效果文本输入
     */
    fun animateTextInput(editText: EditText, text: String, onComplete: () -> Unit) {
        editText.setText("")
        editText.requestFocus()

        var currentIndex = 0
        val typingRunnable = object : Runnable {
            override fun run() {
                if (currentIndex < text.length) {
                    editText.append(text[currentIndex].toString())
                    currentIndex++
                    editText.postDelayed(this, TYPING_SPEED_MS)
                } else {
                    try {
                        val actualLength = editText.text?.length ?: 0
                        val safePosition =
                            if (actualLength < text.length) actualLength else text.length
                        if (safePosition > 0) {
                            editText.setSelection(safePosition)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        editText.post(typingRunnable)
    }


    /**
     * 创建缩放动画集合
     */
    private fun createScaleAnimatorSet(
        view: View,
        fromScale: Float,
        toScale: Float,
        fromElevation: Float,
        toElevation: Float
    ): AnimatorSet {
        return AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", fromScale, toScale),
                ObjectAnimator.ofFloat(view, "scaleY", fromScale, toScale),
                ObjectAnimator.ofFloat(view, "elevation", fromElevation, toElevation)
            )
            duration = ANIMATION_DURATION_SHORT
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    abstract class AnimListener {
        open fun onOver(v: View?) {
        }

        fun onProgress(v: View?, height: Int) {
        }

        open fun onBegin(v: View?) {
        }
    }


}
