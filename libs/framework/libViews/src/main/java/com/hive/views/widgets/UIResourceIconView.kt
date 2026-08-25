// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.hive.i8n.R as I8nR
import com.hive.views.R
import com.hive.views.utils.RoundCornerHelper
import androidx.core.graphics.withSave

/**
 * Unified resource type icon view — auto-matches corner radius, background color,
 * and default icon based on resource type (workflow/skill/tool).
 *
 * Supports remote icon URLs directly — when a remote image is loaded, the tint
 * is automatically cleared and the image is displayed with rounded corners.
 */
class UIResourceIconView : FrameLayout {

    companion object {
        const val TYPE_WORKFLOW = 0
        const val TYPE_SKILL = 1
        const val TYPE_TOOL = 2

        const val SHAPE_SQUARE = 0
        const val SHAPE_CIRCLE = 1
    }

    private var mResourceType = TYPE_WORKFLOW
    private var mContainerShape = SHAPE_SQUARE
    private var mRadiusRatio = 0.25f
    private var mBackgroundColorExplicit = false
    private var mBackgroundColor = 0
    private var mBorderWidth = 0
    private var mBorderColor = 0
    private var mInnerSize = -1 // -1 = auto (50% of container)
    private var mIconTintExplicit = false
    private var mIconTint = 0
    private var mDefaultIconVisible = true
    private var mRemoteIconLoaded = false

