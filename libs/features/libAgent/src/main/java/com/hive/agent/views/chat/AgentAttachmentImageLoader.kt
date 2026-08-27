// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.hive.net.image.ImageLoader
import com.hive.plugin.agent.model.AttachmentType
import com.hive.plugin.agent.model.ChatAttachment
import com.hive.utils.debug.DLog
import com.hive.utils.extends.gone
import com.hive.utils.extends.visible
import java.io.File

/**
 * 工具/聊天附件图片加载：兼容 http、file、content、data URL、纯 base64、blob:。
 */
object AgentAttachmentImageLoader {

    private const val TAG = "AgentAttachmentImageLoader"

    fun imageAttachments(attachments: List<ChatAttachment>): List<ChatAttachment> {
        return attachments.filter {
            it.type == AttachmentType.IMAGE && hasDisplayableSource(it)
        }
    }

    fun hasDisplayableSource(attachment: ChatAttachment): Boolean {
        return !attachment.url.isNullOrBlank() || !attachment.base64.isNullOrBlank()
    }

    fun loadInto(context: Context, imageView: ImageView, attachment: ChatAttachment) {
        val url = attachment.url?.trim().orEmpty()
        val base64 = attachment.base64?.trim().orEmpty()

        // 优先 base64 / data URL：工具截图等场景最稳，不依赖 blob 服务是否可达
        when {
            base64.isNotEmpty() || url.startsWith("data:") -> {
                loadBase64(context, imageView, if (base64.isNotEmpty()) base64 else url)
            }
            url.startsWith("http://") || url.startsWith("https://") ||
                url.startsWith("file://") || url.startsWith("content://") -> {
                imageView.visible()
                ImageLoader.getInstance().loadImage(context, imageView, url)
            }
            url.startsWith("/") && File(url).exists() -> {
                imageView.visible()
                ImageLoader.getInstance().loadImage(context, imageView, url)
            }
            url.startsWith("blob:") -> {
                val httpUrl = url.removePrefix("blob:")
                if (httpUrl.startsWith("http")) {
                    imageView.visible()
                    ImageLoader.getInstance().loadImage(context, imageView, httpUrl)
                } else {
                    imageView.gone()
                }
            }
            url.isNotEmpty() -> {
                imageView.visible()
                ImageLoader.getInstance().loadImage(context, imageView, url)
            }
            else -> imageView.gone()
        }
    }

    private fun loadBase64(context: Context, imageView: ImageView, raw: String) {
        try {
            val payload = if (raw.startsWith("data:")) {
                raw.substringAfter("base64,", missingDelimiterValue = "").trim()
            } else {
                raw.trim()
            }
            if (payload.isEmpty()) {
                imageView.gone()
                return
            }
            val bytes = Base64.decode(payload, Base64.DEFAULT)
            if (bytes.isEmpty()) {
                imageView.gone()
                return
            }
            // 先校验可解码，再交给 Glide
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                DLog.w(TAG, "base64 is not a decodable image")
                imageView.gone()
                return
            }
            imageView.visible()
            Glide.with(context).load(bytes).fitCenter().into(imageView)
        } catch (e: Exception) {
            DLog.e(TAG, "loadBase64 failed: ${e.message}", e)
            imageView.gone()
        }
    }
}
