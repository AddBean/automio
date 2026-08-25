// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.carlos.ui.ext

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.interpolator.view.animation.FastOutLinearInInterpolator


inline fun View.doResponseAni(noinline onFinished :() -> Unit? = {}){
    responseAnimation(this,onFinished = onFinished)
}

fun TextView.doErrorResponseAni(onFinished :() -> Unit? = {}){
    val originColor = textColors
    setTextColor(Color.RED)
    bounceAnimation(this, duration = 100, repeat = 2) {
        setTextColor(originColor)
        if(background != null){
            background.setTintList(null)
        }
        onFinished?.invoke()
    }
}

fun View.shakeBottomLeftAndRight(onFinished: () -> Unit? = {}){
    post {
        pivotY = (0 - measuredHeight / 3).toFloat()
        pivotX = (measuredWidth / 2).toFloat()
        val rotation = PropertyValuesHolder.ofFloat("rotation", 0f, -10f, 0f, 8f, 0f,
                                                                                    -6f, 0f , 4f , 0f ,-2f ,0f)
        val animator = ObjectAnimator.ofPropertyValuesHolder(this, rotation)
        animator.setDuration(3000).interpolator = LinearInterpolator()
//        animator.addListener (onEnd = { onFinished.invoke()} )
//        animator.repeatCount = 1
//        animator.repeatMode = ValueAnimator.REVERSE
        animator.start()
    }
}

fun scaleAnimation(view: View?,repeat:Int = 1, onFinished: () -> Unit? = {}) {
    val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f,1f)
    val scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f,1f)
    val animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY)
    animator.setDuration(500).interpolator = LinearInterpolator()
//    animator.addListener (onEnd = {onFinished.invoke()})
    animator.repeatCount = 2
    animator.start()
}

fun responseAnimation(view: View?,duration :Long = 100, onFinished: () -> Unit? = {}) {
    val scaleX = PropertyValuesHolder.ofFloat("scaleX", 0.8f, 1f)
    val scaleY = PropertyValuesHolder.ofFloat("scaleY", 0.8f, 1f)
    val animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY)
    animator.setDuration(duration).interpolator = FastOutLinearInInterpolator()
//    animator.addListener (onEnd = {onFinished.invoke()})
    animator.start()
}

fun bounceAnimation(view: View,duration :Long = 100, repeat: Int = 1,onFinished: () -> Unit? = {}) {
    val scaleX = PropertyValuesHolder.ofFloat("translationX", -0.01f * view.width, 0f,0.01f * view.width,0f)
    //val scaleY = PropertyValuesHolder.ofFloat("scaleY", 0.8f, 1f)
    val animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX)
    animator.setDuration(duration).interpolator = LinearInterpolator()
//    animator.addListener (onEnd = { onFinished.invoke() })
    animator.repeatCount = repeat
    animator.start()
}