    private lateinit var mIconView: ImageView
    private var mRoundCornerHelper: RoundCornerHelper? = null
    private var mCirclePath: Path? = null
    private val mCircleOutlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: android.view.View, outline: Outline) {
            outline.setOval(0, 0, view.width, view.height)
        }
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        inflate(context, R.layout.ui_resource_icon_view, this)
        mIconView = findViewById(R.id.iconImageView)

        mRoundCornerHelper = RoundCornerHelper().apply { init() }
        mCirclePath = Path()
        setWillNotDraw(false)

        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.UIResourceIconView) {
                mResourceType = getInt(R.styleable.UIResourceIconView_ri_resourceType, TYPE_WORKFLOW)
                mContainerShape = getInt(R.styleable.UIResourceIconView_ri_containerShape, SHAPE_SQUARE)
                mRadiusRatio = getFloat(R.styleable.UIResourceIconView_ri_radiusRatio, 0.25f)
                mBackgroundColorExplicit = hasValue(R.styleable.UIResourceIconView_ri_backgroundColor)
                if (mBackgroundColorExplicit) {
                    mBackgroundColor = getColor(R.styleable.UIResourceIconView_ri_backgroundColor, 0)
                }
                mBorderWidth = getDimensionPixelSize(R.styleable.UIResourceIconView_ri_borderWidth, 0)
                mBorderColor = getColor(R.styleable.UIResourceIconView_ri_borderColor, 0)
                mInnerSize = getDimensionPixelSize(R.styleable.UIResourceIconView_ri_innerSize, -1)
                mIconTintExplicit = hasValue(R.styleable.UIResourceIconView_ri_iconTint)
                if (mIconTintExplicit) {
                    mIconTint = getColor(R.styleable.UIResourceIconView_ri_iconTint, 0)
                }
                mDefaultIconVisible = getBoolean(R.styleable.UIResourceIconView_ri_defaultIconVisible, true)
            }
        }

        syncOutlineClipping()
        applyTypeColors()
        applyDefaultIcon()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            if (mContainerShape == SHAPE_CIRCLE) {
                val radius = minOf(w, h) / 2f
                mCirclePath?.reset()
                mCirclePath?.addCircle(w / 2f, h / 2f, radius, Path.Direction.CW)
                outlineProvider = mCircleOutlineProvider
                clipToOutline = true
                invalidateOutline()
            } else {
                val radius = minOf(w, h) * mRadiusRatio
                mRoundCornerHelper?.config(
                    RectF(0f, 0f, w.toFloat(), h.toFloat()),
                    RectF(0f, 0f, w.toFloat(), h.toFloat()),
                    floatArrayOf(radius, radius, radius, radius),
                    mBorderWidth,
                    mBorderColor,
                )
            }
            updateIconSize()
        }
    }

    override fun draw(canvas: Canvas) {
        if (mContainerShape == SHAPE_CIRCLE) {
            canvas.withSave {
                mCirclePath?.let { clipPath(it) }
                super.draw(this)
            }
        } else {
            mRoundCornerHelper?.preDraw(canvas)
            super.draw(canvas)
            mRoundCornerHelper?.postDraw(canvas)
        }
    }

    private fun updateIconSize() {
        val size = if (mRemoteIconLoaded) {
            minOf(width, height)
        } else if (mInnerSize > 0) {
            mInnerSize
        } else {
            minOf(width, height) / 2
        }
        val lp = mIconView.layoutParams as? FrameLayout.LayoutParams
        if (lp != null && (lp.width != size || lp.height != size)) {
            lp.width = size
            lp.height = size
            lp.gravity = android.view.Gravity.CENTER
            mIconView.layoutParams = lp
        }
    }

    // ---- Public API ----

    /** Set resource type (TYPE_WORKFLOW / TYPE_SKILL / TYPE_TOOL). */
    fun setResourceType(type: Int): UIResourceIconView {
        mResourceType = type
        applyTypeColors()
        if (!mRemoteIconLoaded) {
            applyDefaultIcon()
        }
        return this
    }

    /** Set resource type by string ("workflow" / "skill" / "tool"). */
    fun setResourceType(type: String): UIResourceIconView {
        val resolved = when (type.lowercase()) {
            "workflow" -> TYPE_WORKFLOW
            "skill" -> TYPE_SKILL
            "tool" -> TYPE_TOOL
            else -> TYPE_WORKFLOW
        }
        return setResourceType(resolved)
    }

    /**
     * Set a local drawable resource as the icon.
     * Clears tint automatically unless explicitly overridden.
     */
    fun setIconResource(@DrawableRes resId: Int): UIResourceIconView {
        mRemoteIconLoaded = true
        mIconView.setImageResource(resId)
        mIconView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        if (!mIconTintExplicit) {
            mIconView.clearColorFilter()
        }
        updateIconSize()
        return this
    }

    /** Set a Drawable directly as the icon. Clears tint unless explicitly overridden. */
    fun setImageDrawable(drawable: Drawable?): UIResourceIconView {
        mRemoteIconLoaded = true
        mIconView.setImageDrawable(drawable)
        mIconView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        if (!mIconTintExplicit) {
            mIconView.clearColorFilter()
        }
        updateIconSize()
        return this
    }

    /**
     * Mark that a remote image has been loaded into the inner ImageView.
     * Clears tint and hides the fallback default icon.
     */
    fun markRemoteIconLoaded(): UIResourceIconView {
        mRemoteIconLoaded = true
        mIconView.scaleType = ImageView.ScaleType.CENTER_CROP
        if (!mIconTintExplicit) {
            mIconView.clearColorFilter()
        }
        updateIconSize()
        return this
    }

    /** Reset to show the default type-based icon. */
    fun resetToDefaultIcon(): UIResourceIconView {
        mRemoteIconLoaded = false
        mIconView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        applyDefaultIcon()
        updateIconSize()
        return this
    }

    fun setContainerShape(shape: Int): UIResourceIconView {
        mContainerShape = shape
        syncOutlineClipping()
        requestLayout()
        return this
    }

    private fun syncOutlineClipping() {
        if (mContainerShape == SHAPE_CIRCLE) {
            outlineProvider = mCircleOutlineProvider
            clipToOutline = true
            invalidateOutline()
        } else {
            clipToOutline = false
            outlineProvider = null
            invalidateOutline()
        }
    }

    fun setRadiusRatio(ratio: Float): UIResourceIconView {
        mRadiusRatio = ratio
        requestLayout()
        return this
    }

    fun setBackgroundColorOverride(@ColorInt color: Int): UIResourceIconView {
        mBackgroundColor = color
        mBackgroundColorExplicit = true
        super.setBackgroundColor(color)
        return this
    }

    fun setBorderWidth(width: Int): UIResourceIconView {
        mBorderWidth = width
        requestLayout()
        return this
    }

    fun setBorderColor(@ColorInt color: Int): UIResourceIconView {
        mBorderColor = color
        requestLayout()
        return this
    }

    fun setInnerSize(size: Int): UIResourceIconView {
        mInnerSize = size
        updateIconSize()
        return this
    }

    fun setIconTintOverride(@ColorInt color: Int): UIResourceIconView {
        mIconTint = color
        mIconTintExplicit = true
        applyIconTintToCurrentDrawable()
        return this
    }

    fun setDefaultIconVisible(visible: Boolean): UIResourceIconView {
        mDefaultIconVisible = visible
        if (!visible && !mRemoteIconLoaded) {
            mIconView.setImageResource(0)
        }
        return this
    }

    /** Returns the inner ImageView for direct Glide/ImageLoader integration. */
    fun getIconImageView(): ImageView = mIconView

    // ---- Internal ----

    private fun applyTypeColors() {
        if (!mBackgroundColorExplicit) {
            mBackgroundColor = getDefaultBackgroundColor(mResourceType)
            super.setBackgroundColor(mBackgroundColor)
        }
        if (!mIconTintExplicit) {
            mIconTint = getDefaultIconTint(mResourceType)
        }
    }

    private fun applyDefaultIcon() {
        if (mDefaultIconVisible) {
            mIconView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            mIconView.setImageResource(getDefaultIconRes(mResourceType))
            applyIconTintToCurrentDrawable()
        }
    }

    private fun applyIconTintToCurrentDrawable() {
        if (mIconTint != 0 && !mRemoteIconLoaded) {
            mIconView.setColorFilter(mIconTint)
        }
    }

    @DrawableRes
    private fun getDefaultIconRes(type: Int): Int = when (type) {
        TYPE_WORKFLOW -> I8nR.drawable.ic_resource_workflow
        TYPE_SKILL -> I8nR.drawable.ic_resource_skill
        TYPE_TOOL -> I8nR.drawable.ic_resource_tool
        else -> I8nR.drawable.ic_resource_workflow
    }

    @ColorInt
    private fun getDefaultBackgroundColor(type: Int): Int = when (type) {
        TYPE_WORKFLOW -> ContextCompat.getColor(context, I8nR.color.design_accent_indigo_10)
        TYPE_SKILL -> ContextCompat.getColor(context, I8nR.color.design_accent_amber_5)
        TYPE_TOOL -> ContextCompat.getColor(context, I8nR.color.design_accent_sky_5)
        else -> ContextCompat.getColor(context, I8nR.color.design_accent_indigo_10)
    }

    @ColorInt
    private fun getDefaultIconTint(type: Int): Int = when (type) {
        TYPE_WORKFLOW -> ContextCompat.getColor(context, I8nR.color.design_accent_indigo)
        TYPE_SKILL -> ContextCompat.getColor(context, I8nR.color.design_accent_amber)
        TYPE_TOOL -> ContextCompat.getColor(context, I8nR.color.design_accent_sky)
        else -> ContextCompat.getColor(context, I8nR.color.design_accent_indigo)
    }
}
