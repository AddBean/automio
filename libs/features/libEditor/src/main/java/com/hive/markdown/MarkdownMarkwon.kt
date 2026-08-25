// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.markdown

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.Target
import com.hive.mermaid.MermaidPlugin
import com.hive.utils.GlobalApp
import com.hive.utils.extends.dp
import com.hive.utils.system.UIUtils
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.glide.GlideImagesPlugin
import java.util.WeakHashMap

object MarkdownMarkwon {


    val TextPadding = 12.dp

    val TextLineMulti =1.5f

    val MaxWidth = (UIUtils.getScreenWidth(GlobalApp.getContext()) - 2 * TextPadding).toInt()

    private val markwonByTextView: MutableMap<TextView, Markwon> = WeakHashMap()

    fun create(context: Context, textView: TextView?): Markwon {
        if (textView != null) {
            markwonByTextView[textView]?.let { return it }
            val built = build(context, textView)
            markwonByTextView[textView] = built
            return built
        }
        return build(context, null)
    }

    private fun build(context: Context, textView: TextView?): Markwon = Markwon.builder(context)
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                configureThemeDefault(builder)
            }
        })
        .usePlugin(GlideImagesPlugin.create(object : GlideImagesPlugin.GlideStore {
            @SuppressLint("CheckResult")
            override fun load(drawable: AsyncDrawable): RequestBuilder<Drawable> {
                val drawable = Glide.with(context).load(drawable.destination);
                //设置大小为屏幕宽度
                drawable.override(MaxWidth, Target.SIZE_ORIGINAL)
                return drawable
            }

            override fun cancel(target: Target<*>) {
                Glide.with(context).clear(target);
            }

        }))
        .usePlugin(HtmlPlugin.create())
        .usePlugin(MermaidPlugin(context, textView))
//        .usePlugin(PlantUmlPlugin(context))
        .usePlugin(TablePlugin.create(context))
        .build()

    private fun configureThemeDefault(builder: MarkwonTheme.Builder) {
        //适合手机风格的字体
        // 全局文本样式
        builder                           // 基础字号
//            .blockQuoteColor(Color.parseColor("#666666"))        // 引用条颜色
//            .linkColor(Color.parseColor("#007AFF"))         // 链接颜色

            // 标题样式
            .headingTextSizeMultipliers(
                floatArrayOf(
                    1.5f,  // h1
                    1.4f,  // h2
                    1.3f,  // h3
                    1.2f,  // h4
                    1.1f,  // h5
                    1.0f   // h6
                )
            )

//            .headingTypeface(Typeface.DEFAULT_BOLD) // 标题字体
            //设置标题颜色
            // 代码块样式，绿色
//            .codeTextColor(GlobalApp.getColor(com.hive.i8n.R.color.colorGreen))          // 代码颜色
//            .codeBackgroundColor(Color.parseColor("#F5F5F5"))    // 代码背景色
//            .codeBlockBackgroundColor(Color.parseColor("#F8F8F8")) // 代码块背景
//            .codeBlockMargin(12.dp)                              // 代码块外边距
            // 列表样式
//            .listItemColor(Color.parseColor("#666666"))       // 列表项颜色

            // 块引用样式
//            .blockQuoteColor(Color.parseColor("#4CAF50"))     // 引用条颜色

//            .linkColor(Color.parseColor("#007AFF"))         // 链接颜色
            .isLinkUnderlined(false)
    }

    fun renderToBitmapSync(markdown: String): Bitmap? {
        val markwon = create(GlobalApp.getContext(), null)
        val node = markwon.parse(markdown)
        val spanned = markwon.render(node)
        Thread.sleep(10000) // 等待图片加载完成
        val textView = TextView(GlobalApp.getContext()).apply {
            text = spanned
            measure(
                View.MeasureSpec.makeMeasureSpec(
                    MaxWidth,
                    View.MeasureSpec.EXACTLY
                ),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            layout(0, 0, measuredWidth, measuredHeight)
        }

        val bmp = Bitmap.createBitmap(
            textView.measuredWidth,
            textView.measuredHeight,
            Bitmap.Config.ARGB_8888
        ).apply {
            Canvas(this).apply {
                drawColor(Color.WHITE)
                textView.draw(this)
            }
        }

        return bmp;
    }


}