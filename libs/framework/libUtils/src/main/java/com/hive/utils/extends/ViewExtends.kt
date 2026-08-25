// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.extends


import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.PointF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Px
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.hive.utils.GlobalApp

fun View.visible(): View {
    this.visibility = android.view.View.VISIBLE
    return this
}

fun View.gone(): View {
    this.visibility = android.view.View.GONE
    return this
}

fun View.invisible(): View {
    this.visibility = android.view.View.INVISIBLE
    return this
}

fun View.visibleOrGone(visible: Boolean): View {
    this.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
    return this
}

fun View.visibleOrInvisible(visible: Boolean): View {
    this.visibility = if (visible) android.view.View.VISIBLE else android.view.View.INVISIBLE
    return this
}

fun View.goneOrInvisible(gone: Boolean): View {
    this.visibility = if (gone) android.view.View.GONE else android.view.View.INVISIBLE
    return this
}

fun View.visibleFadeIn(): View {
    this.alpha = 0f
    this.visibility = View.VISIBLE
    this.animate().alpha(1f).setDuration(200).start()
    return this
}

fun View.goneFadeOut(): View {
    this.animate().alpha(0f).setDuration(200).withEndAction {
        this.visibility = View.GONE
        this.alpha = 1f
    }.start()
    return this
}

fun View.invisibleFadeOut(): View {
    this.animate().alpha(0f).setDuration(200).withEndAction {
        this.visibility = View.INVISIBLE
        this.alpha = 1f
    }.start()
    return this
}

// 左右摇晃动画
fun View.shakeAnimation(): View {
    this.animate().translationX(10f).setDuration(50).withEndAction {
        this.animate().translationX(-10f).setDuration(50).withEndAction {
            this.animate().translationX(10f).setDuration(50).withEndAction {
                this.animate().translationX(0f).setDuration(50).start()
            }.start()
        }.start()
    }.start()
    return this
}

fun View.openSoftInput(): View {
    this.post {
        this.requestFocus()
        val imm =
            this.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    return this
}

fun View.isSoftInputShowing(): Boolean {
    return ViewCompat.getRootWindowInsets(this)?.isVisible(
        WindowInsetsCompat.Type.ime(),
    ) ?: false
}

fun View.hideSoftInput(clearFocus: Boolean = true) {
    if (!isSoftInputShowing()) return
    (context as? Activity)?.window?.let {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsControllerCompat(it, this).hide(WindowInsetsCompat.Type.ime())
        } else {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(it.decorView.windowToken, 0)
        }
    }
    if (clearFocus) {
        val focus = findFocus() ?: return
        focus.clearFocus()
    }
}

fun EditText.setTextAndMoveCursor(text: String) {
    this.setText(text)
    this.setSelection(text.length)
}

fun TextView.clearDrawable() {
    this.setCompoundDrawables(null, null, null, null)
}

fun TextView.setDrawable(resId: Int, position: DrawablePosition) {
    if (resId == -1) {
        this.clearDrawable()
        return
    }
    val drawable = resources.getDrawable(resId, null)
    drawable.setBounds(0, 0, drawable.minimumWidth, drawable.minimumHeight)
    when (position) {
        DrawablePosition.LEFT -> this.setCompoundDrawables(drawable, null, null, null)
        DrawablePosition.TOP -> this.setCompoundDrawables(null, drawable, null, null)
        DrawablePosition.RIGHT -> this.setCompoundDrawables(null, null, drawable, null)
        DrawablePosition.BOTTOM -> this.setCompoundDrawables(null, null, null, drawable)
    }
}

fun Context.getActivityByContext(): Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}

fun Context.getFragmentActivityByContext(): FragmentActivity? {
    return getActivityByContext() as? FragmentActivity
}

/**
 * 检查 Context 是否有效，可以安全地用于 Glide 等需要 Activity Context 的操作
 * @return true 如果 Context 有效（是 Application Context 或 Activity 未销毁），否则返回 false
 */
fun Context.isValidForGlide(): Boolean {
    val activity = getActivityByContext()
    return if (activity != null) {
        // 如果是 Activity Context，检查是否已销毁
        !activity.isDestroyed && !activity.isFinishing
    } else {
        // 如果是 Application Context，始终有效
        true
    }
}

fun Context.isLandscape(): Boolean {
    return GlobalApp.isLandscape()
}

fun Context.toastLong(message: String) {
    Toast.makeText(GlobalApp.getContext(), message, android.widget.Toast.LENGTH_LONG).show()
}

fun Context.toastShort(message: String) {
    Toast.makeText(GlobalApp.getContext(), message, android.widget.Toast.LENGTH_SHORT).show()
}

enum class DrawablePosition {
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
}

/**
 * 给view 设置圆角
 */
