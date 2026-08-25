// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.markdown

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IEditorProvider

class MarkdownTextView : androidx.appcompat.widget.AppCompatTextView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        setTextColor(Color.WHITE)
        setLineSpacing(0f, MarkdownMarkwon.TextLineMulti)
        setPadding(MarkdownMarkwon.TextPadding, 0, MarkdownMarkwon.TextPadding, 0)
    }

    fun loadMarkdown(markdown: String) {
        val provider2 = ComponentManager.getInstance()
            .getProvider(IEditorProvider::class.java) as IEditorProvider?
        provider2?.renderMarkdown(this, markdown)
    }
}