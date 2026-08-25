// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.extension

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.hive.anim.AnimUtils.AnimListener
import com.hive.anim.AnimUtils.fadeInAnim
import com.hive.anim.AnimUtils.fadeOutAnim
import com.hive.utils.thread.UIHandlerUtils

/**
 * View 扩展
 */
fun <T : View> T.visible(): T {
    if (UIHandlerUtils.isOnMainThread()) {
        this.visibility = View.VISIBLE
    } else {
        post {
            this.visibility = View.VISIBLE
        }
    }
    return this
}

fun <T : View> T.invisible(): T {
    if (UIHandlerUtils.isOnMainThread()) {
        this.visibility = View.INVISIBLE
    } else {
        post {
            this.visibility = View.INVISIBLE
        }
    }
    return this
}

fun <T : View> T.gone(): T {
    if (UIHandlerUtils.isOnMainThread()) {
        this.visibility = View.GONE
    } else {
        post {
            this.visibility = View.GONE
        }
    }
    return this
}

fun <T : View> T.removeSelf(): T {
    this.parent?.let {
        if (it::class.java.name.contains("ViewRootImpl")) {
            var windowManager =
                this.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.removeView(this)
        } else {
            (it as ViewGroup).removeView(this)
        }

    }
    return this
}


fun <T : View> T.visibleOrGone(visible: Boolean): T {
    if (UIHandlerUtils.isOnMainThread()) {
        this.visibility = if (visible) View.VISIBLE else View.GONE
    } else {
        post {
            this.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }
    return this
}

fun <T : View> T.visibleOrGoneWithFadeAnim(visible: Boolean): T {
    if (visible) {
        this.visible()
        fadeInAnim(this, null)
    } else {
        this.visible()
        fadeOutAnim(this, object : AnimListener() {
            override fun onOver(v: View?) {
                v?.gone()
            }
        })
    }
    return this
}

fun <T : View> T.setWidth(width: Int): T {
    this.layoutParams.apply {
        this.width = width
    }
    this.layoutParams = this.layoutParams
    return this
}

fun <T : View> T.setHeight(height: Int): T {
    this.layoutParams.apply {
        this.height = height
    }
    this.layoutParams = this.layoutParams
    return this
}

fun <T : View> T.visibleOrInvisible(visible: Boolean): T {
    this.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    return this
}

fun View.isVisible(): Boolean {
    return this.visibility == View.VISIBLE
}

fun View.isInvisible(): Boolean {
    return this.visibility == View.INVISIBLE
}

fun View.isGone(): Boolean {
    return this.visibility == View.GONE
}

fun View.enableOrDisable(enable: Boolean) {
    this.isEnabled = enable
    this.alpha = if (enable) 1.0f else 0.4f
}
