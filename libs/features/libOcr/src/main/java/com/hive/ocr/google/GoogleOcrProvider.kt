// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ocr.google

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.text.TextUtils
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
//import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
//import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hive.plugin.ocr.OcrResult
import com.hive.utils.GlobalApp
import java.util.Locale

//https://developers.google.cn/ml-kit/vision/text-recognition/v2/android?hl=zh-cn
class GoogleOcrProvider {

    /**
     * The map of text recognizer by locale language
     */
    private val recognizerMap: MutableMap<String, TextRecognizer> = mutableMapOf()

    /**
     * Get the default local text recognizer by locale Language
     */
    private fun getDefaultRecognizer(): TextRecognizer? {
        val lang = getLanguage(GlobalApp.getContext())
        if (recognizerMap[lang] != null) {
            return recognizerMap[lang]
        }
        when {
            lang.contains("zh") -> {
                recognizerMap[lang] =
                    TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            }

//            "ja" -> {
//                recognizerMap[lang] =
//                    TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
//            }
//
//            "ko" -> {
//                recognizerMap[lang] =
//                    TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
//            }

            else -> {
                recognizerMap[lang] =
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            }
        }
        return recognizerMap[lang]
    }

    /**
     * Get the language from the context
     */
    private fun getLanguage(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("LanguagePrefs", Context.MODE_PRIVATE)
        val savedLanguage =
            sharedPreferences.getString("language_key", Locale.getDefault().language)
        return savedLanguage ?: Locale.getDefault().language
    }

    /**
     * Find the text in the bitmap in the region
     * @param bmp the bitmap
     * @param type 0 contains, 1 equals
     * @param texts the list of text
     * @param regions the array of Rect
     * @param onResult the callback of the result
     */
    fun findText(
        bmp: Bitmap,
        type: Int,
        texts: List<String>,
        regions: List<Rect>?,
        onResult: (OcrResult?) -> Unit
    ) {
        var regions: List<Rect>? = regions
        if (regions.isNullOrEmpty()) {
            regions = mutableListOf(Rect(0, 0, bmp.width, bmp.height))
        }
        val recognizer = getDefaultRecognizer()
        if (recognizer == null) {
            onResult(null)
            return
        }

        val clips = regions.map {
            Bitmap.createBitmap(bmp, it.left, it.top, it.width(), it.height())
        }

        /**
         * Filter the result in the regions and texts
         */
        fun filterResultInRegionsAndTexts(result: OcrResult) {
            //先过滤掉block.lines中不包含texts、且没有在regions中的line
            result.blocks?.forEach { block ->
                block.lines = block.lines?.filter { line ->
                    texts.any { text ->
                        line.text?.contains(text) == true
                    } && regions?.any { region ->
                        line.rect?.intersect(region) == true
                    } == true
                }?.toMutableList()
            }

            //清除掉空line的block
            result.blocks = result.blocks?.filter { block ->
                block.lines?.isNotEmpty() == true
            }?.toMutableList()
        }

        /**
         * Find the target text in the line and calculate the rect
         */
        fun findTargetTextRect(result: OcrResult, type: Int) {
            result.blocks?.firstOrNull()?.lines?.forEach { line ->
                line.findResult = mutableMapOf()
                texts.forEach { targetText ->
                    val isFind = when (type) {
                        0 -> line.text.contains(targetText)
                        1 -> TextUtils.equals(line.text, targetText)
                        else -> false
                    }
                    if (isFind) {
                        //根据targetText在line.text中的位置，算出在line.rect中的位置

                        val index = line.text.indexOf(targetText)
                        val targetRect = Rect()
                        targetRect.left =
                            line.rect.left + line.rect.width() * index / line.text.length
                        targetRect.right =
                            targetRect.left + line.rect.width() * targetText.length / line.text.length
                        targetRect.top = line.rect.top
                        targetRect.bottom = line.rect.bottom
                        line.findResult[targetText] = targetRect
                    }
                }
            }
        }
        clips.forEach {
            readText(it) {
                it?.let {
                    filterResultInRegionsAndTexts(it)
                    findTargetTextRect(it, type)
                    onResult(it)
                } ?: onResult(null)
            }
        }

    }


