// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.utils.system.CommonUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUploadCompressor {

    enum class Preset {
        AVATAR,
        PUBLISH_COVER,
        PUBLISH_DETAIL,
        LEGACY_DEFAULT
    }

    data class Spec(
        val maxLongEdge: Int,
        val targetSizeKb: Int,
        val initialQuality: Int = 82,
        val minQuality: Int = 60,
        val preferWebp: Boolean = true
    )

    data class Result(
        val file: File,
        val mimeType: String
    )

    @JvmStatic
    fun specFor(preset: Preset): Spec {
        return when (preset) {
            Preset.AVATAR -> Spec(512, 120, 80, 56, true)
            Preset.PUBLISH_COVER -> Spec(1440, 320, 82, 60, true)
            Preset.PUBLISH_DETAIL -> Spec(1280, 220, 80, 58, true)
            Preset.LEGACY_DEFAULT -> Spec(256, 80, 78, 52, true)
        }
    }

    @JvmStatic
    fun specFromLegacyParams(newWidth: Double, sizeKb: Int): Spec {
        val longEdge = if (newWidth > 0) newWidth.toInt().coerceAtLeast(1) else specFor(Preset.LEGACY_DEFAULT).maxLongEdge
        return Spec(
            maxLongEdge = longEdge,
            targetSizeKb = sizeKb,
            initialQuality = 78,
            minQuality = 52,
            preferWebp = true
        )
    }

    @JvmStatic
    @Throws(IllegalStateException::class)
    fun compress(sourceFile: File, spec: Spec): Result {
        require(sourceFile.exists() && sourceFile.isFile) { "Source file not found" }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("Decode image bounds failed")
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, spec.maxLongEdge)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions)
            ?: throw IllegalStateException("Decode image failed")

        val rotated = applyExifRotation(sourceFile.absolutePath, decoded)
        val scaled = resizeIfNeeded(rotated, spec.maxLongEdge)
        if (scaled !== rotated && rotated !== decoded && !rotated.isRecycled) {
            rotated.recycle()
        }
        if (rotated !== decoded && decoded !== scaled && !decoded.isRecycled) {
            decoded.recycle()
        }

        val hasAlpha = scaled.hasAlpha()
        val primaryFormat = selectPrimaryFormat(spec.preferWebp, hasAlpha)
        val fallbackFormat = selectFallbackFormat(hasAlpha)

        return try {
            val primaryBytes = encodeToTargetSize(scaled, primaryFormat, spec)
            Result(writeCompressedFile(primaryBytes, primaryFormat.extension), primaryFormat.mimeType)
        } catch (primaryError: Throwable) {
            DLog.e("ImageUploadCompressor primary format failed: ${primaryError.message}")
            val fallbackBytes = encodeToTargetSize(scaled, fallbackFormat, spec)
            Result(writeCompressedFile(fallbackBytes, fallbackFormat.extension), fallbackFormat.mimeType)
        } finally {
            if (!scaled.isRecycled) {
                scaled.recycle()
            }
        }
    }

    private fun computeInSampleSize(width: Int, height: Int, maxLongEdge: Int): Int {
        val longEdge = maxOf(width, height)
        if (longEdge <= maxLongEdge) return 1

        var sampleSize = 1
        var current = longEdge
        while (current / 2 >= maxLongEdge) {
            current /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun applyExifRotation(path: String, bitmap: Bitmap): Bitmap {
        val degree = try {
            val exif = android.media.ExifInterface(path)
            when (exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (_: Throwable) {
            0f
        }
        if (degree == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degree) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    private fun resizeIfNeeded(bitmap: Bitmap, maxLongEdge: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longEdge = maxOf(width, height)
        if (longEdge <= maxLongEdge) return bitmap

        val scale = maxLongEdge / longEdge.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also {
            if (it !== bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    private fun encodeToTargetSize(bitmap: Bitmap, format: OutputFormat, spec: Spec): ByteArray {
        val source = if (format.requiresOpaqueBackground && bitmap.hasAlpha()) {
            flattenAlpha(bitmap)
        } else {
            bitmap
        }
        try {
            var quality = spec.initialQuality.coerceIn(spec.minQuality, 100)
            var bestBytes = compressBitmap(source, format, quality)
            while (bestBytes.size / 1024 > spec.targetSizeKb && quality > spec.minQuality) {
                quality = (quality - 6).coerceAtLeast(spec.minQuality)
                bestBytes = compressBitmap(source, format, quality)
                if (quality == spec.minQuality) {
                    break
                }
            }
            DLog.e(
                "ImageUploadCompressor format=${format.extension} width=${source.width} height=${source.height} " +
                    "sizeKb=${bestBytes.size / 1024} targetKb=${spec.targetSizeKb}"
            )
            return bestBytes
        } finally {
            if (source !== bitmap && !source.isRecycled) {
                source.recycle()
            }
        }
    }

    private fun compressBitmap(bitmap: Bitmap, format: OutputFormat, quality: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val success = bitmap.compress(format.compressFormat, quality, output)
        if (!success) {
            throw IllegalStateException("Bitmap compress failed: ${format.extension}")
        }
        return output.toByteArray()
    }

    private fun flattenAlpha(bitmap: Bitmap): Bitmap {
        val flattened = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(flattened)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return flattened
    }

    private fun writeCompressedFile(bytes: ByteArray, extension: String): File {
        val file = File(GlobalApp.getApp().cacheDir, "${CommonUtils.getRandomName()}.$extension")
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    private fun selectPrimaryFormat(preferWebp: Boolean, hasAlpha: Boolean): OutputFormat {
        if (!preferWebp) return OutputFormat.JPEG
        return when {
            hasAlpha && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> OutputFormat.WEBP_LOSSLESS
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> OutputFormat.WEBP_LOSSY
            else -> OutputFormat.WEBP_LEGACY
        }
    }

    private fun selectFallbackFormat(hasAlpha: Boolean): OutputFormat {
        return if (hasAlpha) OutputFormat.PNG else OutputFormat.JPEG
    }

    private enum class OutputFormat(
        val compressFormat: Bitmap.CompressFormat,
        val extension: String,
        val mimeType: String,
        val requiresOpaqueBackground: Boolean
    ) {
        JPEG(Bitmap.CompressFormat.JPEG, "jpg", "image/jpeg", true),
        PNG(Bitmap.CompressFormat.PNG, "png", "image/png", false),
        WEBP_LEGACY(Bitmap.CompressFormat.WEBP, "webp", "image/webp", false),
        WEBP_LOSSY(Bitmap.CompressFormat.WEBP_LOSSY, "webp", "image/webp", false),
        WEBP_LOSSLESS(Bitmap.CompressFormat.WEBP_LOSSLESS, "webp", "image/webp", false)
    }
}
