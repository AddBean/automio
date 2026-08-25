// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import com.hive.views.R

/**
 * 协议勾选控件
 * 使用 SpannableString 实现可点击的用户协议和隐私政策链接
 *
 * 示例用法：
 * ```kotlin
 * agreementView.apply {
 *     onAgreementClick = { // 用户协议点击 }
 *     onPrivacyClick = { // 隐私政策点击 }
 *     if (isChecked) { ... }
 * }
 * ```
 */
class AgreementCheckView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val checkBox: CheckBox
    private val tvContent: TextView

    /** 是否已勾选同意 */
    val isChecked: Boolean
        get() = checkBox.isSelected

    /** 用户协议点击回调 */
    var onAgreementClick: (() -> Unit)? = null

    /** 隐私政策点击回调 */
    var onPrivacyClick: (() -> Unit)? = null

    /** 勾选状态变化回调 */
    var onCheckedChange: ((Boolean) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        LayoutInflater.from(context).inflate(R.layout.view_agreement_check, this, true)
        checkBox = findViewById(R.id.checkbox)
        tvContent = findViewById(R.id.tv_content)

        // CheckBox 点击事件
        checkBox.setOnClickListener {
            checkBox.isSelected = !checkBox.isSelected
            onCheckedChange?.invoke(checkBox.isSelected)
        }

        // 设置默认文本（使用国际化字符串）
        setupDefaultText()
    }

    private fun setupDefaultText() {
        val prefix = getString(com.hive.i8n.R.string.agreement_prefix)
        val agreementText = getString(com.hive.i8n.R.string.agreement_text)
        val middleText = getString(com.hive.i8n.R.string.agreement_middle)
        val privacyText = getString(com.hive.i8n.R.string.agreement_privacy)
        setAgreementText(prefix, agreementText, middleText, privacyText)
    }

    private fun getString(resId: Int): String {
        return try {
            context.getString(resId)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 设置协议文本
     * @param prefix 前缀文本，如 "我已阅读并同意"
     * @param agreementText 用户协议文本
     * @param middleText 中间文本，如 "和"
     * @param privacyText 隐私政策文本
     */
    fun setAgreementText(
        prefix: String,
        agreementText: String,
        middleText: String,
        privacyText: String
    ) {
        val fullText = "$prefix$agreementText$middleText$privacyText"
        val spannable = SpannableString(fullText)

        var startIndex = 0

        // 前缀文本（灰色）
        val prefixEnd = prefix.length
        spannable.setSpan(
            ForegroundColorSpan(getColor(com.hive.i8n.R.color.design_text_tertiary)),
            0,
            prefixEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        startIndex = prefixEnd

        // 用户协议（可点击，强调色）
        val agreementEnd = startIndex + agreementText.length
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                onAgreementClick?.invoke()
            }
        }, startIndex, agreementEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(
            ForegroundColorSpan(getColor(com.hive.i8n.R.color.design_accent_indigo)),
            startIndex,
            agreementEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        startIndex = agreementEnd

        // 中间文本（灰色）
        val middleEnd = startIndex + middleText.length
        spannable.setSpan(
            ForegroundColorSpan(getColor(com.hive.i8n.R.color.design_text_tertiary)),
            startIndex,
            middleEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        startIndex = middleEnd

        // 隐私政策（可点击，强调色）
        val privacyEnd = startIndex + privacyText.length
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                onPrivacyClick?.invoke()
            }
        }, startIndex, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(
            ForegroundColorSpan(getColor(com.hive.i8n.R.color.design_accent_indigo)),
            startIndex,
            privacyEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvContent.text = spannable
        tvContent.movementMethod = LinkMovementMethod.getInstance()
        tvContent.highlightColor = android.graphics.Color.TRANSPARENT
    }

    /**
     * 设置勾选状态
     */
    fun setChecked(checked: Boolean) {
        checkBox.isSelected = checked
    }

    /**
     * 切换勾选状态
     */
    fun toggle() {
        checkBox.isSelected = !checkBox.isSelected
        onCheckedChange?.invoke(checkBox.isSelected)
    }

    private fun getColor(colorResId: Int): Int {
        return context.getColor(colorResId)
    }
}