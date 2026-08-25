// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.resource

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.hive.views.R

enum class ResourceDetailType {
    WORKFLOW,
    SKILL,
    TOOL
}

enum class ResourceDetailBadgeVariant {
    FILLED,
    SUBTLE
}

enum class ResourceDetailActionStyle {
    ACCENT,
    NEUTRAL
}

object ResourceDetailTypeStyleResolver {

    fun applyBadge(
        textView: TextView?,
        type: ResourceDetailType,
        variant: ResourceDetailBadgeVariant = ResourceDetailBadgeVariant.FILLED
    ) {
        val target = textView ?: return
        val fillColor = ContextCompat.getColor(target.context, colorRes(type))
        val textColor = if (variant == ResourceDetailBadgeVariant.FILLED) {
            ContextCompat.getColor(target.context, com.hive.i8n.R.color.white)
        } else {
            fillColor
        }
        val background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = target.resources.getDimension(com.hive.i8n.R.dimen.design_radius_full)
            if (variant == ResourceDetailBadgeVariant.FILLED) {
                setColor(fillColor)
                setStroke(0, fillColor)
            } else {
                setColor(ColorUtils.setAlphaComponent(fillColor, 0x24))
                setStroke(
                    target.resources.getDimensionPixelSize(R.dimen.design_border_width_normal),
                    ColorUtils.setAlphaComponent(fillColor, 0x50)
                )
            }
        }
        target.background = background
        target.setTextColor(textColor)
    }

    fun colorRes(type: ResourceDetailType): Int {
        return when (type) {
            ResourceDetailType.WORKFLOW -> com.hive.i8n.R.color.script_workflow_emerald
            ResourceDetailType.SKILL -> com.hive.i8n.R.color.design_accent_amber
            ResourceDetailType.TOOL -> com.hive.i8n.R.color.design_accent_sky
        }
    }
}

object ResourceDetailViewFactory {

    fun createChip(
        context: Context,
        text: String,
        clickable: Boolean = false,
        muted: Boolean = false,
        onClick: (() -> Unit)? = null
    ): TextView {
        val view = TextView(context).apply {
            this.text = text
            textSize = resources.getDimension(R.dimen.design_font_md) / resources.displayMetrics.scaledDensity
            setTypeface(typeface, if (clickable) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (muted) com.hive.i8n.R.color.design_text_muted
                    else com.hive.i8n.R.color.design_text_secondary
                )
            )
            setPadding(
                resources.getDimensionPixelSize(R.dimen.design_spacing_3),
                resources.getDimensionPixelSize(R.dimen.design_spacing_2),
                resources.getDimensionPixelSize(R.dimen.design_spacing_3),
                resources.getDimensionPixelSize(R.dimen.design_spacing_2)
            )
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = resources.getDimensionPixelSize(R.dimen.design_spacing_2)
                bottomMargin = resources.getDimensionPixelSize(R.dimen.design_spacing_2)
            }
            background = createOutlineSurfaceDrawable(context, clickable)
            if (clickable && onClick != null) {
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
        }
        return view
    }

    fun createEmptyText(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = resources.getDimension(R.dimen.design_font_md) / resources.displayMetrics.scaledDensity
            setTextColor(ContextCompat.getColor(context, com.hive.i8n.R.color.design_text_muted))
        }
    }

    fun createInfoCard(
        context: Context,
        title: String,
        desc: String,
        topMarginPx: Int = 0
    ): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.resource_detail_section_bg)
            setPadding(
                resources.getDimensionPixelSize(R.dimen.design_spacing_4),
                dp(context, 14),
                resources.getDimensionPixelSize(R.dimen.design_spacing_4),
                dp(context, 14)
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                this.topMargin = topMarginPx
            }
        }
        val titleView = TextView(context).apply {
            text = title
            setTextColor(ContextCompat.getColor(context, com.hive.i8n.R.color.design_text_secondary))
            textSize = resources.getDimension(R.dimen.design_font_lg) / resources.displayMetrics.scaledDensity
            setTypeface(typeface, Typeface.BOLD)
        }
        val descView = TextView(context).apply {
            text = desc
            setTextColor(ContextCompat.getColor(context, com.hive.i8n.R.color.design_text_muted))
            textSize = resources.getDimension(R.dimen.design_font_md) / resources.displayMetrics.scaledDensity
            setLineSpacing(0f, 1.15f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.design_spacing_1_5)
            }
        }
        card.addView(titleView)
        card.addView(descView)
        return card
    }

    fun styleActionButton(textView: TextView?, style: ResourceDetailActionStyle) {
        val target = textView ?: return
        val background = when (style) {
            ResourceDetailActionStyle.ACCENT -> createFilledActionDrawable(target.context)
            ResourceDetailActionStyle.NEUTRAL -> createOutlineSurfaceDrawable(target.context, clickable = false)
        }
        val textColor = when (style) {
            ResourceDetailActionStyle.ACCENT -> com.hive.i8n.R.color.design_accent_indigo
            ResourceDetailActionStyle.NEUTRAL -> com.hive.i8n.R.color.design_text_secondary
        }
        target.background = background
        target.setTextColor(ContextCompat.getColor(target.context, textColor))
    }

    private fun createFilledActionDrawable(context: Context): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = context.resources.getDimension(R.dimen.design_radius_lg)
            setColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(context, com.hive.i8n.R.color.design_accent_indigo), 0x20))
            setStroke(
                context.resources.getDimensionPixelSize(R.dimen.design_border_width_normal),
                ColorUtils.setAlphaComponent(ContextCompat.getColor(context, com.hive.i8n.R.color.design_accent_indigo), 0x55)
            )
        }
    }

    private fun createOutlineSurfaceDrawable(context: Context, clickable: Boolean): GradientDrawable {
        val strokeColor = if (clickable) {
            ColorUtils.setAlphaComponent(ContextCompat.getColor(context, com.hive.i8n.R.color.white), 0x20)
        } else {
            ContextCompat.getColor(context, com.hive.i8n.R.color.design_publish_detail_section_stroke)
        }
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = context.resources.getDimension(R.dimen.design_radius_lg)
            setColor(ContextCompat.getColor(context, com.hive.i8n.R.color.design_publish_detail_section_fill))
            setStroke(
                context.resources.getDimensionPixelSize(R.dimen.design_border_width_normal),
                strokeColor
            )
        }
    }

    private fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
