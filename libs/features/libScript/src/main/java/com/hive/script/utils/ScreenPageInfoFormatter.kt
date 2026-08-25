// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import com.hive.plugin.ocr.OcrResult
import com.hive.script.driver.ScriptEventHelper
import com.hive.utils.extends.toDigits
import com.hive.utils.extends.toJson

object ScreenPageInfoFormatter {

    fun buildPayload(
        layoutResult: ScriptLayoutReader.LayoutResult? = null,
        ocrResult: OcrResult? = null,
    ): Map<String, Any> {
        val layoutTree = buildLayoutTree(layoutResult)
        val ocrItems = buildOcrItems(ocrResult)
        val payload = linkedMapOf<String, Any>(
            "src" to resolveSource(layoutTree != null, ocrItems.isNotEmpty()),
            "meta" to buildMeta(layoutResult),
            "layout" to (layoutTree ?: emptyMap<String, Any>()),
            "ocr" to ocrItems
        )
        if (layoutTree == null && ocrItems.isEmpty()) {
            payload["err"] = "no_screen_data"
        }
        return payload
    }

    fun buildJson(
        layoutResult: ScriptLayoutReader.LayoutResult? = null,
        ocrResult: OcrResult? = null,
    ): String = buildPayload(layoutResult, ocrResult).toJson()

    private fun resolveSource(hasLayout: Boolean, hasOcr: Boolean): String {
        return when {
            hasLayout && hasOcr -> "layout+ocr"
            hasOcr -> "ocr_only"
            hasLayout -> "layout_only"
            else -> "none"
        }
    }

    private fun buildMeta(layoutResult: ScriptLayoutReader.LayoutResult?): Map<String, Any> {
        val event = ScriptEventHelper.get().accessibilityViewEvent
        val meta = linkedMapOf<String, Any>()
        val pkg = layoutResult?.pkgName ?: event?.packageName?.toString()
        val act = event?.className?.toString()?.substringAfterLast('.')
        val width = layoutResult?.width?.takeIf { it > 0 }
            ?: ScriptCoordinateAdapter.getScreenWidthByOrientation()
        val height = layoutResult?.height?.takeIf { it > 0 }
            ?: ScriptCoordinateAdapter.getScreenHeightByOrientation()
        if (!pkg.isNullOrBlank()) meta["pkg"] = pkg
        if (!act.isNullOrBlank()) meta["act"] = act
        meta["w"] = width
        meta["h"] = height
        return meta
    }

    private fun buildLayoutTree(layoutResult: ScriptLayoutReader.LayoutResult?): Map<String, Any>? {
        val rootNode = layoutResult?.rootNode ?: return null

        fun buildEntry(node: ScriptLayoutReader.LayoutNodeInfo, forceKeep: Boolean = false): Map<String, Any>? {
            val rect = node.bounds?.let {
                listOf(it.l.toDigits(3), it.t.toDigits(3), it.r.toDigits(3), it.b.toDigits(3))
            } ?: return null
            if (!forceKeep && !node.visible) return null

            val item = linkedMapOf<String, Any>("rect" to rect)
            if (!node.clazz.isNullOrBlank()) item["cls"] = node.clazz
            if (!node.text.isNullOrBlank()) item["text"] = node.text.trim().take(100)
            if (!node.desc.isNullOrBlank()) item["desc"] = node.desc.trim().take(100)
            if (!node.id.isNullOrBlank()) item["rid"] = node.id
            if (node.clickable) item["click"] = 1
            if (node.checked) item["checked"] = 1

            val childItems = node.nodes.mapNotNull { buildEntry(it) }
            if (childItems.isNotEmpty()) item["children"] = childItems
            return item
        }

        return buildEntry(rootNode, forceKeep = true)
    }

    private fun buildOcrItems(ocrResult: OcrResult?): List<Map<String, Any>> {
        if (ocrResult == null) return emptyList()
        val items = mutableListOf<Map<String, Any>>()
        ocrResult.blocks.forEach { block ->
            block.lines.forEach { line ->
                val text = line.text?.trim().orEmpty()
                if (text.isBlank()) return@forEach
                val xs = line.points.map { ScriptCoordinateAdapter.toNormalizedX(it.x).toDigits(3) }
                val ys = line.points.map { ScriptCoordinateAdapter.toNormalizedY(it.y).toDigits(3) }
                if (xs.isEmpty() || ys.isEmpty()) return@forEach
                items.add(
                    linkedMapOf(
                        "text" to text.take(100),
                        "rect" to listOf(
                            xs.minOrNull() ?: 0f,
                            ys.minOrNull() ?: 0f,
                            xs.maxOrNull() ?: 0f,
                            ys.maxOrNull() ?: 0f
                        )
                    )
                )
            }
        }
        return items
    }
}
