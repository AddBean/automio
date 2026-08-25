// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.carlos.ui.loading

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import android.view.View

class LoadingImageView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    AppCompatImageView(
        context!!, attrs
    ) {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val drawable = drawable
        if (drawable != null && drawable is LoadingDrawable) {
            if(drawable.isStarted.not() && VISIBLE == visibility){
                drawable.start()
            }
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        val drawable = drawable
        if (drawable != null && drawable is LoadingDrawable) {
            if(visibility == View.VISIBLE){
                if(drawable.isStarted.not()){
                    drawable.start()
                }
            }else {
                drawable.stop()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        val drawable = drawable
        if (drawable != null && drawable is LoadingDrawable) {
            drawable.stop()
        }
    }

    init {
        setImageDrawable(
            LoadingDrawable(
                MaterialLoadingRenderer.Builder(getContext())
                    .setDuration(1130)
                    .build()
            ).apply {
                start()
            }
        )
    }
}