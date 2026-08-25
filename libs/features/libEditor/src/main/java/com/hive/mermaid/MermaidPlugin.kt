// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.mermaid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.style.ReplacementSpan
import android.widget.TextView
import com.hive.markdown.MarkdownMarkwon
import com.hive.markdown.MarkdownMarkwon.MaxWidth
import com.hive.net.image.ImageLoadCallBack
import com.hive.net.image.ImageLoader
import com.hive.utils.GlobalApp
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.CoreProps
import io.noties.markwon.core.SimpleBlockNodeVisitor
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Node
import java.net.URLEncoder

class MermaidPlugin(private val context: Context, val textView: TextView?) :
    AbstractMarkwonPlugin() {

    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
        builder.on(FencedCodeBlock::class.java, object : SimpleBlockNodeVisitor() {
            override fun visit(visitor: MarkwonVisitor, node: Node) {
                val codeBlock = node as FencedCodeBlock
                if (isMermaidCodeBlock(codeBlock)) {
                    visitor.blockStart(node)
                    val imageUrl =
                        "http://212.64.23.27:3000/generate?type=png&data=${
                            //pako压缩
                            URLEncoder.encode(codeBlock.literal, "utf-8")
                        }"
                    val span = GlideMermaidSpan(context, imageUrl, codeBlock.literal).apply {
                        setInvalidateCallback {
                            textView?.postDelayed({
                                textView.postInvalidate()
                            }, 100)
                        }
                    }
                    visitor.builder().append("\uFFFC").apply {
                        setSpan(span, length - 1, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    visitor.forceNewLine()
                    visitor.blockEnd(node)
                } else {
                    handleNonMermaidCodeBlock(visitor, node)
                }
            }
        })

    }

    private fun handleNonMermaidCodeBlock(visitor: MarkwonVisitor, codeBlock: FencedCodeBlock) {
        val info = codeBlock.info ?: ""
        val code = codeBlock.literal
        visitor.blockStart(codeBlock)
        val length = visitor.length()

        visitor.builder()
            .append('\u00a0').append('\n')
            .append(visitor.configuration().syntaxHighlight().highlight(info, code))

        visitor.ensureNewLine()

        visitor.builder().append('\u00a0')


        // @since 4.1.1
        CoreProps.CODE_BLOCK_INFO[visitor.renderProps()] = info

        visitor.setSpansForNodeOptional<Node>(codeBlock, length)

        visitor.blockEnd(codeBlock)
    }

    private fun isMermaidCodeBlock(codeBlock: FencedCodeBlock): Boolean {
        return codeBlock.info?.toString() == "mermaid"
    }


    class GlideMermaidSpan(
        private val context: Context,
        private val imageUrl: String,
        private val mermaid: String? = null
    ) :
        ReplacementSpan() {
        private var drawable: Drawable? = null
        private var invalidateCallback: (() -> Unit)? = null
        private var loadAttempted: Boolean = false

        fun setInvalidateCallback(callback: () -> Unit) {
            invalidateCallback = callback
        }

        override fun getSize(
            paint: Paint,
            text: CharSequence?,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?
        ): Int {
            val rect = sizeCache[imageUrl] ?: Rect(0, 0, 0, 0)
            fm?.let {
                val lineHeight = paint.fontMetricsInt.descent - paint.fontMetricsInt.ascent
                if (rect.height() > lineHeight) {
                    val offset = rect.height() - lineHeight
                    val off = ((offset / 2) / MarkdownMarkwon.TextLineMulti).toInt()
                    it.descent += off
                    it.bottom += off
                    it.ascent -= off
                    it.top -= off
                }
            }
            return rect.width()
        }

        override fun draw(
            canvas: Canvas,
            text: CharSequence?,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint
        ) {
            drawable?.let {
                val transY = (bottom - top - it.bounds.height()) / 2 + top
                canvas.save()
                canvas.translate(x, transY.toFloat())
                it.draw(canvas)
                canvas.restore()
            } ?: run {
                drawLoading(canvas, x, top, bottom, paint)
                // draw() 可能被频繁调用；避免重复触发异步加载与 invalidate 导致闪烁/抖动
                if (!loadAttempted) {
                    loadAttempted = true
                    loadImageAsync()
                }
            }
        }

        private fun drawLoading(canvas: Canvas, x: Float, top: Int, bottom: Int, paint: Paint) {
            paint.color = Color.LTGRAY
            canvas.drawRect(x, top.toFloat(), x + MaxWidth, bottom.toFloat(), paint)
        }

        private fun loadImageAsync() {
            ImageLoader.getInstance()
                .loadImageAsync(GlobalApp.getContext(), imageUrl, object : ImageLoadCallBack() {
                    override fun onImageLoadFinish(bmp: Bitmap?) {
                        val bitmap = adjustBitmap(bmp)
                        bitmap?.let {
                            drawable = BitmapDrawable(context.resources, it).apply {
                                calculateSize(this)
                            }
                            invalidateCallback?.invoke()
                        } ?: run {
                            drawable = null
                            invalidateCallback?.invoke()
                        }
                    }
                })

        }

        /**
         * 如果bmp高/宽比大于1.2，则将其缩放到正方形的高newHeight和宽newWidth为MaxWidth的bitmap中，保持不发生形变
         */
        private fun adjustBitmap(bmp: Bitmap?): Bitmap? {
            bmp?.let {
                val width = it.width
                val height = it.height
                if (height > width) {
                    val newWidth = MaxWidth
                    val newHeight = (MaxWidth * 0.8).toInt()
                    val newBitmap =
                        Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(newBitmap)
                    //自动计算bmp在newBitmap的居中位置
                    val rect = Rect(
                        (newWidth - (width * newHeight / height.toFloat()).toInt()) / 2,
                        0,
                        (newWidth + (width * newHeight / height.toFloat()).toInt()) / 2,
                        newHeight
                    )
                    canvas.drawBitmap(it, null, rect, null)
                    return newBitmap
                }
                return it
            }
            return null
        }

        private fun calculateSize(drawable: Drawable) {
            val targetWidth = MaxWidth
            val (width, height) = targetWidth to (targetWidth * drawable.intrinsicHeight / drawable.intrinsicWidth.toFloat()).toInt()
            drawable.setBounds(0, 0, width, height)
            sizeCache[imageUrl] = Rect(0, 0, width, height)
        }

        companion object {
            private val sizeCache = mutableMapOf<String, Rect>()

        }
    }
}
