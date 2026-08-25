// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plantuml

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
import com.hive.markdown.MarkdownEncoder
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

class PlantUmlPlugin(private val context: Context) : AbstractMarkwonPlugin() {

    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
        builder.on(FencedCodeBlock::class.java, object : SimpleBlockNodeVisitor() {
            override fun visit(visitor: MarkwonVisitor, node: Node) {
                val codeBlock = node as FencedCodeBlock
                if (isPlantUmlCodeBlock(codeBlock)) {
                    visitor.blockStart(node)
                    val imageUrl = "https://www.plantuml.com/plantuml/png/~1${
                        MarkdownEncoder.encodePlantUml(codeBlock.literal)
                    }"
                    val span = GlidePlantUmlSpan(context, imageUrl, codeBlock.literal).apply {
                        setInvalidateCallback {
                            visitor.builder().setSpan(
                                this,
                                visitor.length(),
                                visitor.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            visitor.builder().append("\n")
                        }
                    }
                    visitor.builder().append("\uFFFC").apply {
                        setSpan(span, length - 1, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    visitor.forceNewLine()
                    visitor.blockEnd(node)
                } else {
                    handleNonPlantUmlCodeBlock(visitor, node)
                }
            }
        })

    }

    private fun handleNonPlantUmlCodeBlock(visitor: MarkwonVisitor, codeBlock: FencedCodeBlock) {
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

    private fun isPlantUmlCodeBlock(codeBlock: FencedCodeBlock): Boolean {
        return codeBlock.info?.toString() == "plantuml"
    }

    class GlidePlantUmlSpan(
        private val context: Context,
        private val imageUrl: String,
        private val plantUml: String? = null
    ) :
        ReplacementSpan() {
        private var drawable: Drawable? = null
        private var invalidateCallback: (() -> Unit)? = null

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
            val rect = sizeCache[imageUrl] ?: Rect(0, 0, MaxWidth, MaxWidth / 2)
            fm?.let {
                val lineHeight = paint.fontMetricsInt.descent - paint.fontMetricsInt.ascent
                if (rect.height() > lineHeight) {
                    val offset = rect.height() - lineHeight
                    it.descent += offset / 2
                    it.bottom += offset / 2
                    it.ascent -= offset / 2
                    it.top -= offset / 2
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
                loadImageAsync()
            }
        }

        private fun drawLoading(canvas: Canvas, x: Float, top: Int, bottom: Int, paint: Paint) {
            paint.color = Color.LTGRAY
            canvas.drawRect(x, top.toFloat(), x + MaxWidth, bottom.toFloat(), paint)
        }

        private fun loadImageAsync() {
            ImageLoader.getInstance()
                .loadImageAsync(GlobalApp.getContext(), imageUrl, object : ImageLoadCallBack() {
                    override fun onImageLoadFinish(bitmap: Bitmap?) {
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
