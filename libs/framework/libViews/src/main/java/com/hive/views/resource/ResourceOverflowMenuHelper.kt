// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.resource

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import com.hive.views.R

data class ResourceOverflowAction(
    val title: CharSequence,
    val danger: Boolean = false,
    val onClick: () -> Unit
)

object ResourceOverflowMenuHelper {

    fun show(
        anchor: View,
        actions: List<ResourceOverflowAction>,
        widthDp: Int = 164
    ): PopupWindow {
        val context = anchor.context
        var popupRef: PopupWindow? = null
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = createMenuBackground(context)
            setPadding(0, dp(context, 4), 0, dp(context, 4))
        }

        val visibleActions = actions.filter { it.title.isNotBlank() }
        visibleActions.forEachIndexed { index, action ->
            if (action.danger && index > 0) {
                root.addView(createDivider(context))
            }
            root.addView(createItemView(context, action) {
                popupRef?.dismiss()
                action.onClick()
            })
        }

        val popup = PopupWindow(
            root,
            dp(context, widthDp),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
            elevation = context.resources.getDimension(R.dimen.design_spacing_2)
        }
        popupRef = popup

        root.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val xOffset = anchor.width - root.measuredWidth
        popup.showAsDropDown(
            anchor,
            xOffset,
            context.resources.getDimensionPixelSize(R.dimen.design_spacing_2),
            Gravity.START
        )
        return popup
    }

    private fun createMenuBackground(context: Context): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = context.resources.getDimension(com.hive.i8n.R.dimen.design_radius_xl)
            setColor(ContextCompat.getColor(context, com.hive.i8n.R.color.colorPrimary))
            setStroke(
                context.resources.getDimensionPixelSize(R.dimen.design_border_width_normal),
                ContextCompat.getColor(context, com.hive.i8n.R.color.colorSplitLine)
            )
        }
    }

    private fun createDivider(context: Context): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.resources.getDimensionPixelSize(R.dimen.design_border_width_normal)
            ).apply {
                marginStart = context.resources.getDimensionPixelSize(R.dimen.design_spacing_4)
                marginEnd = context.resources.getDimensionPixelSize(R.dimen.design_spacing_4)
                topMargin = context.resources.getDimensionPixelSize(R.dimen.design_spacing_1)
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.design_spacing_1)
            }
            setBackgroundColor(
                ColorUtils.setAlphaComponent(
                    ContextCompat.getColor(context, com.hive.i8n.R.color.white),
                    0x14
                )
            )
        }
    }

    private fun createItemView(context: Context, action: ResourceOverflowAction, onClick: () -> Unit): View {
        return TextView(context).apply {
            text = action.title
            gravity = Gravity.CENTER_VERTICAL
            minHeight = dp(context, 44)
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.design_spacing_4),
                context.resources.getDimensionPixelSize(R.dimen.design_spacing_3),
                context.resources.getDimensionPixelSize(R.dimen.design_spacing_4),
                context.resources.getDimensionPixelSize(R.dimen.design_spacing_3)
            )
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (action.danger) android.R.color.holo_red_light else com.hive.i8n.R.color.white
                )
            )
            setOnClickListener { onClick() }
        }
    }

    private fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
