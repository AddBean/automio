// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.mermaid

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.webkit.WebView

class MermaidRenderer : WebView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        privateBrowsing: Boolean
    ) : super(context, attrs, defStyleAttr, privateBrowsing)


    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
    }

    fun renderMermaid(mermaid: String, onRenderer: (bmp: Bitmap) -> Unit) {
        if (parent != null) {
            detachFromWindow()
        }
        attachToWindow()
        val html = """
          <!doctype html>
<html lang="en">
  <body>
    <div id="graphDiv"></div>
    <script type="module">
      import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';
       mermaid.initialize({ startOnLoad: false });

  // Example of using the render function
  const drawDiagram = async function () {
    element = document.querySelector('#graphDiv');
    const graphDefinition = '$mermaid';
    const { svg } = await mermaid.render('graphDiv', graphDefinition);
    element.innerHTML = svg;
    
  };
    </script>
  </body>
</html>
        """.trimIndent()
        loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        this.postDelayed({
            val bmpWidth = if (this.width == 0) 1000 else this.width
            val bmpHeight = if (this.height == 0) 1000 else this.height
            val bmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bmp))
            onRenderer(bmp)
            //                    detachFromWindow()
        }, 5000)
    }

    private fun attachToWindow() {
        if (context is Activity) {
            (context as Activity).addContentView(
                this,
                LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun detachFromWindow() {
        (parent as ViewGroup).removeView(this)
    }

    companion object {

        private val mermaidCache = mutableMapOf<String, Bitmap>()

        fun renderMermaidInBackend(
            activity: Activity,
            mermaid: String,
            onRenderer: (bmp: Bitmap) -> Unit
        ) {
            if (mermaidCache.containsKey(mermaid)) {
                onRenderer(mermaidCache[mermaid]!!)
                return
            }
            val webView = MermaidRenderer(activity)
            //开始渲染
            webView.renderMermaid(mermaid) {
                mermaidCache[mermaid] = it
                onRenderer(it)
            }
        }
    }
}