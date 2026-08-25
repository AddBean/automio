// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.hive.annotation.NotProguard
import com.hive.ocr.google.GoogleOcrProvider
import com.hive.plugin.ocr.IOcrResultListener
import com.hive.plugin.provider.IOcrProvider

@NotProguard
class OcrProvider : IOcrProvider {

    private val ocrProvider = GoogleOcrProvider()

    override fun init(context: Context?) {

    }

    override fun findText(
        bmp: Bitmap,
        type: Int,
        text: MutableList<String>,
        regions: MutableList<Rect>?,
        listener: IOcrResultListener?
    ) {
        ocrProvider.findText(bmp, type, text, regions) {
            listener?.onResult(it)
        }
    }

    override fun readText(
        bmp: Bitmap?,
        regions: MutableList<Rect>?,
        regionListener: IOcrResultListener?,
        finalListener: IOcrResultListener?
    ) {
        ocrProvider.readText(bmp, regions, { rect, ocrResult ->
            regionListener?.onResult(ocrResult)
        }, {
            finalListener?.onResult(it)
        })
    }
}