fun View.setCornerRadius(radius: Float) {
    clipToOutline = true
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }
    addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
    }
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun View.setBackgroundFromPath(imagePath: String) {
    try {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath, options)

        options.inSampleSize = calculateInSampleSize(
            options,
            width,
            height
        )
        options.inJustDecodeBounds = false

        val bitmap = BitmapFactory.decodeFile(imagePath, options)
        background = BitmapDrawable(resources, bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 从URI加载图片并设置为背景
 * @param layout 目标布局/View
 * @param imageUri 图片URI（如相册选择的content://xxx）
 */
fun View.setBackgroundFromUriOptimize(imageUri: Uri) {
    try {
        // 步骤1：先获取图片尺寸（不加载完整Bitmap）
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true // 仅解析尺寸，不分配内存

        // 从URI获取输入流，解析尺寸
        context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }

        // 步骤2：计算缩放比例（适配目标View的宽高）
        // 若View还未测量（如onCreate中），可传固定值（如屏幕宽高）
        val targetWidth = if (width > 0) width else resources.displayMetrics.widthPixels
        val targetHeight = if (height > 0) height else resources.displayMetrics.heightPixels
        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)

        // 步骤3：加载压缩后的Bitmap
        options.inJustDecodeBounds = false // 开始加载图片
        val bitmap = context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }

        // 步骤4：设置为背景
        bitmap?.let {
            background = BitmapDrawable(resources, it)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private inline fun View.updateMarginLayoutParams(block: ViewGroup.MarginLayoutParams.() -> Unit) {
    (layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
        params.block()
        layoutParams = params
    }
}

fun View.setMargins(t: Int, l: Int, b: Int, r: Int) {
    updateMarginLayoutParams {
        setMargins(l, t, r, b)
    }
}

// 为了方便使用，可以添加一些重载版本
fun View.setMargins(all: Int) {
    setMargins(all, all, all, all)
}

fun View.setMargins(horizontal: Int, vertical: Int) {
    setMargins(vertical, horizontal, vertical, horizontal)
}


/**
 * 设置顶部边距，保持其他方向的边距不变
 */
fun View.setMarginTop(@Px top: Int) {
    updateMarginLayoutParams {
        topMargin = top
    }
}

/**
 * 设置左边距，保持其他方向的边距不变
 */
fun View.setMarginLeft(@Px left: Int) {
    updateMarginLayoutParams {
        leftMargin = left
    }
}

/**
 * 设置右边距，保持其他方向的边距不变
 */
fun View.setMarginRight(@Px right: Int) {
    updateMarginLayoutParams {
        rightMargin = right
    }
}

/**
 * 设置start边距，保持其他方向的边距不变
 */
fun View.setMarginStart(@Px start: Int) {
    updateMarginLayoutParams {
        marginStart = start
    }
}

/**
 * 设置end边距，保持其他方向的边距不变
 */
fun View.setMarginEnd(@Px end: Int) {
    updateMarginLayoutParams {
        marginEnd = end
    }
}

/**
 * 设置底部边距，保持其他方向的边距不变
 */
fun View.setMarginBottom(@Px bottom: Int) {
    updateMarginLayoutParams {
        bottomMargin = bottom
    }
}

/**
 * 获取文本内部第一个字符的坐标位置
 */
fun EditText.getFirstCharPosition(): PointF? {
    val layout = this.layout ?: return null
    if (text.isNullOrEmpty()) return null

    val line = layout.getLineForOffset(0)
    val x = layout.getPrimaryHorizontal(0)
    val y = layout.getLineBaseline(line) + layout.getLineAscent(line)

    val finalX = x + totalPaddingLeft - scrollX
    val finalY = y + totalPaddingTop - scrollY

    return PointF(finalX, finalY.toFloat())
}

/**
 * 获取文本内部第一个字符的坐标位置
 */
fun TextView.getCharPosition(index: Int): PointF? {
    val layout = this.layout ?: return null
    if (text.isNullOrEmpty() || index < 0 || index >= text.length) return null

    val line = layout.getLineForOffset(index)
    val x = layout.getPrimaryHorizontal(index)
    val y = layout.getLineBaseline(line) + layout.getLineAscent(line)

    val finalX = x + totalPaddingLeft - scrollX
    val finalY = y + totalPaddingTop - scrollY
    return PointF(finalX, finalY.toFloat())
}

fun TextView.setLinearGradient(colors: IntArray) {
    val textWidth = paint.measureText(text.toString())
    val shader = LinearGradient(
        0f, 0f,
        textWidth, 0f,
        colors,
        null,                       // 均匀分布
        Shader.TileMode.CLAMP
    )

    paint.shader = shader
    invalidate()  // 重绘
}


fun TextView.enableMarquee() {
    isSingleLine = true
    ellipsize = TextUtils.TruncateAt.MARQUEE
    marqueeRepeatLimit = -1  // 无限循环
    isSelected = true  // 必须设置为true才能启动跑马灯
    isFocusable = true
    isFocusableInTouchMode = true
    requestFocus()
}

/**
 * 禁用跑马灯效果
 */
fun TextView.disableMarquee() {
    isSelected = false
    marqueeRepeatLimit = 0
}

/**
 * 查找指定类型的父View
 * @param T 要查找的父View类型
 * @return 找到的父View实例，如果不存在则返回null
 */
inline fun <reified T> View.findParentViewByClass(): T? {
    var parent = this.parent
    while (parent != null) {
        if (parent is T) {
            return parent as T
        }
        parent = parent.parent
    }
    return null
}