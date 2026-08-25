// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.hive.utils.GlobalApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object ScriptBitmapHelper {

    @OptIn(DelicateCoroutinesApi::class)
    fun createBitmapByFilesAsync(paths: List<File>?, callback: (Bitmap?) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            callback(withContext(Dispatchers.IO) {
                createBitmapByFiles(paths)
            })
        }
    }

    fun createBitmapByPathsAsync(paths: List<String>, callback: (Bitmap?) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            callback(withContext(Dispatchers.IO) {
                createBitmapByPaths(paths)
            })
        }
    }

    fun createBitmapByFiles(paths: List<File>?): Bitmap? {
        if (paths == null) return null
        val bitmapList = mutableListOf<Bitmap>()
        paths.forEach {
            try {
                val bmp = BitmapFactory.decodeFile(it.absolutePath)
                if (bmp != null)
                    bitmapList.add(bmp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (bitmapList.isEmpty()) return null
        return createIconBitmap(bitmapList)
    }

    private fun createBitmapByPaths(paths: List<String>): Bitmap? {
        val bitmapList = mutableListOf<Bitmap>()
        paths.forEach {
            try {
                val bmp = BitmapFactory.decodeFile(it)
                if (bmp != null)
                    bitmapList.add(bmp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return createIconBitmap(bitmapList)
    }

    /**
     * create icon fot script short cut
     */
    fun createIconBitmap(bitmapList: List<Bitmap>): Bitmap? {
        val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val pad = 3 * GlobalApp.DP
        when (bitmapList.size) {
            1 -> ScriptIconMaker.drawBitmapToGridView(canvas, bitmapList, pad)
            //2x1
            2 -> ScriptIconMaker.drawBitmapToGridView(canvas, bitmapList, pad)
            //1x3
            3 -> ScriptIconMaker.drawBitmapToGridView(canvas, bitmapList, pad)
            //2x2
            4 -> ScriptIconMaker.drawBitmapToGridView(canvas, bitmapList, pad)
            //2x3
            5, 6 -> ScriptIconMaker.drawBitmapToGridView(canvas, bitmapList, pad)
            //3x3
            else -> ScriptIconMaker.drawBitmapToGridView(canvas, bitmapList, pad)
        }
        return bmp
    }
}