    /**
     * Find the text in the bitmap in the region
     * @param bmp the bitmap
     * @param regions the array of Rect
     * @param onRectResult the callback of the result of the region
     * @param onFinalResult the callback of the final result
     */
    fun readText(
        bmp: Bitmap?,
        regions: List<Rect>?,
        onRectResult: (Rect, OcrResult?) -> Unit,
        onFinalResult: (OcrResult?) -> Unit
    ) {
        if (bmp == null) {
            onFinalResult(null)
            return
        }
        var regions: List<Rect>? = regions
        if (regions.isNullOrEmpty()) {
            regions = mutableListOf(Rect(0, 0, bmp.width, bmp.height))
        }
        //clip the region in the bitmap
        val clips = regions.map {
            Bitmap.createBitmap(bmp, it.left, it.top, it.width(), it.height())
        }

        //convert the result's rect in regions to the bmp's rect
        fun covertOcrResult(region: Rect, result: OcrResult?): OcrResult? {
            result?.blocks?.forEach { it ->
                it.rect?.offset(region.left, region.top)
                it.lines?.forEach {
                    it.rect?.offset(region.left, region.top)
                }
            }
            return result
        }

        val finalOcrResult = OcrResult().apply {
            blocks = mutableListOf()
        }
        clips.forEachIndexed { index, bitmap ->
            readText(bitmap) { it ->
                val srcRect = regions[index]
                val results = covertOcrResult(srcRect, it)
                onRectResult(srcRect, results)
                if (index == clips.size - 1) {
                    it?.blocks?.let {
                        finalOcrResult.blocks?.addAll(it)
                    }
                    onFinalResult(finalOcrResult)
                }
            }
        }
    }

    /**
     * Find the text in the bitmap and return the array of Rect
     *
     * public class OcrResult {
     *     public List<Block> blocks;
     *
     *     public static class Block {
     *         public String text;
     *         public Rect rect;
     *         public String language;
     *         public Point[] points;
     *         public List<Line> lines;
     *     }
     *
     *
     *     public static class Line {
     *         public String text;
     *         public Rect rect;
     *         public String language;
     *         public Point[] points;
     *
     *
     *         public Line(String text, Rect rect, String language, Point[] points) {
     *             this.text = text;
     *             this.rect = rect;
     *             this.language = language;
     *             this.points = points;
     *         }
     *     }
     * }
     */
    private fun readText(
        bmp: Bitmap?,
        onResult: (OcrResult?) -> Unit
    ) {
        val recognizer = getDefaultRecognizer()
        if (recognizer == null) {
            onResult(null)
        }
        val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bmp!!, 0)
        val result = recognizer?.process(image)
        result?.addOnSuccessListener {
            val ggText = it
            onResult(coverTextToOcrResult(ggText))
        }
        result?.addOnFailureListener {
            onResult(null)
        }
        result?.addOnCompleteListener {
//            recognizer.close()
        }
        result?.addOnCanceledListener {
//            recognizer.close()
            onResult(null)
        }
    }


    private fun coverTextToOcrResult(ggText: Text): OcrResult {
        val ocrResult = OcrResult().apply {
            blocks = mutableListOf()
        }
        for (block in ggText.textBlocks) {
            val blockResult = OcrResult.Block().apply {
                text = block.text
                rect = block.boundingBox
                language = block.recognizedLanguage
                points = block.cornerPoints
                lines = mutableListOf()
            }
            for (line in block.lines) {
                val lineResult = OcrResult.Line().apply {
                    text = line.text
                    rect = line.boundingBox
                    language = line.recognizedLanguage
                    points = line.cornerPoints
                }
                blockResult.lines?.add(lineResult)
            }
            ocrResult.blocks?.add(blockResult)
        }
        return ocrResult
    }

    companion object {
        val instance: GoogleOcrProvider by lazy { GoogleOcrProvider() }

        fun get() = instance
    }

}